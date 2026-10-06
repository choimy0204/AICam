package com.aiguidecamera.capture

import com.aiguidecamera.analysis.FrameAnalysisResult
import com.aiguidecamera.guide.GuideConstants
import kotlin.math.abs
import kotlin.math.hypot

enum class AutoCaptureState { AIMING, STABILIZING, CAPTURING, COOLDOWN }

/**
 * 자동 촬영 상태 머신: AIMING → STABILIZING → CAPTURING → COOLDOWN → AIMING.
 * 조건이 [holdMs] 동안 끊김 없이 유지될 때만 촬영을 지시하고, 시간 초과 폴백은 없다. 분석 스레드에서만 쓴다.
 */
class AutoCaptureStateMachine(private val holdMs: Long = GuideConstants.STABLE_HOLD_MS) {

    var state: AutoCaptureState = AutoCaptureState.AIMING
        private set

    private var stableSinceMs = 0L

    // 촬영 당시 구도. 포즈 중심이 없으면 NaN.
    private var anchorX = Float.NaN
    private var anchorY = Float.NaN
    private var anchorPitch = 0f

    // 매 프레임 계산하는 포즈 중심 (할당을 피하려고 필드에 담는다).
    private var centerX = Float.NaN
    private var centerY = Float.NaN

    /** 프레임마다 호출한다. true를 돌려준 순간이 "지금 찍어라" 신호다. */
    fun onFrame(isReady: Boolean, result: FrameAnalysisResult): Boolean {
        val now = result.timestampMs
        when (state) {
            AutoCaptureState.AIMING -> if (isReady) {
                state = AutoCaptureState.STABILIZING
                stableSinceMs = now
            }
            AutoCaptureState.STABILIZING -> when {
                !isReady -> state = AutoCaptureState.AIMING
                now - stableSinceMs >= holdMs -> {
                    updatePoseCenter(result)
                    anchorX = centerX
                    anchorY = centerY
                    anchorPitch = result.pitchDeg
                    state = AutoCaptureState.CAPTURING
                    return true
                }
            }
            AutoCaptureState.CAPTURING -> Unit
            AutoCaptureState.COOLDOWN -> if (compositionChanged(result)) state = AutoCaptureState.AIMING
        }
        return false
    }

    /** 촬영(성공·실패 무관)이 끝나면 호출한다. 같은 구도에서 연달아 찍지 않도록 쿨다운에 들어간다. */
    fun onCaptureFinished() {
        if (state == AutoCaptureState.CAPTURING) state = AutoCaptureState.COOLDOWN
    }

    fun reset() {
        state = AutoCaptureState.AIMING
    }

    private fun compositionChanged(result: FrameAnalysisResult): Boolean {
        if (abs(result.pitchDeg - anchorPitch) > GuideConstants.COOLDOWN_PITCH_CHANGE_DEG) return true
        updatePoseCenter(result)
        // 어느 한쪽이라도 포즈를 못 잡았으면 이동량을 알 수 없으니 바뀌지 않은 것으로 본다(검출 깜빡임 방지).
        if (anchorX.isNaN() || centerX.isNaN()) return false
        return hypot(centerX - anchorX, centerY - anchorY) > GuideConstants.COOLDOWN_POSE_SHIFT_MIN
    }

    /** 신뢰도 기준을 넘는 관절들의 평균 위치. 하나도 없으면 NaN. */
    private fun updatePoseCenter(result: FrameAnalysisResult) {
        var sumX = 0f
        var sumY = 0f
        var count = 0
        for (landmark in result.landmarks.values) {
            if (landmark.confidence < GuideConstants.LANDMARK_CONFIDENCE_MIN) continue
            sumX += landmark.x
            sumY += landmark.y
            count++
        }
        centerX = if (count > 0) sumX / count else Float.NaN
        centerY = if (count > 0) sumY / count else Float.NaN
    }
}
