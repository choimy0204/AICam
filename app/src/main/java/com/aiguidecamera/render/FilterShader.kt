package com.aiguidecamera.render

import android.graphics.RectF
import android.opengl.GLES30

/**
 * 필터 셰이더 프로그램과 유니폼, LUT 텍스처를 묶어 관리한다. 반드시 GL 스레드에서만 쓴다.
 * 프리뷰([GLRenderer], OES)와 저장([OffscreenFilterRenderer], 2D)이 같은 클래스·같은 셰이더 본문을 쓴다.
 */
class FilterShader(fragmentSource: String, private val lutLoader: LutLoader) {

    private val program = GlProgram(ShaderSources.VERTEX, fragmentSource)
    private val quad = FullScreenQuad()

    private val uMvpMatrix = program.uniformLocation("uMvpMatrix")
    private val uTexMatrix = program.uniformLocation("uTexMatrix")
    private val uTexture = program.uniformLocation("uTexture")
    private val uLut = program.uniformLocation("uLut")
    private val uLutEnabled = program.uniformLocation("uLutEnabled")
    private val uIntensity = program.uniformLocation("uIntensity")
    private val uVignette = program.uniformLocation("uVignette")
    private val uGrain = program.uniformLocation("uGrain")
    private val uViewSize = program.uniformLocation("uViewSize")
    private val uImageTransform = program.uniformLocation("uImageTransform")
    private val uSkinLevel = program.uniformLocation("uSkinLevel")
    private val uFaceCount = program.uniformLocation("uFaceCount")
    private val uFaceBoxes = program.uniformLocation("uFaceBoxes")
    private val uBlurStep = program.uniformLocation("uBlurStep")

    /** 이 GL 컨텍스트에 올린 LUT 텍스처 (asset 경로 → 텍스처 핸들). */
    private val lutTextures = HashMap<String, Int>()

    private var params: FilterParams = FilterParams.ORIGINAL
    private var lutTexture = 0

    private val faceBoxes = FloatArray(ShaderSources.MAX_FACES * 4)
    private var faceCount = 0

    fun setParams(newParams: FilterParams) {
        params = newParams
        val path = newParams.preset.lutAssetPath
        lutTexture = if (path == null) 0 else lutTextures.getOrPut(path) { lutLoader.createTexture(path) }
    }

    /** 얼굴 박스(똑바로 선 사진 기준 0~1 좌표). [ShaderSources.MAX_FACES]개까지만 쓴다. */
    fun setFaceBoxes(boxes: List<RectF>) {
        faceCount = minOf(boxes.size, ShaderSources.MAX_FACES)
        for (i in 0 until faceCount) {
            val box = boxes[i]
            faceBoxes[i * 4] = box.left
            faceBoxes[i * 4 + 1] = box.top
            faceBoxes[i * 4 + 2] = box.right
            faceBoxes[i * 4 + 3] = box.bottom
        }
    }

    /**
     * 현재 바인딩된 프레임버퍼에 그린다.
     * @param imageTransform imageCoord = (gl_FragCoord / viewSize) * xy + zw 의 (x, y, z, w)
     * @param sourceWidth 원본 텍스처 가로 픽셀 (피부 보정 블러 반경 계산용)
     */
    fun draw(
        textureTarget: Int,
        textureId: Int,
        mvpMatrix: FloatArray,
        texMatrix: FloatArray,
        viewWidth: Int,
        viewHeight: Int,
        imageTransform: FloatArray,
        sourceWidth: Int,
        sourceHeight: Int,
    ) {
        program.use()

        GLES30.glActiveTexture(GLES30.GL_TEXTURE0)
        GLES30.glBindTexture(textureTarget, textureId)
        GLES30.glUniform1i(uTexture, 0)

        GLES30.glActiveTexture(GLES30.GL_TEXTURE1)
        GLES30.glBindTexture(GLES30.GL_TEXTURE_3D, lutTexture)
        GLES30.glUniform1i(uLut, 1)
        GLES30.glUniform1f(uLutEnabled, if (lutTexture != 0) 1f else 0f)

        GLES30.glUniform1f(uIntensity, params.intensity)
        GLES30.glUniform1f(uVignette, params.preset.vignette)
        GLES30.glUniform1f(uGrain, params.preset.grain)
        GLES30.glUniform2f(uViewSize, viewWidth.toFloat(), viewHeight.toFloat())
        GLES30.glUniform4fv(uImageTransform, 1, imageTransform, 0)

        GLES30.glUniform1f(uSkinLevel, params.skinSmoothLevel)
        GLES30.glUniform1i(uFaceCount, faceCount)
        GLES30.glUniform4fv(uFaceBoxes, ShaderSources.MAX_FACES, faceBoxes, 0)
        val blurRadiusPx = SkinSmoothShader.BLUR_RADIUS_RATIO * minOf(sourceWidth, sourceHeight)
        GLES30.glUniform2f(uBlurStep, blurRadiusPx / sourceWidth, blurRadiusPx / sourceHeight)

        GLES30.glUniformMatrix4fv(uMvpMatrix, 1, false, mvpMatrix, 0)
        GLES30.glUniformMatrix4fv(uTexMatrix, 1, false, texMatrix, 0)
        quad.draw()

        GLES30.glActiveTexture(GLES30.GL_TEXTURE0)
    }
}
