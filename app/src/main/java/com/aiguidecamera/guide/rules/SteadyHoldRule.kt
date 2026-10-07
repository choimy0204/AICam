package com.aiguidecamera.guide.rules

import com.aiguidecamera.analysis.FrameAnalysisResult
import com.aiguidecamera.guide.GuideConstants

/** 야경: 어두우면 셔터가 길어져 작은 손떨림도 번지므로, 폰이 조금이라도 흔들리면 단단히 잡으라고 한다. */
class SteadyHoldRule : Rule {
    override val id = ID
    override val priority = 1

    override fun check(result: FrameAnalysisResult): Issue? {
        if (result.gyroMagnitude <= GuideConstants.NIGHT_GYRO_STILL_MAX_RAD_S) return null
        return Issue(id, priority, "흔들려요. 두 손으로 잡고 팔꿈치를 몸에 붙이거나 어딘가에 기대세요", HintType.NONE)
    }

    companion object {
        const val ID = "steady_hold"
    }
}
