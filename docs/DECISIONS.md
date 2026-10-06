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
