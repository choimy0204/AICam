package com.aiguidecamera.camera

import com.aiguidecamera.analysis.NormRect
import com.aiguidecamera.guide.GuideConstants
import kotlin.math.hypot

/** 인물: 얼굴에 초점·노출 측정을 언제 다시 걸고 언제 풀지 정한다. 얼굴이 크게 움직였을 때만 다시 걸어 초점이 헤매지 않게 한다. */
class FaceMeteringPolicy {

    enum class Action { NONE, METER, CANCEL }

    private var metering = false
    private var meteredCenterX = 0f
    private var meteredCenterY = 0f
    private var lastMeterMs = 0L
    private var lastFaceSeenMs = 0L

    fun update(face: NormRect?, nowMs: Long): Action {
        if (face == null) {
            if (!metering || nowMs - lastFaceSeenMs <= GuideConstants.FACE_METERING_LOST_MS) return Action.NONE
            metering = false
            return Action.CANCEL
        }
        lastFaceSeenMs = nowMs
        val centerX = (face.left + face.right) / 2f
        val centerY = (face.top + face.bottom) / 2f
        val moved = hypot(centerX - meteredCenterX, centerY - meteredCenterY) > GuideConstants.FACE_METERING_MOVE_MIN
        if (metering && !moved) return Action.NONE
        if (nowMs - lastMeterMs < GuideConstants.FACE_METERING_MIN_INTERVAL_MS) return Action.NONE
        metering = true
        meteredCenterX = centerX
        meteredCenterY = centerY
        lastMeterMs = nowMs
        return Action.METER
    }

    fun reset() {
        metering = false
        lastMeterMs = 0L
        lastFaceSeenMs = 0L
    }
}
