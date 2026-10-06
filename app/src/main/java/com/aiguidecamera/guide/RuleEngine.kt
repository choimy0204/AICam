package com.aiguidecamera.guide

import com.aiguidecamera.analysis.FrameAnalysisResult
import com.aiguidecamera.guide.rules.BacklightRule
import com.aiguidecamera.guide.rules.BodyCropRule
import com.aiguidecamera.guide.rules.CameraHeightRule
import com.aiguidecamera.guide.rules.ColorCastRule
import com.aiguidecamera.guide.rules.EyeLineRule
import com.aiguidecamera.guide.rules.FootMarginRule
import com.aiguidecamera.guide.rules.FoodAngleRule
import com.aiguidecamera.guide.rules.HeadroomRule
import com.aiguidecamera.guide.rules.HorizonRule
import com.aiguidecamera.guide.rules.Issue
import com.aiguidecamera.guide.rules.Rule
import com.aiguidecamera.guide.rules.SubjectPlacementRule
import com.aiguidecamera.guide.rules.SubjectPresenceRule

/**
 * 모드(와 음식 목표 각도)에 맞는 규칙 묶음을 돌려 이번 프레임의 문제 목록을 만든다.
 * 분석 스레드 전용이며, 돌려준 목록은 다음 [evaluate] 호출 전까지만 유효하다(할당 줄이려고 재사용).
 */
class RuleEngine {

    private val portraitRules: List<Rule> = listOf(
        SubjectPresenceRule(),
        CameraHeightRule(),
        BodyCropRule(),
        HeadroomRule(),
        FootMarginRule(),
        EyeLineRule(),
        BacklightRule(),
        HorizonRule(priority = 5),
        SubjectPlacementRule(),
    )

    private val foodRules: Map<FoodAngle, List<Rule>> = FoodAngle.entries.associateWith { angle ->
        listOf(FoodAngleRule(angle), ColorCastRule(), HorizonRule(priority = 3))
    }

    private val issues = ArrayList<Issue>()

    fun evaluate(result: FrameAnalysisResult, mode: ShootingMode, foodAngle: FoodAngle): List<Issue> {
        val rules = when (mode) {
            ShootingMode.PORTRAIT -> portraitRules
            ShootingMode.FOOD -> foodRules.getValue(foodAngle)
        }
        issues.clear()
        for (rule in rules) {
            rule.check(result)?.let(issues::add)
        }
        return issues
    }
}
