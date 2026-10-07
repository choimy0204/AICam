package com.aiguidecamera.camera

import com.aiguidecamera.guide.GuideConstants

/** 풍경: 하늘이 하얗게 날아가면 노출을 한 단계씩 내리고, 날아간 곳이 거의 없으면 다시 0까지 올린다. 노출이 자리 잡을 시간을 두고 바꾼다. */
class AutoExposureTuner {

    var ev = 0f
        private set

    private var lastChangeMs = 0L

    /** 노출을 바꿨으면 true. 새 값은 [ev]. */
    fun update(highlightClipRatio: Float, nowMs: Long): Boolean {
        if (nowMs - lastChangeMs < GuideConstants.AUTO_EV_SETTLE_MS) return false
        val target = when {
            highlightClipRatio > GuideConstants.AUTO_EV_CLIP_HIGH -> ev - GuideConstants.AUTO_EV_STEP
            highlightClipRatio < GuideConstants.AUTO_EV_CLIP_LOW -> ev + GuideConstants.AUTO_EV_STEP
            else -> ev
        }.coerceIn(GuideConstants.AUTO_EV_MIN, 0f)
        if (target == ev) return false
        ev = target
        lastChangeMs = nowMs
        return true
    }

    fun reset() {
        ev = 0f
        lastChangeMs = 0L
    }
}
