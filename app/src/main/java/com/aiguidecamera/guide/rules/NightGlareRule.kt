package com.aiguidecamera.guide.rules

import com.aiguidecamera.analysis.FrameAnalysisResult
import com.aiguidecamera.guide.GuideConstants

/** 야경: 하얗게 날아간 불빛이 화면을 많이 차지하면 밝은 조명을 정면에서 비키라고 한다. 작은 가로등 정도는 통과. */
class NightGlareRule : Rule {
    override val id = ID
    override val priority = 3

    override fun check(result: FrameAnalysisResult): Issue? {
        if (result.highlightClipRatio <= GuideConstants.NIGHT_HIGHLIGHT_CLIP_RATIO_MAX) return null
        return Issue(id, priority, "불빛이 하얗게 번져요. 밝은 조명을 화면 가장자리로 빼보세요", HintType.NONE)
    }

    companion object {
        const val ID = "night_glare"
    }
}
