package com.aiguidecamera.analysis

import android.graphics.RectF
import androidx.camera.core.ImageProxy
import com.aiguidecamera.guide.GuideConstants

/**
 * YUV_420_888 프레임을 격자로 샘플링해 전체 평균 밝기·평균 RGB·얼굴 영역 평균 밝기를 구한다.
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

    /**
     * [faceBox]는 똑바로 선 이미지 기준 0~1 좌표. 버퍼 좌표를 회전 방향에 맞춰 똑바로 선 좌표로 바꿔 비교한다.
     */
    fun analyze(image: ImageProxy, rotationDegrees: Int, faceBox: RectF?) {
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
                sumRed += clampByte(luma + (1.402f * cr).toInt())
                sumGreen += clampByte(luma - (0.344f * cb + 0.714f * cr).toInt())
                sumBlue += clampByte(luma + (1.772f * cb).toInt())
                count++

                if (faceBox != null) {
                    val u = x.toFloat() / width
                    if (faceBox.contains(uprightX(u, v, rotationDegrees), uprightY(u, v, rotationDegrees))) {
                        faceSum += luma
                        faceCount++
                    }
                }
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

        fun uprightY(u: Float, v: Float, rotation: Int): Float = when (rotation) {
            90 -> u
            180 -> 1f - v
            270 -> 1f - u
            else -> v
        }
    }
}
