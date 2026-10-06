package com.aiguidecamera.guide.rules

import com.aiguidecamera.analysis.FrameAnalysisResult
import com.aiguidecamera.guide.GuideConstants
import kotlin.math.abs

/**
 * 폰이 좌우로 기울었는지(롤) 본다. 인물·음식 공용이라 우선순위는 모드별로 받는다.
 * 바닥을 내려다볼 때(탑뷰)는 롤로 수평을 판단할 수 없어 건너뛴다.
 */
class HorizonRule(override val priority: Int) : Rule {
    override val id = ID

    override fun check(result: FrameAnalysisResult): Issue? {
        if (abs(result.pitchDeg) > GuideConstants.HORIZON_MAX_PITCH_DEG) return null
        if (abs(result.rollDeg) <= GuideConstants.HORIZON_TOLERANCE_DEG) return null
        // 롤이 음수 = 폰이 시계 방향으로 돌아감 → 반시계로 돌려야 한다.
        val hint = if (result.rollDeg < 0f) HintType.ROTATE_LEFT else HintType.ROTATE_RIGHT
        return Issue(id, priority, "폰이 살짝 기울었어요", hint)
    }

    companion object {
        const val ID = "horizon"
    }
}
