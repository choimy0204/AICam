package com.aiguidecamera.guide.rules

import com.aiguidecamera.analysis.BodyPart
import com.aiguidecamera.analysis.FrameAnalysisResult
import com.aiguidecamera.guide.GuideConstants
import com.aiguidecamera.guide.ShotClassifier
import com.aiguidecamera.guide.ShotType
import com.aiguidecamera.guide.meanVisibleY

/**
 * 전신이 아닌 인물(상반신·클로즈업·얼굴)에서 눈이 위쪽 3분할선 근처에 있는지 본다.
 * 인물 사진의 기본: 눈을 위에서 1/3 지점에 두면 머리 위 여백도 자연히 맞는다.
 */
class EyeLineRule : Rule {
    override val id = "eye_line"
    override val priority = 3

    override fun check(result: FrameAnalysisResult): Issue? {
        if (ShotClassifier.classify(result.landmarks) == ShotType.FULL_BODY) return null
        val eyeY = estimateEyeY(result) ?: return null
        val offset = eyeY - GuideConstants.EYE_LINE_TARGET
        return when {
            offset < -GuideConstants.EYE_LINE_TOLERANCE ->
                Issue(id, priority, "얼굴이 너무 위에 있어요. 폰을 살짝 위로 기울여 주세요", HintType.UP)
            offset > GuideConstants.EYE_LINE_TOLERANCE ->
                Issue(id, priority, "얼굴이 너무 아래에 있어요. 폰을 살짝 아래로 기울여 주세요", HintType.DOWN)
            else -> null
        }
    }

    /** 눈 관절이 보이면 그 높이, 아니면 얼굴 박스에서 추정한다. */
    private fun estimateEyeY(result: FrameAnalysisResult): Float? {
        result.landmarks.meanVisibleY(BodyPart.LEFT_EYE, BodyPart.RIGHT_EYE)?.let { return it }
        val face = result.primaryFace ?: return null
        return face.box.top + face.box.height * GuideConstants.EYES_BELOW_FACE_TOP_RATIO
    }
}
