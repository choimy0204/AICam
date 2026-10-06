# 결정 기록

명세(SPEC.md)에서 정해지지 않았거나 서로 충돌한 부분을 어떻게 처리했는지 남긴다.

## 공통
- 패키지/applicationId: `com.aiguidecamera`
- 빌드: AGP 8.12.3, Kotlin 2.2.10, Gradle 8.14.5, compileSdk/targetSdk 36, minSdk 26. JDK는 Android Studio JBR.
- 9절(갤러리·사후 편집)은 **Phase 2.5**로 둔다. 필터 렌더러가 있어야 하므로 Phase 2 뒤.
- 헤드룸 임계값: 3절(3%/20%)과 10절(5%/15%)이 충돌 → **10절 값** 사용.

## Phase 1
- 프리뷰·촬영 모두 4:3. 프리뷰 영역은 3:4 박스, 렌더러는 center-crop.
- 화면은 세로 고정. 폰을 가로로 잡으면 `OrientationEventListener`로 촬영본 방향만 맞춘다.
- 프리뷰 회전은 CameraX `TransformationInfo.rotationDegrees`를 정점 행렬로 적용한다
  (SurfaceTexture 변환 행렬에는 센서 회전이 들어 있지 않다).
- 촬영: `ImageCapture` 메모리 콜백 → `toBitmap()` → `rotationDegrees`만큼 회전 → `OffscreenFilterRenderer` → JPEG(품질 95).
- 원본 저장본도 똑바로 세운 비트맵을 JPEG로 다시 인코딩해 저장한다 (EXIF 회전에 의존하지 않음 → 사후 편집이 단순해짐).
- `OffscreenFilterRenderer`는 Application이 하나만 들고 프로세스 수명 동안 유지한다.
- API 26~28은 `WRITE_EXTERNAL_STORAGE`로 Pictures/AIGuideCamera에 직접 쓰고 MediaStore에 등록한다.
- PhotoRecord(Room)는 Phase 1부터 기록한다. 필터가 없으므로 `filterId = "none"`.
- 셔터음: `MediaActionSound`.

## Phase 2
- LUT: 512x512 PNG(8x8 타일, 64³)를 `tools/generate_luts.py`(표준 라이브러리만)로 생성해 `assets/luts/`에 커밋한다.
  앱은 PNG를 디코딩해 `GL_TEXTURE_3D`(RGB8)로 올리고 하드웨어 3선형 보간으로 샘플링한다. 디코딩 결과는 두 GL 컨텍스트가 공유.
- 페이드는 LUT에만 굽는다 (셰이더 파라미터는 강도·그레인·비네팅만). 강도 = 원본과 LUT 결과의 mix, 그레인·비네팅에도 강도를 곱한다.
- 셰이더 한 패스: 피부 보정 → LUT mix → 비네팅 → 그레인. 프리뷰(OES)와 저장(2D)은 `sampleSource()` 헤더만 다르고 본문 문자열이 같다.
  정밀도는 highp (저장본 그라데이션 밴딩 방지).
- 비네팅·그레인·얼굴 마스크는 "똑바로 선 사진 기준 좌표"로 계산한다. 프리뷰는 center-crop·거울 반전을 역산한 변환을 넘긴다
  → 화면과 저장본에서 같은 위치에 같은 효과. 그레인 셀 크기·블러 반경도 해상도 대비 비율이라 프리뷰와 저장본 느낌이 같다.
- "원본" 필터(id `none`)를 목록 맨 앞에 둔다 (10종 + 원본).
- 기본값: 인물 `clear_skin`, 음식 `food_warm`, 강도 0.8, 피부 보정 0.4. 모드별 마지막 필터·강도는 SharedPreferences에 기억.
- 피부 보정은 인물 모드에서만 (음식 모드는 0). 저장 시 고해상도 원본을 1280px로 줄여 ML Kit Face Detection을 한 번 돌려 얼굴 박스를 얻는다.
  검출 실패 시 피부 보정 없이 저장. 프리뷰 피부 보정은 Phase 3의 실시간 얼굴 박스가 들어오면 동작한다 (`GLRenderer.setFaceBoxes`).
- 필터 썸네일: 색 띠 견본 이미지를 `OffscreenFilterRenderer`로 렌더링 (결과물과 같은 경로).
- 강도·피부 보정 슬라이더는 프리뷰 위 반투명 패널. 선택된 필터 썸네일을 다시 탭하면 열고 닫는다 (화면 높이 확보).

## Phase 2.5
- "원본도 함께 저장" 설정(`AppSettings`, 기본 OFF)과 최소 설정 화면을 Phase 6보다 앞당겨 만들었다.
  원본이 있어야 사후 편집을 실기기에서 확인할 수 있기 때문. 카메라 화면 오른쪽 아래 톱니바퀴로 들어간다.
- 화면 이동: navigation-compose. 카메라 → (썸네일) 갤러리 → 상세 → 필터 변경. 편집 저장 후에는 갤러리로 돌아간다.
- 편집 미리보기는 원본을 긴 변 1600px 이상이 되도록 inSampleSize로 줄여 읽고, 값이 바뀔 때마다 `OffscreenFilterRenderer`로
  다시 그린다(collectLatest). 저장은 원본 전체 해상도로 같은 렌더러를 한 번 더 돌린다.
- 얼굴 박스는 축소 원본에서 한 번만 검출해(0~1 좌표라 해상도 무관) 미리보기와 저장에 같이 쓴다.
- 편집 저장 파일명은 저장 시각 기준 `IMG_yyyyMMdd_HHmmss.jpg`, 새 PhotoRecord는 원본 Uri·모드를 그대로 이어받는다.
- 편집 화면에서는 모드와 무관하게 피부 보정 슬라이더를 보여준다(음식 사진이면 얼굴이 없어 효과 없음).

## Phase 3 — 분석 파이프라인 + 디버그 오버레이

- **ImageAnalysis 640x480(4:3), KEEP_ONLY_LATEST, 10fps 스로틀**: 포즈·얼굴 동시 처리에 충분하고 저사양에서도 밀리지 않는 크기. 프리뷰·촬영과 같은 4:3이라 0~1 좌표를 그대로 공유한다.
- **좌표계 통일**: 모든 분석 좌표는 "똑바로 선 사진 기준 0~1, 거울 반전 없음". 저장본·오프스크린 피부 보정과 같은 좌표라 얼굴 박스를 프리뷰 셰이더에 그대로 넘긴다. 거울 반전은 화면에 그릴 때만(디버그 오버레이) 적용.
- **ML Kit Pose(base, STREAM_MODE) + Face(FAST, 분류 ON)를 병렬 실행**, `Tasks.whenAllComplete` 뒤 같은 분석 스레드에서 밝기 계산 후 ImageProxy를 닫는다. 음식 모드는 포즈·얼굴을 건너뛴다.
- **밝기·색은 YUV 직접 샘플링(8px 격자)**: Bitmap 변환 없이 할당 0. 얼굴 밝기는 버퍼 좌표를 회전 반영해 얼굴 박스와 비교.
- **센서**: 회전 벡터 → pitch = asin(R[8]) (0 = 정면, 90 = 탑뷰), roll = atan2(R[6], R[7])을 가장 가까운 90° 기준 ±45°로 정규화(가로 촬영 대응). 자이로는 분석 간격 사이 최댓값을 쓴다(순간 흔들림 놓치지 않게). 화면이 보일 때만 등록.
- **poseMotion**: 두 프레임 모두 신뢰도 ≥ 0.5인 관절의 평균 이동 거리. 자동 촬영의 "피사체 정지" 판단용.
- **디버그 오버레이**: 프리뷰 좌상단 "DBG" 토글. 관절 점(초록 = 신뢰, 빨강 = 낮음), 얼굴 박스(노랑), roll/pitch/gyro/밝기/RGB/눈 뜸/yaw 수치. 임계값 튜닝 용도.

## Phase 4 — 규칙 엔진 + 조언 UI

- **FaceInfo.box를 RectF → NormRect(순수 Kotlin)로 교체**: android.graphics.RectF는 JVM 단위 테스트에서 동작하지 않아(스텁) 규칙 테스트가 불가능. 렌더러로 넘길 때만 RectF로 변환한다.
- **샷 종류 판별(ShotClassifier)**: 신뢰도 ≥ 0.5 관절 기준, 발목 → 전신 / 엉덩이 → 상반신 / 어깨 → 클로즈업. 헤드룸 기준표는 샷별로 두라고 했지만 SPEC에 전신 값만 있어 일단 모든 샷에 같은 값(0.05~0.15)을 쓴다. 실기기 튜닝 때 샷별로 나눈다.
- **정수리 추정**: 얼굴 박스 top − 박스 높이 × 0.25(머리카락). 얼굴이 없으면 코 − (어깨선−코) × 0.65.
- **CameraHeightRule 휴리스틱**: 피치 > 10° 또는 (전신일 때) (코→엉덩이)/(엉덩이→발목) > 1.0이면 내려다본 것으로 판단.
- **HorizonRule은 |피치| > 60°면 건너뜀**: 탑뷰에서는 중력이 화면과 수직이라 롤 값이 의미 없다. 롤 음수(시계 방향으로 기움) → ROTATE_LEFT.
- **수평선은 정규화 전 롤(deviceRollDeg)로 회전**: 화면이 세로 고정이라 가로로 잡아도 실제 수평과 맞게 그린다. 판정·초록 표시는 정규화된 롤로.
- **IssueStabilizer**: 규칙별 연속 감지/소실 카운터. "좋아요"도 문제 없는 프레임이 5연속일 때만 표시(시작 직후·전환 직후 초록 깜빡임 방지). 그 사이는 Pending(배너 없음).
- **음식 목표 각도 토글(탑뷰/45°)**: 음식 모드에서 모드 토글 아래 표시. 모드·각도 전환 시 안정화기 초기화.
- **범위 밖으로 둔 것**: UNDEREXPOSED_MEAN_LUMA_MIN, SUBJECT_CENTER_TOLERANCE, FULL_BODY_HEIGHT, FOOT_MARGIN_MAX는 SPEC 규칙 목록에 대응 규칙이 없어 상수만 두고 쓰지 않는다. 사람이 없는 장면에서도 "좋아요"가 나올 수 있다(자동 촬영은 Phase 5의 눈 뜸 조건으로 막힌다).
- **알려진 한계**: 피치는 후면 카메라 기준. 전면 전환(Phase 6) 때 부호를 바꾼다.
