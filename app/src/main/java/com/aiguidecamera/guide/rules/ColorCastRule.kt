package com.aiguidecamera.guide.rules

import com.aiguidecamera.analysis.FrameAnalysisResult
import com.aiguidecamera.guide.GuideConstants

/** 그레이월드 가정: 프레임 평균 R/B 비율이 범위를 벗어나면 조명이 누렇거나(높음) 푸르다(낮음). */
class ColorCastRule : Rule {
    override val id = ID
    override val priority = 2

    override fun check(result: FrameAnalysisResult): Issue? {
        if (result.meanBlue < GuideConstants.COLOR_CAST_CHANNEL_MIN) return null
        val ratio = result.meanRed / result.meanBlue
        if (ratio in GuideConstants.COLOR_CAST_RB_RATIO_MIN..GuideConstants.COLOR_CAST_RB_RATIO_MAX) return null
        return Issue(id, priority, "조명 색이 강해요. 자연광 쪽으로 옮겨보세요", HintType.NONE)
    }

    companion object {
        const val ID = "color_cast"
    }
}
