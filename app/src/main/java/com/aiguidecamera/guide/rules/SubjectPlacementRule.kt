package com.aiguidecamera.guide.rules

import com.aiguidecamera.analysis.BodyPart
import com.aiguidecamera.analysis.FrameAnalysisResult
import com.aiguidecamera.guide.GuideConstants
import com.aiguidecamera.guide.visible
import kotlin.math.abs

/**
 * 인물의 가로 위치가 가운데 또는 세로 3분할선 근처인지 본다. 그 사이 어중간한 위치나 화면 가장자리면 문제.
 * 방향은 말하지 않는다 (전면 카메라는 프리뷰가 거울상이라 좌우 안내가 헷갈린다).
 */
class SubjectPlacementRule : Rule {
    override val id = "subject_placement"
    override val priority = 6

    override fun check(result: FrameAnalysisResult): Issue? {
        val x = subjectX(result) ?: return null
        val tolerance = GuideConstants.SUBJECT_CENTER_TOLERANCE
        val aligned = abs(x - CENTER) <= tolerance ||
            abs(x - LEFT_THIRD) <= tolerance ||
            abs(x - RIGHT_THIRD) <= tolerance
        if (aligned) return null
        return Issue(id, priority, "인물을 화면 가운데나 3분할선에 맞춰 주세요", HintType.NONE)
    }

    /** 얼굴 중심, 없으면 코, 없으면 양 어깨 가운데. */
    private fun subjectX(result: FrameAnalysisResult): Float? {
        result.primaryFace?.let { return (it.box.left + it.box.right) / 2f }
        val landmarks = result.landmarks
        landmarks.visible(BodyPart.NOSE)?.let { return it.x }
        val left = landmarks.visible(BodyPart.LEFT_SHOULDER) ?: return null
        val right = landmarks.visible(BodyPart.RIGHT_SHOULDER) ?: return null
        return (left.x + right.x) / 2f
    }

    private companion object {
        const val CENTER = 0.5f
        const val LEFT_THIRD = 1f / 3f
        const val RIGHT_THIRD = 2f / 3f
    }
}
