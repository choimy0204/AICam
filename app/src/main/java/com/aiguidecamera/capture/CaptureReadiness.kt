package com.aiguidecamera.capture

import com.aiguidecamera.analysis.FrameAnalysisResult
import com.aiguidecamera.guide.Advice
import com.aiguidecamera.guide.GuideConstants
import com.aiguidecamera.guide.ShootingMode
import kotlin.math.abs

/**
 * 한 프레임이 자동 촬영 조건을 모두 채웠는지, 아니라면 무엇을 기다리는지 판정한다.
 * 구도(규칙 통과) → 카메라 흔들림 → 피사체(인물 모드만: 얼굴·눈·고개·움직임) 순으로 본다.
 */
enum class Readiness(val waitingMessage: String?) {
    COMPOSITION_NOT_READY(null),
    CAMERA_SHAKING("흔들림이 멈추길 기다리는 중…"),
    NO_FACE("얼굴을 찾는 중…"),
    EYES_CLOSED("눈 뜨는 순간을 기다리는 중…"),
    FACE_TURNED("정면을 보는 순간을 기다리는 중…"),
    SUBJECT_MOVING("움직임이 멈추길 기다리는 중…"),
    READY(null),
}

object CaptureReadiness {

    fun evaluate(advice: Advice, result: FrameAnalysisResult, mode: ShootingMode): Readiness {
        if (advice != Advice.Good) return Readiness.COMPOSITION_NOT_READY
        if (result.gyroMagnitude >= GuideConstants.GYRO_STILL_MAX_RAD_S) return Readiness.CAMERA_SHAKING
        if (mode == ShootingMode.PORTRAIT) {
            val face = result.primaryFace ?: return Readiness.NO_FACE
            if (!isOpen(face.leftEyeOpenProbability) || !isOpen(face.rightEyeOpenProbability)) return Readiness.EYES_CLOSED
            if (abs(face.headEulerY) >= GuideConstants.HEAD_YAW_MAX_DEG) return Readiness.FACE_TURNED
            // 움직임을 잴 수 없는 프레임(관절이 안 잡힘)은 멈췄다고 단정하지 않는다.
            val motion = result.poseMotion ?: return Readiness.SUBJECT_MOVING
            if (motion > GuideConstants.POSE_MOTION_STILL_MAX) return Readiness.SUBJECT_MOVING
        }
        return Readiness.READY
    }

    /** 확률을 못 구한 경우(null)는 뜬 것으로 보지 않는다. */
    private fun isOpen(probability: Float?): Boolean =
        probability != null && probability > GuideConstants.EYE_OPEN_PROB_MIN
}
