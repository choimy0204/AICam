package com.aiguidecamera.guide.rules

import com.aiguidecamera.analysis.BodyPart
import com.aiguidecamera.analysis.FrameAnalysisResult
import com.aiguidecamera.guide.GuideConstants
import com.aiguidecamera.guide.ShotClassifier
import com.aiguidecamera.guide.ShotType
import com.aiguidecamera.guide.meanVisibleY
import com.aiguidecamera.guide.visible

/**
 * 폰이 너무 높아 사람을 내려다보는지 판단한다. 피치가 기준보다 크거나, 전신에서 상체에 비해 다리가 짧아 보이면 문제.
 */
class CameraHeightRule : Rule {
    override val id = ID
    override val priority = 1

    override fun check(result: FrameAnalysisResult): Issue? {
        val shot = ShotClassifier.classify(result.landmarks)
        if (shot == ShotType.NONE) return null
        val tooHigh = result.pitchDeg > GuideConstants.CAMERA_DOWNWARD_PITCH_MAX_DEG ||
            (shot == ShotType.FULL_BODY && legsLookShort(result))
        if (!tooHigh) return null
        return Issue(id, priority, "폰을 허리 높이로 내려주세요", HintType.DOWN)
    }

    private fun legsLookShort(result: FrameAnalysisResult): Boolean {
        val landmarks = result.landmarks
        val noseY = landmarks.visible(BodyPart.NOSE)?.y ?: return false
        val hipY = landmarks.meanVisibleY(BodyPart.LEFT_HIP, BodyPart.RIGHT_HIP) ?: return false
        val ankleY = landmarks.meanVisibleY(BodyPart.LEFT_ANKLE, BodyPart.RIGHT_ANKLE) ?: return false
        val legLength = ankleY - hipY
        if (legLength <= 0f) return false
        return (hipY - noseY) / legLength > GuideConstants.LOOKDOWN_TORSO_LEG_RATIO_MAX
    }

    companion object {
        const val ID = "camera_height"
    }
}
