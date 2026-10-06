package com.aiguidecamera.guide

/**
 * 구도 판정·분석·자동 촬영에 쓰는 모든 임계값. 실기기 튜닝 전제의 초기값이다 (SPEC 10절).
 * 디버그 오버레이로 실제 수치를 보면서 여기 값만 바꿔 조정한다.
 */
object GuideConstants {

    // ── 인물: 구도 (화면 높이·너비 대비 비율) ─────────────────────────────
    const val HEADROOM_MIN = 0.05f
    const val HEADROOM_MAX = 0.15f
    const val FOOT_MARGIN_MIN = 0.02f
    const val FOOT_MARGIN_MAX = 0.08f
    const val JOINT_EDGE_TOLERANCE = 0.03f
    const val FULL_BODY_HEIGHT_MIN = 0.60f
    const val FULL_BODY_HEIGHT_MAX = 0.85f
    const val SUBJECT_CENTER_TOLERANCE = 0.05f
    const val HORIZON_TOLERANCE_DEG = 2.0f
    const val CAMERA_DOWNWARD_PITCH_MAX_DEG = 10.0f
    const val BACKLIGHT_RATIO_MIN = 0.6f

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
