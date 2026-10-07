package com.aiguidecamera.guide.rules

import com.aiguidecamera.analysis.FrameAnalysisResult
import com.aiguidecamera.guide.GuideConstants

/** 풍경: 하얗게 날아간(최대 밝기) 부분이 너무 많으면 하늘·해를 덜 담으라고 한다. */
class HighlightClipRule : Rule {
    override val id = ID
    override val priority = 3

    override fun check(result: FrameAnalysisResult): Issue? {
        if (result.highlightClipRatio <= GuideConstants.HIGHLIGHT_CLIP_RATIO_MAX) return null
        return Issue(id, priority, "하늘이 하얗게 날아가요. 해를 피하거나 하늘을 덜 담아보세요", HintType.NONE)
    }

    companion object {
        const val ID = "highlight_clip"
    }
}
