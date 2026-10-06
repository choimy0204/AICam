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
