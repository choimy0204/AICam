package com.aiguidecamera.storage

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 저장된 사진(content Uri)을 비트맵으로 읽는다. 사후 편집에서 원본을 불러올 때 쓴다.
 * 이 앱이 저장한 JPEG는 이미 똑바로 선 상태로 인코딩되어 있어 EXIF 회전은 보지 않는다.
 */
class PhotoLoader(context: Context) {

    private val resolver = context.applicationContext.contentResolver

    /**
     * [maxSide]가 있으면 긴 변이 그 이상이 되도록 2의 거듭제곱으로 줄여 읽는다 (편집 미리보기용).
     * null이면 원본 해상도 그대로 읽는다 (저장용).
     */
    suspend fun load(uri: Uri, maxSide: Int? = null): Bitmap = withContext(Dispatchers.IO) {
        val options = BitmapFactory.Options().apply {
            inPreferredConfig = Bitmap.Config.ARGB_8888
            inSampleSize = if (maxSide == null) 1 else sampleSizeFor(uri, maxSide)
        }
        val bitmap = resolver.openInputStream(uri).use { stream ->
            checkNotNull(stream) { "사진을 열지 못했습니다" }
            BitmapFactory.decodeStream(stream, null, options)
        }
        checkNotNull(bitmap) { "사진을 읽지 못했습니다" }
    }

    private fun sampleSizeFor(uri: Uri, maxSide: Int): Int {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        resolver.openInputStream(uri).use { BitmapFactory.decodeStream(it, null, bounds) }
        val longSide = maxOf(bounds.outWidth, bounds.outHeight)
        var sampleSize = 1
        while (longSide / (sampleSize * 2) >= maxSide) sampleSize *= 2
        return sampleSize
    }
}
