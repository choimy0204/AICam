package com.aiguidecamera.analysis

import com.aiguidecamera.guide.GuideConstants

/**
 * 위→아래로 나눈 띠별 평균 밝기에서 "위는 밝고 아래는 어두운" 경계(하늘과 땅이 만나는 선)를 찾는다.
 * 두 부분으로 나눴을 때 분산이 가장 크게 갈리는 위치(1차원 Otsu)를 고르고, 위가 충분히 더 밝을 때만 인정한다.
 */
object SkyLineFinder {

    /** 경계 위치(0 = 위, 1 = 아래)를 돌려준다. 뚜렷한 경계가 없으면 null. */
    fun find(bandMeans: FloatArray): Float? {
        val bandCount = bandMeans.size
        var total = 0f
        for (mean in bandMeans) total += mean

        val margin = GuideConstants.SKY_LINE_EDGE_BANDS
        var aboveSum = 0f
        for (i in 0 until margin) aboveSum += bandMeans[i]

        var bestScore = 0f
        var bestBoundary = -1
        var bestContrast = 0f
        for (boundary in margin..bandCount - margin) {
            val aboveMean = aboveSum / boundary
            val belowMean = (total - aboveSum) / (bandCount - boundary)
            val contrast = aboveMean - belowMean
            if (contrast > 0f) {
                val score = boundary.toFloat() * (bandCount - boundary) * contrast * contrast
                if (score > bestScore) {
                    bestScore = score
                    bestBoundary = boundary
                    bestContrast = contrast
                }
            }
            aboveSum += bandMeans[boundary]
        }
        if (bestBoundary < 0 || bestContrast < GuideConstants.SKY_LINE_CONTRAST_MIN) return null
        return bestBoundary.toFloat() / bandCount
    }
}
