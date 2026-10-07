package com.aiguidecamera.analysis

import androidx.camera.core.ImageProxy
import com.aiguidecamera.guide.GuideConstants

/**
 * YUV_420_888 프레임을 격자로 샘플링해 전체 평균 밝기·평균 RGB·얼굴 영역 평균 밝기,
 * 하얗게 날아간 비율, 위→아래 띠별 밝기(하늘/땅 경계 찾기용)를 구한다.
 * 결과는 이 객체의 필드에 담기며, 프레임마다 객체를 새로 만들지 않도록 재사용한다(분석 스레드 전용).
 */
class LightAnalyzer {

    var frameBrightness = 0f
        private set
    var meanRed = 0f
        private set
    var meanGreen = 0f
        private set
    var meanBlue = 0f
        private set
    /** 얼굴 박스 안 샘플이 하나도 없으면 null. */
    var faceBrightness: Float? = null
        private set
    var highlightClipRatio = 0f
        private set
    /** 하늘/땅 경계. 폰을 잡은 방향 기준 0 = 위. 없으면 null. */
    var skyLineY: Float? = null
        private set

    private val bandSums = LongArray(GuideConstants.SKY_LINE_BAND_COUNT)
    private val bandCounts = IntArray(GuideConstants.SKY_LINE_BAND_COUNT)
    private val bandMeans = FloatArray(GuideConstants.SKY_LINE_BAND_COUNT)

    /**
     * [faceBox]는 똑바로 선 이미지 기준 0~1 좌표. 버퍼 좌표를 회전 방향에 맞춰 똑바로 선 좌표로 바꿔 비교한다.
     * [heldQuarterTurns]는 폰을 세로에서 반시계로 몇 번 90도 돌려 잡았는지(0~3). 띠를 "잡은 방향의 위→아래"로 나눈다.
     */
    fun analyze(image: ImageProxy, rotationDegrees: Int, faceBox: NormRect?, heldQuarterTurns: Int) {
        val yPlane = image.planes[0]
        val uPlane = image.planes[1]
        val vPlane = image.planes[2]
        val yBuffer = yPlane.buffer
        val uBuffer = uPlane.buffer
        val vBuffer = vPlane.buffer
        val yRowStride = yPlane.rowStride
        val yPixelStride = yPlane.pixelStride
        val uvRowStride = uPlane.rowStride
        val uvPixelStride = uPlane.pixelStride
        val width = image.width
        val height = image.height
        val step = GuideConstants.LIGHT_SAMPLE_STEP_PX

        var sumLuma = 0L
        var sumRed = 0L
        var sumGreen = 0L
        var sumBlue = 0L
        var count = 0
        var faceSum = 0L
        var faceCount = 0
        var clipCount = 0
        val bandCount = GuideConstants.SKY_LINE_BAND_COUNT
        bandSums.fill(0L)
        bandCounts.fill(0)

        var y = 0
        while (y < height) {
            val v = y.toFloat() / height
            var x = 0
            while (x < width) {
                val luma = yBuffer.get(y * yRowStride + x * yPixelStride).toInt() and 0xFF
                val uvIndex = (y / 2) * uvRowStride + (x / 2) * uvPixelStride
                val cb = (uBuffer.get(uvIndex).toInt() and 0xFF) - CHROMA_OFFSET
                val cr = (vBuffer.get(uvIndex).toInt() and 0xFF) - CHROMA_OFFSET

                sumLuma += luma
                if (luma >= GuideConstants.HIGHLIGHT_CLIP_LUMA) clipCount++
                sumRed += clampByte(luma + (1.402f * cr).toInt())
                sumGreen += clampByte(luma - (0.344f * cb + 0.714f * cr).toInt())
                sumBlue += clampByte(luma + (1.772f * cb).toInt())
                count++

                val u = x.toFloat() / width
                val uprightX = uprightX(u, v, rotationDegrees)
                val uprightY = uprightY(u, v, rotationDegrees)
                if (faceBox != null && faceBox.contains(uprightX, uprightY)) {
                    faceSum += luma
                    faceCount++
                }
                val band = (heldDown(uprightX, uprightY, heldQuarterTurns) * bandCount).toInt().coerceIn(0, bandCount - 1)
                bandSums[band] += luma.toLong()
                bandCounts[band]++
                x += step
            }
            y += step
        }

        val divisor = count * MAX_BYTE
        frameBrightness = sumLuma.toFloat() / divisor
        meanRed = sumRed.toFloat() / divisor
        meanGreen = sumGreen.toFloat() / divisor
        meanBlue = sumBlue.toFloat() / divisor
        faceBrightness = if (faceCount > 0) faceSum.toFloat() / (faceCount * MAX_BYTE) else null
        highlightClipRatio = clipCount.toFloat() / count
        for (i in 0 until bandCount) {
            bandMeans[i] = if (bandCounts[i] > 0) bandSums[i].toFloat() / (bandCounts[i] * MAX_BYTE) else 0f
        }
        skyLineY = SkyLineFinder.find(bandMeans)
    }

    private fun clampByte(value: Int): Int = value.coerceIn(0, 255)

    private companion object {
        const val CHROMA_OFFSET = 128
        const val MAX_BYTE = 255f

        /** 버퍼 좌표(u, v: 0~1)를 시계 방향 [rotation]만큼 돌린 똑바로 선 좌표로 바꾼다. */
        fun uprightX(u: Float, v: Float, rotation: Int): Float = when (rotation) {
            90 -> 1f - v
            180 -> 1f - u
            270 -> v
            else -> u
        }

        /**
         * 똑바로 선(세로) 좌표를 "폰을 잡은 방향의 위→아래" 축(0~1)으로 바꾼다.
         * 반시계로 한 번 돌려 잡으면 화면 오른쪽이 위가 되므로 아래쪽 = 1 - x.
         */
        fun heldDown(x: Float, y: Float, quarterTurns: Int): Float = when (quarterTurns) {
            1 -> 1f - x
            2 -> 1f - y
            3 -> x
            else -> y
        }

        fun uprightY(u: Float, v: Float, rotation: Int): Float = when (rotation) {
            90 -> u
            180 -> 1f - v
            270 -> 1f - u
            else -> v
        }
    }
}
