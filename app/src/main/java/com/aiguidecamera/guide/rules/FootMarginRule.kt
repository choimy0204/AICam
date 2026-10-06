package com.aiguidecamera.guide.rules

import com.aiguidecamera.analysis.BodyPart
import com.aiguidecamera.analysis.FrameAnalysisResult
import com.aiguidecamera.guide.GuideConstants
import com.aiguidecamera.guide.ShotClassifier
import com.aiguidecamera.guide.ShotType
import com.aiguidecamera.guide.visible

/**
 * 전신에서 발 아래 여백이 너무 넓은지 본다. 발끝을 화면 아래에 붙여야 다리가 길어 보인다.
 * (발이 잘린 경우는 BodyCropRule이 맡는다)
 */
class FootMarginRule : Rule {
    override val id = "foot_margin"
    override val priority = 3

    override fun check(result: FrameAnalysisResult): Issue? {
        val landmarks = result.landmarks
        if (ShotClassifier.classify(landmarks) != ShotType.FULL_BODY) return null
        val lowestAnkleY = maxOf(
            landmarks.visible(BodyPart.LEFT_ANKLE)?.y ?: 0f,
            landmarks.visible(BodyPart.RIGHT_ANKLE)?.y ?: 0f,
        )
        if (1f - lowestAnkleY <= GuideConstants.FOOT_MARGIN_MAX) return null
        return Issue(id, priority, "발 아래 여백이 넓어요. 폰을 살짝 위로 기울여 주세요", HintType.UP)
    }
}
