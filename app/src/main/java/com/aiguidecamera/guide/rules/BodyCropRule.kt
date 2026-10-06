package com.aiguidecamera.guide.rules

import com.aiguidecamera.analysis.BodyPart
import com.aiguidecamera.analysis.FrameAnalysisResult
import com.aiguidecamera.analysis.Landmark
import com.aiguidecamera.guide.GuideConstants
import com.aiguidecamera.guide.anyVisible
import com.aiguidecamera.guide.visible

/**
 * 몸이 어색하게 잘렸는지 본다: 무릎은 보이는데 발목이 없거나 화면 하단에 걸린 경우, 또는 관절이 화면 끝에 걸린 경우.
 */
class BodyCropRule : Rule {
    override val id = ID
    override val priority = 2

    override fun check(result: FrameAnalysisResult): Issue? {
        val landmarks = result.landmarks
        if (feetCut(landmarks)) {
            return Issue(id, priority, "발끝이 잘렸어요. 조금 뒤로 물러나 주세요", HintType.BACK)
        }
        if (EDGE_JOINTS.any { part -> landmarks.visible(part)?.let(::isAtEdge) == true }) {
            return Issue(id, priority, "관절에서 잘렸어요. 조금 뒤로 물러나 주세요", HintType.BACK)
        }
        return null
    }

    private fun feetCut(landmarks: Map<BodyPart, Landmark>): Boolean {
        if (!landmarks.anyVisible(BodyPart.LEFT_KNEE, BodyPart.RIGHT_KNEE)) return false
        val left = landmarks.visible(BodyPart.LEFT_ANKLE)
        val right = landmarks.visible(BodyPart.RIGHT_ANKLE)
        if (left == null && right == null) return true
        val lowestAnkleY = maxOf(left?.y ?: 0f, right?.y ?: 0f)
        return lowestAnkleY > 1f - GuideConstants.FOOT_MARGIN_MIN
    }

    private fun isAtEdge(landmark: Landmark): Boolean {
        val tolerance = GuideConstants.JOINT_EDGE_TOLERANCE
        return landmark.x < tolerance || landmark.x > 1f - tolerance || landmark.y > 1f - tolerance
    }

    companion object {
        const val ID = "body_crop"

        /** 화면 끝에 걸리면 어색한 관절. */
        private val EDGE_JOINTS = listOf(
            BodyPart.LEFT_KNEE, BodyPart.RIGHT_KNEE,
            BodyPart.LEFT_ANKLE, BodyPart.RIGHT_ANKLE,
            BodyPart.LEFT_ELBOW, BodyPart.RIGHT_ELBOW,
        )
    }
}
