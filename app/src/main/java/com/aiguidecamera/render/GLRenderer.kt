package com.aiguidecamera.render

import android.graphics.RectF
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
 * OES 외부 텍스처(SurfaceTexture) → 필터 셰이더 → 화면. 회전·center-crop은 정점 행렬로 처리한다.
 *
 * 스레드: GLSurfaceView.Renderer 콜백은 GL 스레드, set* 함수는 어느 스레드에서 불러도 된다(queueEvent로 넘긴다).
 * 새 SurfaceTexture가 만들어질 때마다(GL 컨텍스트 재생성 포함) [onSurfaceTextureReady]를 메인 스레드로 알린다.
 */
class GLRenderer(
    private val glView: GLSurfaceView,
    private val lutLoader: LutLoader,
    private val onSurfaceTextureReady: (SurfaceTexture) -> Unit,
) : GLSurfaceView.Renderer {

    private val mainHandler = Handler(Looper.getMainLooper())

    // --- GL 스레드 전용 상태 ---
    private var filterShader: FilterShader? = null
    private var oesTextureId = 0
    private var surfaceTexture: SurfaceTexture? = null

    private val texMatrix = FloatArray(16)
    private val mvpMatrix = FloatArray(16)
    private val imageTransform = FloatArray(4)

    private var viewWidth = 0
    private var viewHeight = 0
    private var bufferWidth = 0
    private var bufferHeight = 0
    private var rotationDegrees = 0
    private var mirrorHorizontally = false

    /** GL 컨텍스트가 다시 만들어져도 복원할 수 있도록 마지막 값을 기억한다. */
    private var filterParams: FilterParams = FilterParams.ORIGINAL
    private var faceBoxes: List<RectF> = emptyList()

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        // GL 컨텍스트가 새로 만들어졌으므로 이전 GL 리소스는 모두 무효다. 새로 만든다.
        val shader = FilterShader(ShaderSources.FRAGMENT_OES, lutLoader)
        shader.setParams(filterParams)
        shader.setFaceBoxes(faceBoxes)
        filterShader = shader

        oesTextureId = createOesTexture()
        val newSurfaceTexture = SurfaceTexture(oesTextureId)
        newSurfaceTexture.setOnFrameAvailableListener { glView.requestRender() }
        surfaceTexture = newSurfaceTexture

        Matrix.setIdentityM(texMatrix, 0)
        updateMvpMatrix()
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
        val shader = filterShader ?: return

        texture.updateTexImage()
        texture.getTransformMatrix(texMatrix)
        if (bufferWidth == 0 || bufferHeight == 0) return

        GLES30.glViewport(0, 0, viewWidth, viewHeight)
        shader.draw(
            textureTarget = GLES11Ext.GL_TEXTURE_EXTERNAL_OES,
            textureId = oesTextureId,
            mvpMatrix = mvpMatrix,
            texMatrix = texMatrix,
            viewWidth = viewWidth,
            viewHeight = viewHeight,
            imageTransform = imageTransform,
            sourceWidth = bufferWidth,
            sourceHeight = bufferHeight,
        )
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

    fun setFilterParams(params: FilterParams) {
        glView.queueEvent {
            filterParams = params
            filterShader?.setParams(params)
        }
        glView.requestRender()
    }

    /** 얼굴 박스(똑바로 선 사진 기준 0~1 좌표). 프리뷰 피부 보정용으로 Phase 3 분석 결과가 넣는다. */
    fun setFaceBoxes(boxes: List<RectF>) {
        glView.queueEvent {
            faceBoxes = boxes
            filterShader?.setFaceBoxes(boxes)
        }
    }

    /**
     * 버퍼를 회전한 뒤 뷰를 꽉 채우도록(center-crop) 확대하는 행렬을 만들고,
     * 화면 좌표 → 똑바로 선 사진 좌표 변환([imageTransform])도 함께 계산한다.
     */
    private fun updateMvpMatrix() {
        Matrix.setIdentityM(mvpMatrix, 0)
        setImageTransform(1f, 1f, mirror = false)
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
        setImageTransform(visibleX = 1f / scaleX, visibleY = 1f / scaleY, mirror = mirrorHorizontally)
        if (mirrorHorizontally) scaleX = -scaleX

        // 최종 = Scale * Rotate. setRotateM은 반시계 방향이 양수이므로 시계방향 회전은 음수로 준다.
        Matrix.scaleM(mvpMatrix, 0, scaleX, scaleY, 1f)
        Matrix.rotateM(mvpMatrix, 0, -rotationDegrees.toFloat(), 0f, 0f, 1f)
    }

    /**
     * 화면에는 사진의 가운데 [visibleX] x [visibleY] 비율만 보인다.
     * 화면 좌표는 아래가 0, 사진 좌표는 위가 0이므로 y를 뒤집는다. 거울 모드면 x도 뒤집는다(저장본은 거울상이 아니다).
     */
    private fun setImageTransform(visibleX: Float, visibleY: Float, mirror: Boolean) {
        val signX = if (mirror) -1f else 1f
        imageTransform[0] = signX * visibleX
        imageTransform[1] = -visibleY
        imageTransform[2] = 0.5f - signX * visibleX * 0.5f
        imageTransform[3] = 0.5f + visibleY * 0.5f
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
