# 프로젝트: AI 촬영 가이드 카메라 (Android)

## 목표
인물/음식 사진을 찍을 때 실시간으로 "한 번에 하나씩" 촬영 조언을 해주고,
모든 조건이 완벽할 때만 자동으로 촬영하며, 감성 필터를 실시간으로 적용하는 카메라 앱.
SODA 같은 감성 필터 카메라에 AI 촬영 가이드를 접목한 컨셉이다.

## 작업 방식 (반드시 지킬 것)
- 아래 Phase 순서대로 진행한다. 각 Phase 시작 전에 구현 계획(파일 목록, 클래스 구조, 주요 결정사항)을 먼저 보여주고 승인을 받은 뒤 코드를 작성한다.
- 각 Phase 끝나면 완료 기준 충족 여부와 실기기 테스트 방법을 정리해서 보고한다.
- 명세가 모호하거나 더 나은 방법이 있으면 구현 전에 질문한다. 임의로 범위를 넓히지 않는다.

## 코드 스타일
- 영리한 추상화보다 읽기 쉽고 명시적인 코드를 우선한다. 조금 비효율적이어도 읽기 쉬운 쪽을 택한다.
- 단, 프레임 단위로 도는 코드(분석, 렌더링)는 객체 할당 최소화 등 성능을 신경 쓴다.
- 규칙 하나 = 클래스 하나. 임계값은 모두 `GuideConstants.kt` 한 곳에 이름 있는 상수로 모은다. 매직 넘버 금지.
- 각 클래스 상단에 역할을 한두 줄 주석으로 설명한다.

## 기술 스택
- Kotlin, minSdk 26, targetSdk 최신
- UI: Jetpack Compose (카메라 프리뷰는 AndroidView로 GLSurfaceView 임베드)
- 카메라: CameraX (Preview + ImageAnalysis + ImageCapture)
- 렌더링: OpenGL ES 3.0 (OES 외부 텍스처 → 필터 셰이더 → 화면)
- 비전: ML Kit Pose Detection(스트림 모드), ML Kit Face Detection(분류 옵션 켜기: 눈 뜸 확률)
- 센서: SensorManager — Rotation Vector(롤/피치), Gyroscope(흔들림)
- 저장: MediaStore (Pictures/AIGuideCamera) + Room (PhotoRecord)

## 패키지 구조
```
camera/      CameraController (CameraX 바인딩, 촬영)
render/      GLRenderer, FilterShader, LutLoader, SkinSmoothShader,
             OffscreenFilterRenderer (촬영 저장·사후 편집 공용)
analysis/    FrameAnalyzer, PoseAnalyzer, FaceAnalyzer, LightAnalyzer, SensorReader
             FrameAnalysisResult (한 프레임의 모든 분석 결과를 담는 데이터 클래스)
guide/       GuideConstants, rules/ (Rule 인터페이스 + 규칙 클래스들), RuleEngine, IssueStabilizer
capture/     AutoCaptureStateMachine, BestShotSelector
filter/      FilterCatalog, FilterPreset
storage/     PhotoSaver (MediaStore 저장), PhotoRecord / PhotoRecordDao / AppDatabase (Room)
ui/          CameraScreen, AdviceBanner, HorizonLine, FilterPicker, ModeSwitch,
             GalleryScreen, PhotoDetailScreen, FilterEditScreen
```

## 핵심 원칙: 분석과 필터의 분리
```
카메라 원본 ─┬─► ImageAnalysis(저해상도) ─► AI 분석 ─► 조언 / 자동촬영 판단
            └─► GL 필터 셰이더 ─► 화면 프리뷰
```
- AI 분석은 항상 필터 적용 전 원본 프레임으로 한다.
- 촬영은 고해상도 원본을 찍은 뒤, 프리뷰와 동일한 셰이더로 오프스크린 렌더링해서 저장한다.

## 기능 명세

### 1. 모드
- 화면 상단 토글: 인물 / 음식. (자동 장면 판별은 MVP에서 제외)

### 2. 분석 (ImageAnalysis, 약 10fps 목표, STRATEGY_KEEP_ONLY_LATEST)
FrameAnalysisResult에 담을 것:
- 포즈 랜드마크 (코, 눈, 어깨, 엉덩이, 무릎, 발목) + 신뢰도
- 얼굴 박스, 좌/우 눈 뜸 확률, 얼굴 좌우 회전각(headEulerAngleY)
- 얼굴 영역 평균 밝기, 프레임 전체 평균 밝기
- 프레임 평균 R/G/B (그레이월드 방식 색온도 추정용)
- 롤/피치(도), 자이로 크기(rad/s)
- 직전 프레임 대비 포즈 랜드마크 평균 이동량 (피사체 움직임)

### 3. 규칙 (Rule 인터페이스)
```kotlin
interface Rule {
    val id: String
    val priority: Int   // 낮을수록 먼저 보여줌
    fun check(result: FrameAnalysisResult): Issue?   // 문제 없으면 null
}
data class Issue(val ruleId: String, val priority: Int, val message: String, val hint: HintType)
```
HintType: 화살표 방향(UP/DOWN/BACK/ROTATE_LEFT/ROTATE_RIGHT) 또는 NONE

인물 모드 규칙 (priority 순):
1. CameraHeightRule — 폰이 너무 높아 내려다보는 각도(피치 기준 + 머리/발목 화면 비율 휴리스틱) → "폰을 허리 높이로 내려주세요 ↓"
2. BodyCropRule — 무릎이 보이는데 발목이 없거나 화면 하단 경계에 걸림, 또는 관절 부위에서 잘림 → "발끝이 잘렸어요. 조금 뒤로 물러나 주세요"
3. HeadroomRule — 머리 위 여백이 너무 좁거나 너무 넓음 → 방향 조언 (임계값은 10절 GuideConstants 기준)
4. BacklightRule — 얼굴 밝기 / 전체 밝기 < 0.6 → "역광이에요. 빛을 등지지 않게 위치를 바꿔주세요"
5. HorizonRule — |롤| > 2° → "폰이 살짝 기울었어요" + 수평선 표시

음식 모드 규칙:
1. FoodAngleRule — 사용자가 고른 목표 각도(탑뷰 / 45°)와 피치 차이가 허용 오차 초과 → 방향 조언
2. ColorCastRule — R/B 비율로 누런/푸른 조명 감지 → "조명 색이 강해요. 자연광 쪽으로 옮겨보세요"
3. HorizonRule (공용)

모든 수치는 GuideConstants에 상수로 두고, 실기기 튜닝을 전제로 한다.

### 4. 안정화 (IssueStabilizer)
- 같은 문제가 연속 N프레임(기본 5) 감지돼야 표시, 연속 N프레임 사라져야 해제 (히스테리시스).
- 확정된 문제 중 priority가 가장 높은 1개만 UI로 전달.
- 문제가 없으면 "좋아요" 상태 전달.

### 5. 조언 UI
- 화면 하단 배너에 조언 1개 + 방향 화살표. 바뀔 때 부드러운 페이드.
- 수평 문제일 때 화면 중앙 수평선 표시, 맞으면 초록.
- 모든 규칙 통과 시 프리뷰 테두리 초록.
- 자동 촬영 대기 중 구도는 맞고 피사체 조건만 남았으면 "눈 뜨는 순간을 기다리는 중…" 같은 대기 문구 표시.

### 6. 자동 촬영 (AutoCaptureStateMachine)
원칙: 모든 조건이 "동시에" 충족될 때만 찍는다. 시간 초과 폴백 없음.

상태: AIMING → STABILIZING → CAPTURING → COOLDOWN → AIMING
- AIMING: 모든 규칙 통과 + 아래 촬영 조건 전부 충족 시 STABILIZING
  - 인물: 양쪽 눈 뜸 확률 > 0.7, |headEulerY| < 15°, 피사체 이동량 작음
  - 공통: 자이로 크기 < 임계값 (흔들림 없음)
- STABILIZING: 위 상태가 700ms 연속 유지되면 CAPTURING. 하나라도 깨지면 AIMING.
- CAPTURING: 3장 연속 촬영 → BestShotSelector가 선명도(라플라시안 분산) + 눈 뜸 점수로 1장 선택 → 필터 적용 후 저장.
- COOLDOWN: 구도가 크게 바뀔 때까지(포즈 중심 이동/피치 변화가 임계값 초과) 재촬영 안 함.
- 카운트다운 없음. 셔터음 있음.
- 자동 촬영 기본값 OFF, 화면 토글로 켬. 수동 셔터 버튼은 항상 동작.

### 7. 필터
- 방식: 3D LUT (512x512 PNG, 64³) + 셰이더 파라미터(그레인, 비네팅, 페이드, 강도).
- 필터 강도 슬라이더 (0~100%, 원본과 LUT 결과를 mix).
- SODA 감성의 오리지널 필터 10종을 만든다. 실제 SODA 필터를 복제하지 않는다.
  예시 방향: 맑은 피부톤, 따뜻한 필름, 차가운 필름, 파스텔, 빈티지 페이드, 흑백, 생기(채도 업), 음식용 따뜻함, 음식용 선명, 카페 무드
- LUT 생성: 디자이너가 없으므로 `tools/generate_luts.py` 파이썬 스크립트를 만들어 색온도·틴트·채도·대비·커브·페이드 파라미터로 LUT PNG를 생성한다. 필터 정의는 스크립트 안에 표로 명시적으로 둔다.
- 피부 보정: 얼굴 박스 기반 부드러운 마스크 영역에만 엣지 보존 블러(간이 bilateral) 적용, 강도 조절 가능. 얼굴형 변형(갸름하게, 눈 키우기)은 MVP 제외.
- 하단 필터 피커: 썸네일 가로 스크롤, 탭하면 즉시 프리뷰 반영.
- 음식 모드 진입 시 음식용 필터를 기본 선택 (사용자가 변경 가능, 모드별 마지막 선택 기억).

### 8. 저장 (교체)
- 기본: 필터 적용본만 갤러리(Pictures/AIGuideCamera)에 저장.
- 설정 "원본도 함께 저장" (기본 OFF): 켜면 원본도 같은 폴더에 저장.
- 파일명 규칙: IMG_yyyyMMdd_HHmmss.jpg (필터본), IMG_yyyyMMdd_HHmmss_orig.jpg (원본)
- Room DB 테이블 PhotoRecord로 둘을 연결한다:
  id, filteredUri, originalUri(nullable), filterId, filterIntensity, skinSmoothLevel, mode, createdAt
- 저장 후 좌하단 썸네일 갱신. 썸네일 탭 시 앱 내 갤러리로 이동.

### 9. 찍은 후 필터 변경 (신규)
- 앱 내 갤러리 화면: PhotoRecord 목록을 최신순 그리드로 표시.
- 사진 상세 화면: 크게 보기 + "필터 변경" 버튼.
  - originalUri가 있을 때만 버튼 활성화. 없으면 비활성 + "원본이 저장된 사진만 필터를 바꿀 수 있어요" 안내.
- 필터 편집 화면:
  - 원본을 불러와 촬영 때와 동일한 오프스크린 필터 렌더러로 미리보기.
  - 촬영 당시 필터/강도/피부보정 값을 초기값으로 표시.
  - 필터 피커, 강도 슬라이더, 피부보정 슬라이더 제공.
  - 피부보정용 얼굴 박스는 편집 시 원본에 ML Kit Face Detection을 한 번 실행해서 얻는다.
  - 저장 시 기존 파일을 덮어쓰지 않고 새 필터본을 저장, 새 PhotoRecord 생성(같은 originalUri 공유).
- 촬영 저장 경로와 편집 저장 경로는 같은 렌더러 클래스(OffscreenFilterRenderer)를 공유한다. 별도 구현 금지.

### 10. 구도 판정 초기값 (GuideConstants)
모든 값은 실기기 튜닝 전제의 초기값이다. 디버그 오버레이로 수치를 확인하며 조정한다.

인물 — 샷 종류는 보이는 관절로 먼저 판별한다
(발목 보임 = 전신 / 엉덩이까지 = 상반신 / 어깨까지 = 클로즈업). 기준표는 샷 종류별로 둔다.
- HEADROOM_MIN = 0.05, HEADROOM_MAX = 0.15 (화면 높이 대비, 전신 기준)
- FOOT_MARGIN_MIN = 0.02, FOOT_MARGIN_MAX = 0.08 (전신)
- JOINT_EDGE_TOLERANCE = 0.03 (무릎·발목·팔꿈치가 화면 끝에서 이 거리 이내면 "관절 잘림")
- FULL_BODY_HEIGHT_MIN = 0.60, FULL_BODY_HEIGHT_MAX = 0.85
- SUBJECT_CENTER_TOLERANCE = 0.05 (중앙선 또는 3분할선과의 가로 거리)
- HORIZON_TOLERANCE_DEG = 2.0
- CAMERA_DOWNWARD_PITCH_MAX_DEG = 10.0 (이보다 내려다보면 "폰을 내려주세요")
- BACKLIGHT_RATIO_MIN = 0.6 (얼굴 밝기 / 전체 밝기)

음식
- FOOD_TOPVIEW_PITCH_MIN = 85.0, FOOD_TOPVIEW_PITCH_MAX = 90.0
- FOOD_ANGLED_PITCH_MIN = 40.0, FOOD_ANGLED_PITCH_MAX = 50.0
- COLOR_CAST_RB_RATIO_MAX = 1.35, COLOR_CAST_RB_RATIO_MIN = 0.75
- UNDEREXPOSED_MEAN_LUMA_MIN = 0.30

자동 촬영 공통
- EYE_OPEN_PROB_MIN = 0.7
- HEAD_YAW_MAX_DEG = 15.0
- GYRO_STILL_MAX_RAD_S = 0.05
- STABLE_HOLD_MS = 700
- ISSUE_CONFIRM_FRAMES = 5, ISSUE_CLEAR_FRAMES = 5
- BURST_COUNT = 3

## 개발 단계

Phase 1 — 카메라 + GL 프리뷰 파이프라인
완료 기준: 후면 카메라 프리뷰가 GLSurfaceView로 나오고, 패스스루 셰이더 동작, 수동 촬영 후 MediaStore 저장.

Phase 2 — 필터
완료 기준: LUT 생성 스크립트 + 10종 필터, 강도 슬라이더, 프리뷰와 저장본 색감 일치, 피부 보정 동작. 저사양 기기에서도 프리뷰 30fps 유지.

Phase 3 — 분석 파이프라인
완료 기준: FrameAnalysisResult가 매 분석 프레임 채워짐. 디버그 오버레이(랜드마크 점, 롤/피치 수치, 밝기 값) 토글로 확인 가능.

Phase 4 — 규칙 엔진 + 조언 UI
완료 기준: 인물/음식 규칙 동작, 히스테리시스로 깜빡임 없음, 조언 1개만 표시, 규칙별 단위 테스트(FrameAnalysisResult 목 데이터 사용).

Phase 5 — 자동 촬영
완료 기준: 상태 머신 단위 테스트, 3장 중 베스트 선택, 쿨다운 동작, 조건 미충족 시 절대 촬영 안 됨.

Phase 6 — 마무리
완료 기준: 권한 처리, 설정 화면, 전면/후면 전환, 에러 처리, 성능 점검(분석이 프리뷰 프레임레이트를 떨어뜨리지 않음).

Phase 2.5 — 갤러리 + 사후 편집 (9절)
완료 기준: 갤러리 그리드, 상세 화면, 원본 있는 사진만 필터 변경 가능, 편집 저장 시 새 파일 + 새 PhotoRecord. 촬영과 같은 OffscreenFilterRenderer 사용.
