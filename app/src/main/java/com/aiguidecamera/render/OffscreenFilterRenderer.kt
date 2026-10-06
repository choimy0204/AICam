package com.aiguidecamera.render

import android.graphics.Bitmap
import android.graphics.Matrix
import android.opengl.EGL14
import android.opengl.EGLConfig
import android.opengl.EGLContext
import android.opengl.EGLDisplay
import android.opengl.EGLExt
import android.opengl.EGLSurface
import android.opengl.GLES30
import android.opengl.GLUtils
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.withContext
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.concurrent.Executors

/**
 * 고해상도 비트맵에 프리뷰와 같은 셰이더를 적용해 새 비트맵으로 돌려주는 오프스크린 렌더러.
 * 촬영 저장 경로와 사후 편집 경로가 이 클래스 하나를 공유한다 (별도 구현 금지).
 *
 * 전용 스레드 하나에 EGL 컨텍스트(1x1 pbuffer)를 만들어 계속 재사용하고, 실제 그리기는 FBO에 한다.
 * 앱 전역에서 하나만 쓰며([com.aiguidecamera.AIGuideCameraApp]) 프로세스 수명 동안 유지한다.
 */
class OffscreenFilterRenderer {

    private val glDispatcher = Executors.newSingleThreadExecutor { runnable ->
        Thread(runnable, "OffscreenFilterRenderer")
    }.asCoroutineDispatcher()

    // --- GL 스레드 전용 상태 ---
    private var eglDisplay: EGLDisplay = EGL14.EGL_NO_DISPLAY
    private var eglContext: EGLContext = EGL14.EGL_NO_CONTEXT
    private var eglSurface: EGLSurface = EGL14.EGL_NO_SURFACE
    private var program: GlProgram? = null
    private var quad: FullScreenQuad? = null
    private var maxTextureSize = 0

    private val identityMatrix = FloatArray(16).also { android.opengl.Matrix.setIdentityM(it, 0) }

    /**
     * [source]에 셰이더를 적용한 결과를 같은 크기의 새 비트맵으로 반환한다.
     * GPU 한계(GL_MAX_TEXTURE_SIZE)보다 크면 비율을 유지한 채 줄여서 처리한다.
     */
    suspend fun render(source: Bitmap): Bitmap = withContext(glDispatcher) {
        ensureEglReady()
        val input = downscaleIfNeeded(source)
        try {
            drawToBitmap(input)
        } finally {
            if (input !== source) input.recycle()
        }
    }

    private fun drawToBitmap(input: Bitmap): Bitmap {
        val width = input.width
        val height = input.height

        val inputTexture = createTexture2D()
        GLUtils.texImage2D(GLES30.GL_TEXTURE_2D, 0, input, 0)

        val outputTexture = createTexture2D()
        GLES30.glTexImage2D(
            GLES30.GL_TEXTURE_2D, 0, GLES30.GL_RGBA, width, height, 0,
            GLES30.GL_RGBA, GLES30.GL_UNSIGNED_BYTE, null,
        )
        val framebuffer = IntArray(1)
        GLES30.glGenFramebuffers(1, framebuffer, 0)
        GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, framebuffer[0])
        GLES30.glFramebufferTexture2D(
            GLES30.GL_FRAMEBUFFER, GLES30.GL_COLOR_ATTACHMENT0, GLES30.GL_TEXTURE_2D, outputTexture, 0,
        )
        check(GLES30.glCheckFramebufferStatus(GLES30.GL_FRAMEBUFFER) == GLES30.GL_FRAMEBUFFER_COMPLETE) {
            "FBO 생성 실패 (${width}x$height)"
        }

        val drawProgram = requireNotNull(program)
        GLES30.glViewport(0, 0, width, height)
        drawProgram.use()
        GLES30.glActiveTexture(GLES30.GL_TEXTURE0)
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, inputTexture)
        GLES30.glUniform1i(drawProgram.uniformLocation("uTexture"), 0)
        // 업로드한 비트맵의 첫 행이 t=0, glReadPixels의 첫 행도 y=0 → 뒤집힘이 서로 상쇄되어 행렬은 단위행렬이면 된다.
        GLES30.glUniformMatrix4fv(drawProgram.uniformLocation("uMvpMatrix"), 1, false, identityMatrix, 0)
        GLES30.glUniformMatrix4fv(drawProgram.uniformLocation("uTexMatrix"), 1, false, identityMatrix, 0)
        requireNotNull(quad).draw()

        val pixels = ByteBuffer.allocateDirect(width * height * BYTES_PER_PIXEL).order(ByteOrder.nativeOrder())
        GLES30.glReadPixels(0, 0, width, height, GLES30.GL_RGBA, GLES30.GL_UNSIGNED_BYTE, pixels)
        pixels.rewind()
        val output = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        output.copyPixelsFromBuffer(pixels)

        GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, 0)
        GLES30.glDeleteFramebuffers(1, framebuffer, 0)
        GLES30.glDeleteTextures(2, intArrayOf(inputTexture, outputTexture), 0)
        return output
    }

    private fun downscaleIfNeeded(source: Bitmap): Bitmap {
        val longSide = maxOf(source.width, source.height)
        if (longSide <= maxTextureSize) return source
        val scale = maxTextureSize.toFloat() / longSide
        val matrix = Matrix().apply { setScale(scale, scale) }
        return Bitmap.createBitmap(source, 0, 0, source.width, source.height, matrix, true)
    }

    private fun createTexture2D(): Int {
        val ids = IntArray(1)
        GLES30.glGenTextures(1, ids, 0)
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, ids[0])
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MIN_FILTER, GLES30.GL_LINEAR)
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MAG_FILTER, GLES30.GL_LINEAR)
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_WRAP_S, GLES30.GL_CLAMP_TO_EDGE)
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_WRAP_T, GLES30.GL_CLAMP_TO_EDGE)
        return ids[0]
    }

    private fun ensureEglReady() {
        if (eglContext != EGL14.EGL_NO_CONTEXT) return

        eglDisplay = EGL14.eglGetDisplay(EGL14.EGL_DEFAULT_DISPLAY)
        check(eglDisplay != EGL14.EGL_NO_DISPLAY) { "EGL 디스플레이를 얻지 못했습니다" }
        val version = IntArray(2)
        check(EGL14.eglInitialize(eglDisplay, version, 0, version, 1)) { "EGL 초기화 실패" }

        val configAttributes = intArrayOf(
            EGL14.EGL_RENDERABLE_TYPE, EGLExt.EGL_OPENGL_ES3_BIT_KHR,
            EGL14.EGL_SURFACE_TYPE, EGL14.EGL_PBUFFER_BIT,
            EGL14.EGL_RED_SIZE, 8,
            EGL14.EGL_GREEN_SIZE, 8,
            EGL14.EGL_BLUE_SIZE, 8,
            EGL14.EGL_ALPHA_SIZE, 8,
            EGL14.EGL_NONE,
        )
        val configs = arrayOfNulls<EGLConfig>(1)
        val configCount = IntArray(1)
        EGL14.eglChooseConfig(eglDisplay, configAttributes, 0, configs, 0, 1, configCount, 0)
        val config = checkNotNull(configs[0]) { "ES 3.0 EGL 설정을 찾지 못했습니다" }

        val contextAttributes = intArrayOf(EGL14.EGL_CONTEXT_CLIENT_VERSION, 3, EGL14.EGL_NONE)
        eglContext = EGL14.eglCreateContext(eglDisplay, config, EGL14.EGL_NO_CONTEXT, contextAttributes, 0)
        check(eglContext != EGL14.EGL_NO_CONTEXT) { "EGL 컨텍스트 생성 실패" }

        val surfaceAttributes = intArrayOf(EGL14.EGL_WIDTH, 1, EGL14.EGL_HEIGHT, 1, EGL14.EGL_NONE)
        eglSurface = EGL14.eglCreatePbufferSurface(eglDisplay, config, surfaceAttributes, 0)
        check(EGL14.eglMakeCurrent(eglDisplay, eglSurface, eglSurface, eglContext)) { "eglMakeCurrent 실패" }

        val limits = IntArray(1)
        GLES30.glGetIntegerv(GLES30.GL_MAX_TEXTURE_SIZE, limits, 0)
        maxTextureSize = limits[0]
        GLES30.glGetIntegerv(GLES30.GL_MAX_RENDERBUFFER_SIZE, limits, 0)
        maxTextureSize = minOf(maxTextureSize, limits[0])

        program = GlProgram(ShaderSources.VERTEX, ShaderSources.FRAGMENT_2D)
        quad = FullScreenQuad()
    }

    private companion object {
        const val BYTES_PER_PIXEL = 4
    }
}
