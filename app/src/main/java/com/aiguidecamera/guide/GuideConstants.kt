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

    // ── 음식 ─────────────────────────────────────────────────────────────
    const val FOOD_TOPVIEW_PITCH_MIN = 85.0f
    const val FOOD_TOPVIEW_PITCH_MAX = 90.0f
    const val FOOD_ANGLED_PITCH_MIN = 40.0f
    const val FOOD_ANGLED_PITCH_MAX = 50.0f
    const val COLOR_CAST_RB_RATIO_MAX = 1.35f
    const val COLOR_CAST_RB_RATIO_MIN = 0.75f
    const val UNDEREXPOSED_MEAN_LUMA_MIN = 0.30f

    // ── 자동 촬영 공통 ───────────────────────────────────────────────────
    const val EYE_OPEN_PROB_MIN = 0.7f
    const val HEAD_YAW_MAX_DEG = 15.0f
    const val GYRO_STILL_MAX_RAD_S = 0.05f
    const val STABLE_HOLD_MS = 700L
    const val ISSUE_CONFIRM_FRAMES = 5
    const val ISSUE_CLEAR_FRAMES = 5
    const val BURST_COUNT = 3

    // ── 분석 파이프라인 ──────────────────────────────────────────────────
    /** 분석 프레임 간 최소 간격 (약 10fps). */
    const val ANALYSIS_INTERVAL_MS = 100L

    /** 밝기·색 평균을 낼 때 가로·세로로 이 픽셀 간격마다 하나씩 샘플링한다. */
    const val LIGHT_SAMPLE_STEP_PX = 8

    /** 이보다 신뢰도가 낮은 포즈 랜드마크는 "안 보임"으로 취급한다. */
    const val LANDMARK_CONFIDENCE_MIN = 0.5f
}
