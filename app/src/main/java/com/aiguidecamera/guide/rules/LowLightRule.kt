package com.aiguidecamera.guide.rules

import com.aiguidecamera.analysis.FrameAnalysisResult
import com.aiguidecamera.guide.GuideConstants

/** 야경: 화면이 거의 까맣다면 담을 빛이 없는 것이므로 불빛이 있는 쪽을 함께 담으라고 한다. */
class LowLightRule : Rule {
    override val id = ID
    override val priority = 4

    override fun check(result: FrameAnalysisResult): Issue? {
        if (result.frameBrightness >= GuideConstants.NIGHT_FRAME_BRIGHTNESS_MIN) return null
        return Issue(id, priority, "너무 어두워요. 가로등·간판 같은 불빛을 함께 담아보세요", HintType.NONE)
    }

    companion object {
        const val ID = "low_light"
    }
}
