package com.aiguidecamera.guide

/**
 * 구도 판정·분석·자동 촬영에 쓰는 모든 임계값. 실기기 튜닝 전제의 초기값이다 (SPEC 10절).
 * 디버그 오버레이로 실제 수치를 보면서 여기 값만 바꿔 조정한다.
 */
object GuideConstants {

    // ── 인물: 구도 (화면 높이·너비 대비 비율) ─────────────────────────────
    /** 전신 정수리 위 최소 여백. 최대는 두지 않는다 (전신은 위를 넉넉히 두는 게 정석). */
    const val HEADROOM_MIN = 0.05f
    /** 발 아래 여백은 발목 기준이다. 발바닥은 발목보다 약 0.03~0.04 아래라 SPEC 값(0.02~0.08)보다 넉넉히 잡는다. */
    const val FOOT_MARGIN_MIN = 0.02f
    const val FOOT_MARGIN_MAX = 0.12f
    const val JOINT_EDGE_TOLERANCE = 0.03f
    const val FULL_BODY_HEIGHT_MIN = 0.60f
    const val FULL_BODY_HEIGHT_MAX = 0.85f
    const val SUBJECT_CENTER_TOLERANCE = 0.05f

    /** 상반신·클로즈업에서 눈의 목표 높이 (위쪽 3분할선)와 허용 오차. */
    const val EYE_LINE_TARGET = 1f / 3f
    const val EYE_LINE_TOLERANCE = 0.06f

    /** 눈 관절이 없을 때 얼굴 박스 위에서 눈까지의 거리 (얼굴 박스 높이 대비). */
    const val EYES_BELOW_FACE_TOP_RATIO = 0.4f
    const val HORIZON_TOLERANCE_DEG = 2.0f
    const val CAMERA_DOWNWARD_PITCH_MAX_DEG = 10.0f
    const val BACKLIGHT_RATIO_MIN = 0.6f

    /** 전신이 아닐 때 얼굴 박스 높이가 이보다 작으면 "인물이 너무 작다"고 본다 (예: 멀리 있는 사람, 거울 속 얼굴). */
    const val SUBJECT_FACE_HEIGHT_MIN = 0.07f

    /** 전신에서 코→발목 길이가 이보다 짧으면 "인물이 너무 작다"고 본다. */
    const val SUBJECT_BODY_SPAN_MIN = 0.45f

    /** 화면 전체가 이보다 어두우면 역광 비율을 믿을 수 없어 판정하지 않는다. */
    const val BACKLIGHT_FRAME_BRIGHTNESS_MIN = 0.05f

    /** 전신에서 (코→엉덩이 길이) / (엉덩이→발목 길이)가 이보다 크면 위에서 내려다본 것으로 본다 (다리가 짧아 보임). */
    const val LOOKDOWN_TORSO_LEG_RATIO_MAX = 1.0f

    /** 얼굴 박스 위로 머리카락이 차지하는 높이 (얼굴 박스 높이 대비). 정수리 추정용. */
    const val HEAD_TOP_ABOVE_FACE_RATIO = 0.25f

    /** 얼굴 박스가 없을 때 정수리 추정: 코 위로 (어깨선→코 거리) × 이 비율. */
    const val HEAD_TOP_ABOVE_NOSE_RATIO = 0.65f

    /** 피치가 이보다 크면(바닥을 내려다봄) 롤로 수평을 판단할 수 없어 수평 규칙을 건너뛴다. */
    const val HORIZON_MAX_PITCH_DEG = 60.0f

    // ── 음식 ─────────────────────────────────────────────────────────────
    const val FOOD_TOPVIEW_PITCH_MIN = 85.0f
    const val FOOD_TOPVIEW_PITCH_MAX = 90.0f
    const val FOOD_ANGLED_PITCH_MIN = 40.0f
    const val FOOD_ANGLED_PITCH_MAX = 50.0f
    const val COLOR_CAST_RB_RATIO_MAX = 1.35f
    const val COLOR_CAST_RB_RATIO_MIN = 0.75f
    const val UNDEREXPOSED_MEAN_LUMA_MIN = 0.30f

    /** 파랑 평균이 이보다 작으면 R/B 비율이 의미 없어 색 판정을 하지 않는다. */
    const val COLOR_CAST_CHANNEL_MIN = 0.02f

    // ── 풍경 ─────────────────────────────────────────────────────────────
    /** 하늘/땅 경계를 찾을 때 화면을 위→아래로 나누는 띠 개수. */
    const val SKY_LINE_BAND_COUNT = 24

    /** 맨 위·맨 아래 이 개수의 띠 안쪽 경계는 무시한다 (가장자리의 작은 밝은 조각에 끌려가지 않게). */
    const val SKY_LINE_EDGE_BANDS = 2

    /** 경계 위 평균 밝기가 아래보다 이만큼(0~1) 이상 밝아야 하늘/땅 경계로 인정한다. */
    const val SKY_LINE_CONTRAST_MIN = 0.12f

    /** 수평선이 화면 가운데(0.5)에서 이 범위 안이면 "가운데에 걸쳤다"고 본다. 3분할선(0.33/0.67)은 통과. */
    const val SKY_LINE_CENTER_TOLERANCE = 0.09f

    /** 이 밝기(0~255) 이상인 샘플은 하얗게 날아간 것으로 센다. */
    const val HIGHLIGHT_CLIP_LUMA = 250

    /** 날아간 샘플 비율이 이보다 크면 노출 경고. */
    const val HIGHLIGHT_CLIP_RATIO_MAX = 0.15f

    // ── 자동 촬영 공통 ───────────────────────────────────────────────────
    const val EYE_OPEN_PROB_MIN = 0.7f
    const val HEAD_YAW_MAX_DEG = 15.0f
    const val GYRO_STILL_MAX_RAD_S = 0.05f
    const val STABLE_HOLD_MS = 700L
    const val ISSUE_CONFIRM_FRAMES = 5
    const val ISSUE_CLEAR_FRAMES = 5
    const val BURST_COUNT = 3

    /** 분석 프레임(약 100ms) 사이 관절 평균 이동량이 이보다 작으면 피사체가 멈춘 것으로 본다. 화면 대비 비율. */
    const val POSE_MOTION_STILL_MAX = 0.01f

    /** 쿨다운 해제: 포즈 중심이 촬영 당시보다 이만큼(화면 대비) 옮겨가면 구도가 바뀐 것으로 본다. */
    const val COOLDOWN_POSE_SHIFT_MIN = 0.15f

    /** 쿨다운 해제: 피치가 촬영 당시보다 이만큼 바뀌면 구도가 바뀐 것으로 본다. */
    const val COOLDOWN_PITCH_CHANGE_DEG = 15.0f

    /** 베스트 컷 점수 가중치. 선명도는 연사 중 최댓값 대비 비율(0~1), 눈 뜸은 확률(0~1). */
    const val BEST_SHOT_SHARPNESS_WEIGHT = 0.5f
    const val BEST_SHOT_EYE_WEIGHT = 0.5f

    /** 베스트 컷 채점용으로 연사 사진을 줄여 디코딩할 때 긴 변의 최소 크기. */
    const val BEST_SHOT_ANALYSIS_SIDE_PX = 1280

    // ── 분석 파이프라인 ──────────────────────────────────────────────────
    /** 분석 프레임 간 최소 간격 (약 10fps). */
    const val ANALYSIS_INTERVAL_MS = 100L

    /** 밝기·색 평균을 낼 때 가로·세로로 이 픽셀 간격마다 하나씩 샘플링한다. */
    const val LIGHT_SAMPLE_STEP_PX = 8

    /** 이보다 신뢰도가 낮은 포즈 랜드마크는 "안 보임"으로 취급한다. */
    const val LANDMARK_CONFIDENCE_MIN = 0.5f
}
