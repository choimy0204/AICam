package com.aiguidecamera.analysis

/**
 * 분석 프레임 하나의 모든 결과. 규칙 엔진·자동 촬영·디버그 오버레이가 이것만 보고 판단한다.
 *
 * 좌표는 모두 "똑바로 선 사진 기준 0~1" (0,0 = 왼쪽 위, 거울 반전 없음 = 저장본과 같은 방향).
 */
data class FrameAnalysisResult(
    val timestampMs: Long,
    /** 포즈 랜드마크. 신뢰도와 관계없이 검출된 것은 모두 담는다 (판단 쪽에서 신뢰도를 본다). 사람이 없으면 빈 맵. */
    val landmarks: Map<BodyPart, Landmark>,
    /** 검출된 얼굴. 큰 얼굴이 앞. */
    val faces: List<FaceInfo>,
    /** 가장 큰 얼굴 영역의 평균 밝기 0~1. 얼굴이 없으면 null. */
    val faceBrightness: Float?,
    /** 프레임 전체 평균 밝기 0~1. */
    val frameBrightness: Float,
    /** 프레임 평균 R/G/B 0~1 (그레이월드 색온도 추정용). */
    val meanRed: Float,
    val meanGreen: Float,
    val meanBlue: Float,
    /** 폰을 잡은 방향(세로/가로) 기준 기울기. -45~45도, 0이면 수평. */
    val rollDeg: Float,
    /** 정규화 전 기기 롤(-180~180). 화면(세로 고정)에 실제 수평선을 그릴 때 쓴다. */
    val deviceRollDeg: Float,
    /** 후면 카메라가 수평선 아래를 향하는 각도. 0 = 정면, 90 = 바로 아래(탑뷰), 음수 = 위를 봄. */
    val pitchDeg: Float,
    /** 직전 분석 이후 자이로 각속도 크기의 최댓값 (rad/s). */
    val gyroMagnitude: Float,
    /** 직전 프레임 대비 포즈 랜드마크 평균 이동량 (화면 비율). 비교할 수 없으면 null. */
    val poseMotion: Float?,
) {
    val primaryFace: FaceInfo? get() = faces.firstOrNull()
}

/** 규칙에서 쓰는 포즈 관절. ML Kit PoseLandmark 중 필요한 것만. */
enum class BodyPart {
    NOSE, LEFT_EYE, RIGHT_EYE,
    LEFT_SHOULDER, RIGHT_SHOULDER,
    LEFT_ELBOW, RIGHT_ELBOW,
    LEFT_HIP, RIGHT_HIP,
    LEFT_KNEE, RIGHT_KNEE,
    LEFT_ANKLE, RIGHT_ANKLE,
}

data class Landmark(val x: Float, val y: Float, val confidence: Float)

data class FaceInfo(
    val box: NormRect,
    /** 0~1. 검출기가 판단하지 못하면 null. */
    val leftEyeOpenProbability: Float?,
    val rightEyeOpenProbability: Float?,
    /** 얼굴 좌우 회전각(도). */
    val headEulerY: Float,
)

/** 0~1 정규화 사각형. android.graphics.RectF 대신 써서 규칙을 JVM 단위 테스트할 수 있게 한다. */
data class NormRect(val left: Float, val top: Float, val right: Float, val bottom: Float) {
    val width: Float get() = right - left
    val height: Float get() = bottom - top

    fun contains(x: Float, y: Float): Boolean = x >= left && x < right && y >= top && y < bottom
}
