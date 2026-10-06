package com.aiguidecamera.guide

import com.aiguidecamera.analysis.BodyPart
import com.aiguidecamera.analysis.FaceInfo
import com.aiguidecamera.analysis.FrameAnalysisResult
import com.aiguidecamera.analysis.Landmark
import com.aiguidecamera.analysis.NormRect

/** 규칙 테스트용 FrameAnalysisResult 목 데이터. 기본값은 "아무 문제 없는" 프레임이다. */
object TestFrames {

    fun frame(
        landmarks: Map<BodyPart, Landmark> = emptyMap(),
        faces: List<FaceInfo> = emptyList(),
        faceBrightness: Float? = null,
        frameBrightness: Float = 0.5f,
        red: Float = 0.5f,
        green: Float = 0.5f,
        blue: Float = 0.5f,
        roll: Float = 0f,
        pitch: Float = 0f,
        gyro: Float = 0f,
        motion: Float? = null,
        time: Long = 0L,
    ) = FrameAnalysisResult(
        timestampMs = time,
        landmarks = landmarks,
        faces = faces,
        faceBrightness = faceBrightness,
        frameBrightness = frameBrightness,
        meanRed = red,
        meanGreen = green,
        meanBlue = blue,
        rollDeg = roll,
        deviceRollDeg = roll,
        pitchDeg = pitch,
        gyroMagnitude = gyro,
        poseMotion = motion,
    )

    /** 구도가 잘 맞은 전신: 정수리 여백 약 0.13, 발 아래 여백 0.14, 관절이 모두 화면 안쪽. */
    fun goodFullBody(): MutableMap<BodyPart, Landmark> = mutableMapOf(
        BodyPart.NOSE to visible(0.50f, 0.18f),
        BodyPart.LEFT_EYE to visible(0.48f, 0.17f),
        BodyPart.RIGHT_EYE to visible(0.52f, 0.17f),
        BodyPart.LEFT_SHOULDER to visible(0.42f, 0.26f),
        BodyPart.RIGHT_SHOULDER to visible(0.58f, 0.26f),
        BodyPart.LEFT_ELBOW to visible(0.38f, 0.38f),
        BodyPart.RIGHT_ELBOW to visible(0.62f, 0.38f),
        BodyPart.LEFT_HIP to visible(0.45f, 0.50f),
        BodyPart.RIGHT_HIP to visible(0.55f, 0.50f),
        BodyPart.LEFT_KNEE to visible(0.45f, 0.68f),
        BodyPart.RIGHT_KNEE to visible(0.55f, 0.68f),
        BodyPart.LEFT_ANKLE to visible(0.45f, 0.86f),
        BodyPart.RIGHT_ANKLE to visible(0.55f, 0.86f),
    )

    /** 엉덩이 위까지만 보이는 상반신. */
    fun upperBodyOnly(): MutableMap<BodyPart, Landmark> = goodFullBody().apply {
        remove(BodyPart.LEFT_KNEE)
        remove(BodyPart.RIGHT_KNEE)
        remove(BodyPart.LEFT_ANKLE)
        remove(BodyPart.RIGHT_ANKLE)
    }

    fun visible(x: Float, y: Float) = Landmark(x, y, confidence = 0.95f)

    fun hidden(x: Float, y: Float) = Landmark(x, y, confidence = 0.1f)

    fun face(
        top: Float,
        bottom: Float,
        left: Float = 0.45f,
        right: Float = 0.55f,
        leftEye: Float? = 0.9f,
        rightEye: Float? = 0.9f,
        yaw: Float = 0f,
    ) = FaceInfo(
        box = NormRect(left, top, right, bottom),
        leftEyeOpenProbability = leftEye,
        rightEyeOpenProbability = rightEye,
        headEulerY = yaw,
    )
}
