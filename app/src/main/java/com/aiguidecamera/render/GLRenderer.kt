package com.aiguidecamera.render

import android.graphics.SurfaceTexture
import android.opengl.GLES11Ext
import android.opengl.GLES30
import android.opengl.GLSurfaceView
import android.opengl.Matrix
import android.os.Handler
import android.os.Looper
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

/**
 * 카메라 프리뷰를 GLSurfaceView에 그리는 렌더러.
 * OES 외부 텍스처(SurfaceTexture) → 프리뷰 셰이더 → 화면. 회전·center-crop은 정점 행렬로 처리한다.
 *
 * 스레드: GLSurfaceView.Renderer 콜백은 GL 스레드, [setPreviewGeometry]는 어느 스레드에서 불러도 된다.
 * 새 SurfaceTexture가 만들어질 때마다(GL 컨텍스트 재생성 포함) [onSurfaceTextureReady]를 메인 스레드로 알린다.
 */
class GLRenderer(
    private val glView: GLSurfaceView,
    private val onSurfaceTextureReady: (SurfaceTexture) -> Unit,
) : GLSurfaceView.Renderer {

    private val mainHandler = Handler(Looper.getMainLooper())

    // --- GL 스레드 전용 상태 ---
    private var program: GlProgram? = null
    private var quad: FullScreenQuad? = null
    private var oesTextureId = 0
    private var surfaceTexture: SurfaceTexture? = null

    private var uMvpMatrix = 0
    private var uTexMatrix = 0
    private var uTexture = 0

    private val texMatrix = FloatArray(16)
    private val mvpMatrix = FloatArray(16)

    private var viewWidth = 0
    private var viewHeight = 0
    private var bufferWidth = 0
    private var bufferHeight = 0
    private var rotationDegrees = 0
    private var mirrorHorizontally = false

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        // GL 컨텍스트가 새로 만들어졌으므로 이전 GL 리소스는 모두 무효다. 새로 만든다.
        val newProgram = GlProgram(ShaderSources.VERTEX, ShaderSources.FRAGMENT_OES)
        program = newProgram
        uMvpMatrix = newProgram.uniformLocation("uMvpMatrix")
        uTexMatrix = newProgram.uniformLocation("uTexMatrix")
        uTexture = newProgram.uniformLocation("uTexture")
        quad = FullScreenQuad()

        oesTextureId = createOesTexture()
        val newSurfaceTexture = SurfaceTexture(oesTextureId)
        newSurfaceTexture.setOnFrameAvailableListener { glView.requestRender() }
        surfaceTexture = newSurfaceTexture

        Matrix.setIdentityM(texMatrix, 0)
        Matrix.setIdentityM(mvpMatrix, 0)
        mainHandler.post { onSurfaceTextureReady(newSurfaceTexture) }
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        viewWidth = width
        viewHeight = height
        updateMvpMatrix()
    }

    override fun onDrawFrame(gl: GL10?) {
        GLES30.glClearColor(0f, 0f, 0f, 1f)
        GLES30.glClear(GLES30.GL_COLOR_BUFFER_BIT)

        val texture = surfaceTexture ?: return
        val drawProgram = program ?: return
        val drawQuad = quad ?: return

        texture.updateTexImage()
        texture.getTransformMatrix(texMatrix)
        if (bufferWidth == 0 || bufferHeight == 0) return

        GLES30.glViewport(0, 0, viewWidth, viewHeight)
        drawProgram.use()
        GLES30.glActiveTexture(GLES30.GL_TEXTURE0)
        GLES30.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, oesTextureId)
        GLES30.glUniform1i(uTexture, 0)
        GLES30.glUniformMatrix4fv(uMvpMatrix, 1, false, mvpMatrix, 0)
        GLES30.glUniformMatrix4fv(uTexMatrix, 1, false, texMatrix, 0)
        drawQuad.draw()
    }

    /**
     * 카메라 버퍼 크기(센서 방향 기준)와 똑바로 세우기 위한 시계방향 회전각을 알려준다.
     * 전면 카메라는 [mirror]=true로 좌우 반전한다.
     */
    fun setPreviewGeometry(width: Int, height: Int, rotation: Int, mirror: Boolean) {
        glView.queueEvent {
            bufferWidth = width
            bufferHeight = height
            rotationDegrees = rotation
            mirrorHorizontally = mirror
            updateMvpMatrix()
        }
    }

    /** 버퍼를 회전한 뒤 뷰를 꽉 채우도록(center-crop) 확대하는 행렬을 만든다. */
    private fun updateMvpMatrix() {
        Matrix.setIdentityM(mvpMatrix, 0)
        if (viewWidth == 0 || viewHeight == 0 || bufferWidth == 0 || bufferHeight == 0) return

        val isSideways = rotationDegrees % 180 != 0
        val rotatedWidth = if (isSideways) bufferHeight else bufferWidth
        val rotatedHeight = if (isSideways) bufferWidth else bufferHeight
        val contentAspect = rotatedWidth.toFloat() / rotatedHeight
        val viewAspect = viewWidth.toFloat() / viewHeight

        var scaleX = 1f
        var scaleY = 1f
        if (contentAspect > viewAspect) {
            scaleX = contentAspect / viewAspect
        } else {
            scaleY = viewAspect / contentAspect
        }
        if (mirrorHorizontally) scaleX = -scaleX

        // 최종 = Scale * Rotate. setRotateM은 반시계 방향이 양수이므로 시계방향 회전은 음수로 준다.
        Matrix.scaleM(mvpMatrix, 0, scaleX, scaleY, 1f)
        Matrix.rotateM(mvpMatrix, 0, -rotationDegrees.toFloat(), 0f, 0f, 1f)
    }

    private fun createOesTexture(): Int {
        val ids = IntArray(1)
        GLES30.glGenTextures(1, ids, 0)
        GLES30.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, ids[0])
        GLES30.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES30.GL_TEXTURE_MIN_FILTER, GLES30.GL_LINEAR)
        GLES30.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES30.GL_TEXTURE_MAG_FILTER, GLES30.GL_LINEAR)
        GLES30.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES30.GL_TEXTURE_WRAP_S, GLES30.GL_CLAMP_TO_EDGE)
        GLES30.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES30.GL_TEXTURE_WRAP_T, GLES30.GL_CLAMP_TO_EDGE)
        return ids[0]
    }
}
