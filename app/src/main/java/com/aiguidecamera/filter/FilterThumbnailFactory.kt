package com.aiguidecamera.filter

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Shader
import com.aiguidecamera.render.FilterParams
import com.aiguidecamera.render.OffscreenFilterRenderer

/**
 * 필터 피커에 보여줄 썸네일을 만든다. 하늘·피부·음식·초록 색 띠에 밝기 그라데이션을 얹은 견본 이미지를
 * 실제 저장과 같은 [OffscreenFilterRenderer]로 렌더링하므로 썸네일 색감이 결과물과 같다.
 */
class FilterThumbnailFactory(private val renderer: OffscreenFilterRenderer) {

    suspend fun createAll(): Map<String, Bitmap> {
        val swatch = createSwatch()
        try {
            return FilterCatalog.all.associate { preset ->
                val params = FilterParams(preset, intensity = 1f, skinSmoothLevel = 0f)
                preset.id to renderer.render(swatch, params, faceBoxes = emptyList())
            }
        } finally {
            swatch.recycle()
        }
    }

    private fun createSwatch(): Bitmap {
        val bitmap = Bitmap.createBitmap(SIZE, SIZE, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val paint = Paint()
        val bandHeight = SIZE.toFloat() / SWATCH_COLORS.size
        SWATCH_COLORS.forEachIndexed { index, color ->
            paint.color = color
            canvas.drawRect(0f, index * bandHeight, SIZE.toFloat(), (index + 1) * bandHeight, paint)
        }
        // 왼쪽은 어둡게, 오른쪽은 밝게 → 섀도·하이라이트 커브 차이도 보이게 한다.
        paint.shader = LinearGradient(
            0f, 0f, SIZE.toFloat(), 0f,
            intArrayOf(0x99000000.toInt(), 0x00000000, 0x66FFFFFF),
            null, Shader.TileMode.CLAMP,
        )
        canvas.drawRect(0f, 0f, SIZE.toFloat(), SIZE.toFloat(), paint)
        return bitmap
    }

    private companion object {
        const val SIZE = 96
        val SWATCH_COLORS = intArrayOf(
            0xFF8EC5E8.toInt(), // 하늘
            0xFFE8B89A.toInt(), // 피부
            0xFFD9663B.toInt(), // 음식(구운 색)
            0xFF6FA35A.toInt(), // 초록
        )
    }
}
