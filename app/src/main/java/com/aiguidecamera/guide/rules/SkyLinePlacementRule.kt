package com.aiguidecamera.guide.rules

import com.aiguidecamera.analysis.FrameAnalysisResult
import com.aiguidecamera.guide.GuideConstants
import kotlin.math.abs

/**
 * 풍경: 하늘과 땅의 경계(수평선)가 화면 가운데에 걸치면 위·아래 3분할선 중 가까운 쪽으로 옮기라고 한다.
 * 경계를 못 찾은 장면(숲·도시 등)은 판단하지 않는다.
 */
class SkyLinePlacementRule : Rule {
    override val id = ID
    override val priority = 2

    override fun check(result: FrameAnalysisResult): Issue? {
        val skyLine = result.skyLineY ?: return null
        if (abs(skyLine - CENTER) > GuideConstants.SKY_LINE_CENTER_TOLERANCE) return null
        // 수평선이 가운데보다 위면 위쪽 1/3로(폰을 숙임), 아래면 아래쪽 1/3로(폰을 듦) 보내는 게 가깝다.
        return if (skyLine < CENTER) {
            Issue(id, priority, "수평선이 가운데예요. 폰을 살짝 숙여 위쪽 1/3에 맞춰보세요", HintType.DOWN)
        } else {
            Issue(id, priority, "수평선이 가운데예요. 폰을 살짝 들어 아래쪽 1/3에 맞춰보세요", HintType.UP)
        }
    }

    companion object {
        const val ID = "sky_line_placement"
        private const val CENTER = 0.5f
    }
}
