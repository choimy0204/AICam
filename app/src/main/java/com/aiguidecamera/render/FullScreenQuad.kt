package com.aiguidecamera.render

import android.opengl.GLES30
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer

/**
 * 화면(또는 FBO) 전체를 덮는 사각형 하나. TRIANGLE_STRIP 4정점으로 그린다.
 * 위치는 attribute location 0, 텍스처 좌표는 location 1에 바인딩한다.
 */
class FullScreenQuad {

    private val positions: FloatBuffer = floatBufferOf(
        -1f, -1f,
        1f, -1f,
        -1f, 1f,
        1f, 1f,
    )

    private val texCoords: FloatBuffer = floatBufferOf(
        0f, 0f,
        1f, 0f,
        0f, 1f,
        1f, 1f,
    )

    fun draw() {
        GLES30.glEnableVertexAttribArray(ATTRIB_POSITION)
        GLES30.glVertexAttribPointer(ATTRIB_POSITION, 2, GLES30.GL_FLOAT, false, 0, positions)
        GLES30.glEnableVertexAttribArray(ATTRIB_TEX_COORD)
        GLES30.glVertexAttribPointer(ATTRIB_TEX_COORD, 2, GLES30.GL_FLOAT, false, 0, texCoords)

        GLES30.glDrawArrays(GLES30.GL_TRIANGLE_STRIP, 0, VERTEX_COUNT)

        GLES30.glDisableVertexAttribArray(ATTRIB_POSITION)
        GLES30.glDisableVertexAttribArray(ATTRIB_TEX_COORD)
    }

    companion object {
        const val ATTRIB_POSITION = 0
        const val ATTRIB_TEX_COORD = 1
        private const val VERTEX_COUNT = 4

        private fun floatBufferOf(vararg values: Float): FloatBuffer {
            val buffer = ByteBuffer.allocateDirect(values.size * Float.SIZE_BYTES)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer()
            buffer.put(values).position(0)
            return buffer
        }
    }
}
