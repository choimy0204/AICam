package com.aiguidecamera.render

import android.content.res.AssetManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.opengl.GLES30
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * 512x512 LUT PNG(8x8 타일, 64³)를 읽어 GL_TEXTURE_3D로 올린다.
 * PNG 디코딩 결과(RGB 바이트)는 프로세스 안에서 캐시해 프리뷰·오프스크린 두 GL 컨텍스트가 같이 쓴다.
 * 텍스처 핸들 자체는 GL 컨텍스트마다 따로라서 [FilterShader]가 컨텍스트별로 들고 있다.
 */
class LutLoader(private val assets: AssetManager) {

    private val decodedCache = HashMap<String, ByteBuffer>()

    /** 현재 스레드의 GL 컨텍스트에 3D 텍스처를 만들어 핸들을 돌려준다. */
    fun createTexture(assetPath: String): Int {
        val data = decodedLut(assetPath)
        val ids = IntArray(1)
        GLES30.glGenTextures(1, ids, 0)
        GLES30.glBindTexture(GLES30.GL_TEXTURE_3D, ids[0])
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_3D, GLES30.GL_TEXTURE_MIN_FILTER, GLES30.GL_LINEAR)
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_3D, GLES30.GL_TEXTURE_MAG_FILTER, GLES30.GL_LINEAR)
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_3D, GLES30.GL_TEXTURE_WRAP_S, GLES30.GL_CLAMP_TO_EDGE)
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_3D, GLES30.GL_TEXTURE_WRAP_T, GLES30.GL_CLAMP_TO_EDGE)
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_3D, GLES30.GL_TEXTURE_WRAP_R, GLES30.GL_CLAMP_TO_EDGE)
        GLES30.glPixelStorei(GLES30.GL_UNPACK_ALIGNMENT, 1)
        GLES30.glTexImage3D(
            GLES30.GL_TEXTURE_3D, 0, GLES30.GL_RGB8, LUT_SIZE, LUT_SIZE, LUT_SIZE, 0,
            GLES30.GL_RGB, GLES30.GL_UNSIGNED_BYTE, data,
        )
        return ids[0]
    }

    /** 두 GL 스레드가 동시에 읽어도 position이 꼬이지 않도록 독립 position을 가진 복제 뷰를 준다. */
    private fun decodedLut(assetPath: String): ByteBuffer = synchronized(decodedCache) {
        decodedCache.getOrPut(assetPath) { decode(assetPath) }.duplicate()
    }

    /** 타일 배치를 3D 배열 순서(r가 가장 빠르게, 그다음 g, 그다음 b)로 바꾼다. */
    private fun decode(assetPath: String): ByteBuffer {
        val options = BitmapFactory.Options().apply {
            inPreferredConfig = Bitmap.Config.ARGB_8888
            inPremultiplied = false
            inScaled = false
        }
        val bitmap = assets.open(assetPath).use { BitmapFactory.decodeStream(it, null, options) }
        checkNotNull(bitmap) { "LUT를 읽지 못했습니다: $assetPath" }
        check(bitmap.width == IMAGE_SIZE && bitmap.height == IMAGE_SIZE) { "LUT 크기가 512x512가 아닙니다: $assetPath" }

        val pixels = IntArray(IMAGE_SIZE * IMAGE_SIZE)
        bitmap.getPixels(pixels, 0, IMAGE_SIZE, 0, 0, IMAGE_SIZE, IMAGE_SIZE)
        bitmap.recycle()

        val buffer = ByteBuffer.allocateDirect(LUT_SIZE * LUT_SIZE * LUT_SIZE * RGB_BYTES).order(ByteOrder.nativeOrder())
        for (b in 0 until LUT_SIZE) {
            val tileX = (b % TILES_PER_ROW) * LUT_SIZE
            val tileY = (b / TILES_PER_ROW) * LUT_SIZE
            for (g in 0 until LUT_SIZE) {
                val rowStart = (tileY + g) * IMAGE_SIZE + tileX
                for (r in 0 until LUT_SIZE) {
                    val color = pixels[rowStart + r]
                    buffer.put((color shr 16 and 0xFF).toByte())
                    buffer.put((color shr 8 and 0xFF).toByte())
                    buffer.put((color and 0xFF).toByte())
                }
            }
        }
        buffer.position(0)
        return buffer
    }

    private companion object {
        const val LUT_SIZE = 64
        const val TILES_PER_ROW = 8
        const val IMAGE_SIZE = LUT_SIZE * TILES_PER_ROW
        const val RGB_BYTES = 3
    }
}
