package com.aiguidecamera.render

import android.opengl.GLES30
import android.util.Log

/**
 * 버텍스/프래그먼트 셰이더를 컴파일·링크해 하나의 GL 프로그램으로 묶는다.
 * 반드시 GL 컨텍스트가 current인 스레드에서 생성·사용해야 한다.
 */
class GlProgram(vertexSource: String, fragmentSource: String) {

    val handle: Int

    init {
        val vertexShader = compileShader(GLES30.GL_VERTEX_SHADER, vertexSource)
        val fragmentShader = compileShader(GLES30.GL_FRAGMENT_SHADER, fragmentSource)
        handle = GLES30.glCreateProgram()
        GLES30.glAttachShader(handle, vertexShader)
        GLES30.glAttachShader(handle, fragmentShader)
        GLES30.glLinkProgram(handle)

        val status = IntArray(1)
        GLES30.glGetProgramiv(handle, GLES30.GL_LINK_STATUS, status, 0)
        val linkLog = GLES30.glGetProgramInfoLog(handle)
        GLES30.glDeleteShader(vertexShader)
        GLES30.glDeleteShader(fragmentShader)
        if (status[0] == 0) {
            GLES30.glDeleteProgram(handle)
            throw IllegalStateException("셰이더 링크 실패: $linkLog")
        }
    }

    fun use() {
        GLES30.glUseProgram(handle)
    }

    fun uniformLocation(name: String): Int = GLES30.glGetUniformLocation(handle, name)

    fun release() {
        GLES30.glDeleteProgram(handle)
    }

    private fun compileShader(type: Int, source: String): Int {
        val shader = GLES30.glCreateShader(type)
        GLES30.glShaderSource(shader, source)
        GLES30.glCompileShader(shader)
        val status = IntArray(1)
        GLES30.glGetShaderiv(shader, GLES30.GL_COMPILE_STATUS, status, 0)
        if (status[0] == 0) {
            val log = GLES30.glGetShaderInfoLog(shader)
            GLES30.glDeleteShader(shader)
            Log.e(TAG, "셰이더 컴파일 실패:\n$source")
            throw IllegalStateException("셰이더 컴파일 실패: $log")
        }
        return shader
    }

    private companion object {
        const val TAG = "GlProgram"
    }
}
