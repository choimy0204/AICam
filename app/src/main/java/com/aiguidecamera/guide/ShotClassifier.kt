package com.aiguidecamera.guide

import com.aiguidecamera.analysis.BodyPart
import com.aiguidecamera.analysis.Landmark

/** 인물 샷 종류. 화면에 보이는 가장 아래 관절로 정한다. */
enum class ShotType { NONE, CLOSE_UP, UPPER_BODY, FULL_BODY }

/**
 * 보이는 관절로 샷 종류를 판별한다 (발목 보임 = 전신 / 엉덩이까지 = 상반신 / 어깨까지 = 클로즈업).
 */
object ShotClassifier {

    fun classify(landmarks: Map<BodyPart, Landmark>): ShotType = when {
        landmarks.anyVisible(BodyPart.LEFT_ANKLE, BodyPart.RIGHT_ANKLE) -> ShotType.FULL_BODY
        landmarks.anyVisible(BodyPart.LEFT_HIP, BodyPart.RIGHT_HIP) -> ShotType.UPPER_BODY
        landmarks.anyVisible(BodyPart.LEFT_SHOULDER, BodyPart.RIGHT_SHOULDER) -> ShotType.CLOSE_UP
        else -> ShotType.NONE
    }
}

/** 신뢰도 기준을 넘는 관절만 돌려준다. */
fun Map<BodyPart, Landmark>.visible(part: BodyPart): Landmark? =
    this[part]?.takeIf { it.confidence >= GuideConstants.LANDMARK_CONFIDENCE_MIN }

fun Map<BodyPart, Landmark>.anyVisible(left: BodyPart, right: BodyPart): Boolean =
    visible(left) != null || visible(right) != null

/** 좌우 한 쌍 중 보이는 관절들의 평균 y. 둘 다 안 보이면 null. */
fun Map<BodyPart, Landmark>.meanVisibleY(left: BodyPart, right: BodyPart): Float? {
    val l = visible(left)
    val r = visible(right)
    return when {
        l != null && r != null -> (l.y + r.y) / 2f
        l != null -> l.y
        r != null -> r.y
        else -> null
    }
}
