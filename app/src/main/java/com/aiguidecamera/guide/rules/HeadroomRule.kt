package com.aiguidecamera.guide.rules

import com.aiguidecamera.analysis.BodyPart
import com.aiguidecamera.analysis.FrameAnalysisResult
import com.aiguidecamera.guide.GuideConstants
import com.aiguidecamera.guide.ShotClassifier
import com.aiguidecamera.guide.ShotType
import com.aiguidecamera.guide.meanVisibleY
import com.aiguidecamera.guide.visible

/**
 * 전신에서 머리 위가 빠듯하지 않은지 본다. 정수리는 얼굴 박스, 없으면 코·어깨 위치로 추정한다.
 * 여백이 넓은 쪽은 보지 않는다: 전신은 발끝을 아래에 붙이고 위를 넉넉히 두는 게 정석이다 (FootMarginRule이 맡음).
 * 전신이 아닌 인물은 EyeLineRule이 맡는다.
 */
class HeadroomRule : Rule {
    override val id = ID
    override val priority = 3

    override fun check(result: FrameAnalysisResult): Issue? {
        if (ShotClassifier.classify(result.landmarks) != ShotType.FULL_BODY) return null
        val headroom = estimateHeadTopY(result) ?: return null
        if (headroom >= GuideConstants.HEADROOM_MIN) return null
        return Issue(id, priority, "머리 위가 빠듯해요. 폰을 살짝 위로 기울여 주세요", HintType.UP)
    }

    /** 화면 위쪽 끝(0)부터 정수리까지의 거리 = 머리 위 여백. 음수면 머리가 잘린 것. */
    private fun estimateHeadTopY(result: FrameAnalysisResult): Float? {
        result.primaryFace?.let { face ->
            return face.box.top - face.box.height * GuideConstants.HEAD_TOP_ABOVE_FACE_RATIO
        }
        val landmarks = result.landmarks
        val noseY = landmarks.visible(BodyPart.NOSE)?.y ?: return null
        val shoulderY = landmarks.meanVisibleY(BodyPart.LEFT_SHOULDER, BodyPart.RIGHT_SHOULDER) ?: return null
        return noseY - (shoulderY - noseY) * GuideConstants.HEAD_TOP_ABOVE_NOSE_RATIO
    }

    companion object {
        const val ID = "headroom"
    }
}
