package com.aiguidecamera.guide.rules

import com.aiguidecamera.analysis.FrameAnalysisResult
import com.aiguidecamera.guide.FoodAngle
import com.aiguidecamera.guide.GuideConstants

/** 사용자가 고른 목표 각도(탑뷰 / 45°)와 현재 피치를 비교해 더 눕히거나 세우라고 안내한다. */
class FoodAngleRule(private val target: FoodAngle) : Rule {
    override val id = ID
    override val priority = 1

    private val minPitch = when (target) {
        FoodAngle.TOP_VIEW -> GuideConstants.FOOD_TOPVIEW_PITCH_MIN
        FoodAngle.ANGLED -> GuideConstants.FOOD_ANGLED_PITCH_MIN
    }
    private val maxPitch = when (target) {
        FoodAngle.TOP_VIEW -> GuideConstants.FOOD_TOPVIEW_PITCH_MAX
        FoodAngle.ANGLED -> GuideConstants.FOOD_ANGLED_PITCH_MAX
    }

    override fun check(result: FrameAnalysisResult): Issue? = when {
        result.pitchDeg < minPitch -> Issue(id, priority, lowerMessage(), HintType.DOWN)
        result.pitchDeg > maxPitch -> Issue(id, priority, "조금 세워서 45°에 맞춰주세요", HintType.UP)
        else -> null
    }

    private fun lowerMessage(): String = when (target) {
        FoodAngle.TOP_VIEW -> "폰을 더 눕혀서 바로 위에서 내려다봐 주세요"
        FoodAngle.ANGLED -> "조금 더 내려다보도록 기울여 45°에 맞춰주세요"
    }

    companion object {
        const val ID = "food_angle"
    }
}
