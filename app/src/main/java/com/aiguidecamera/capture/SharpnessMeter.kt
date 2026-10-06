package com.aiguidecamera.capture

import android.graphics.Bitmap

/**
 * 사진 선명도를 라플라시안 분산으로 잰다. 값이 클수록 경계가 또렷하다(초점 빗나감·손떨림 비교용).
 * 같은 크기로 디코딩한 연사 사진끼리 비교할 때만 의미가 있다.
 */
object SharpnessMeter {

    fun measure(bitmap: Bitmap): Float {
        val width = bitmap.width
        val height = bitmap.height
        val pixels = IntArray(width * height)
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)
        for (i in pixels.indices) {
            val color = pixels[i]
            val red = (color shr 16) and 0xFF
            val green = (color shr 8) and 0xFF
            val blue = color and 0xFF
            pixels[i] = (299 * red + 587 * green + 114 * blue) / 1000
        }
        return laplacianVariance(pixels, width, height)
    }

    /** luma(0~255) 격자에 4-이웃 라플라시안을 적용한 응답의 분산. 가장자리 1픽셀은 제외한다. */
    fun laplacianVariance(luma: IntArray, width: Int, height: Int): Float {
        if (width < 3 || height < 3) return 0f
        var sum = 0.0
        var sumSquares = 0.0
        var count = 0
        for (y in 1 until height - 1) {
            val row = y * width
            for (x in 1 until width - 1) {
                val i = row + x
                val laplacian = 4 * luma[i] - luma[i - 1] - luma[i + 1] - luma[i - width] - luma[i + width]
                sum += laplacian
                sumSquares += laplacian.toDouble() * laplacian
                count++
            }
        }
        val mean = sum / count
        return (sumSquares / count - mean * mean).toFloat()
    }
}
