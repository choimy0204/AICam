package com.aiguidecamera.guide.rules

import com.aiguidecamera.analysis.BodyPart
import com.aiguidecamera.analysis.FrameAnalysisResult
import com.aiguidecamera.guide.GuideConstants
import com.aiguidecamera.guide.ShotClassifier
import com.aiguidecamera.guide.ShotType
import com.aiguidecamera.guide.meanVisibleY
import com.aiguidecamera.guide.visible

/**
 * 인물 사진이 될 만한 사람이 있는지 본다: 사람이 아예 없거나 화면에 비해 너무 작으면 다른 구도 조언보다 먼저 알린다.
 * (예: 거울 속 작은 얼굴만 잡힌 장면을 "좋아요"로 판정하지 않게 한다)
 */
class SubjectPresenceRule : Rule {
    override val id = "subject_presence"
    override val priority = 0

    override fun check(result: FrameAnalysisResult): Issue? {
        val landmarks = result.landmarks
        val shotType = ShotClassifier.classify(landmarks)
        val face = result.primaryFace

        if (shotType == ShotType.NONE && face == null) {
            return Issue(id, priority, "사람이 화면에 들어오게 해 주세요", HintType.NONE)
        }
        val tooSmall = if (shotType == ShotType.FULL_BODY) {
            // 전신은 얼굴이 원래 작으므로 코→발목 길이로 본다.
            val ankleY = landmarks.meanVisibleY(BodyPart.LEFT_ANKLE, BodyPart.RIGHT_ANKLE)
            val noseY = landmarks.visible(BodyPart.NOSE)?.y
            ankleY != null && noseY != null && ankleY - noseY < GuideConstants.SUBJECT_BODY_SPAN_MIN
        } else {
            face != null && face.box.height < GuideConstants.SUBJECT_FACE_HEIGHT_MIN
        }
        return if (tooSmall) Issue(id, priority, "인물이 너무 작아요. 더 가까이 다가가 주세요", HintType.NONE) else null
    }
}
