package com.aiguidecamera.capture

import com.aiguidecamera.guide.GuideConstants

/** 연사 한 장의 채점 재료. eyesOpen은 사진 속 얼굴들 중 가장 덜 뜬 눈의 확률, 얼굴이 없거나 음식 모드면 null. */
data class ShotScore(val sharpness: Float, val eyesOpen: Float?)

/**
 * 연사 사진 중 1장을 고른다: 선명도(연사 중 최댓값 대비 비율) + 눈 뜸 점수의 가중합이 가장 큰 사진.
 * 눈 정보가 있는 사진이 하나도 없으면 선명도만 본다.
 */
object BestShotSelector {

    fun bestIndex(scores: List<ShotScore>): Int {
        require(scores.isNotEmpty()) { "채점할 사진이 없습니다" }
        val maxSharpness = scores.maxOf { it.sharpness }
        val useEyes = scores.any { it.eyesOpen != null }

        var bestIndex = 0
        var bestTotal = Float.NEGATIVE_INFINITY
        scores.forEachIndexed { index, score ->
            val sharpness = if (maxSharpness > 0f) score.sharpness / maxSharpness else 0f
            val total = if (useEyes) {
                // 이 사진에서만 얼굴을 못 찾았다면 눈을 감았거나 고개를 돌린 것으로 보고 0점.
                GuideConstants.BEST_SHOT_SHARPNESS_WEIGHT * sharpness +
                    GuideConstants.BEST_SHOT_EYE_WEIGHT * (score.eyesOpen ?: 0f)
            } else {
                sharpness
            }
            if (total > bestTotal) {
                bestTotal = total
                bestIndex = index
            }
        }
        return bestIndex
    }
}
