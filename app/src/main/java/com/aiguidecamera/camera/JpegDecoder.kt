package com.aiguidecamera.camera

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix

/** 촬영 직후 디코딩하지 않은 JPEG와, 똑바로 세우려면 돌려야 할 각도. */
class CapturedJpeg(val bytes: ByteArray, val rotationDegrees: Int)

/** [CapturedJpeg]를 똑바로 선 Bitmap으로 디코딩한다. 채점용으로는 줄여서 디코딩할 수 있다. */
object JpegDecoder {

    /** [minLongSide]를 주면 긴 변이 그 이상인 범위에서 2의 거듭제곱으로 줄여 디코딩한다. */
    fun decodeUpright(jpeg: CapturedJpeg, minLongSide: Int = Int.MAX_VALUE): Bitmap {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(jpeg.bytes, 0, jpeg.bytes.size, bounds)
        val longSide = maxOf(bounds.outWidth, bounds.outHeight)
        var sampleSize = 1
        while (longSide / (sampleSize * 2) >= minLongSide) sampleSize *= 2

        val options = BitmapFactory.Options().apply { inSampleSize = sampleSize }
        val decoded = BitmapFactory.decodeByteArray(jpeg.bytes, 0, jpeg.bytes.size, options)
            ?: error("JPEG 디코딩 실패")
        return rotateUpright(decoded, jpeg.rotationDegrees)
    }

    /** 회전이 필요하면 새 Bitmap을 만들고 입력은 해제한다. */
    fun rotateUpright(bitmap: Bitmap, rotationDegrees: Int): Bitmap {
        if (rotationDegrees == 0) return bitmap
        val matrix = Matrix().apply { postRotate(rotationDegrees.toFloat()) }
        val rotated = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
        bitmap.recycle()
        return rotated
    }
}
