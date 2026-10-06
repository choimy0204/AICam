package com.aiguidecamera.guide.rules

import com.aiguidecamera.analysis.FrameAnalysisResult
import com.aiguidecamera.guide.GuideConstants

/** 얼굴이 배경보다 많이 어두우면(얼굴 밝기 / 전체 밝기 < 기준) 역광으로 본다. */
class BacklightRule : Rule {
    override val id = ID
    override val priority = 4

    override fun check(result: FrameAnalysisResult): Issue? {
        val faceBrightness = result.faceBrightness ?: return null
        if (result.frameBrightness < GuideConstants.BACKLIGHT_FRAME_BRIGHTNESS_MIN) return null
        if (faceBrightness / result.frameBrightness >= GuideConstants.BACKLIGHT_RATIO_MIN) return null
        return Issue(id, priority, "역광이에요. 빛을 등지지 않게 위치를 바꿔주세요", HintType.NONE)
    }

    companion object {
        const val ID = "backlight"
    }
}
