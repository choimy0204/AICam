package com.aiguidecamera.render

/**
 * GLSL 소스 모음. 프리뷰(OES 외부 텍스처)와 저장(2D 텍스처)은 샘플러 선언만 다르고
 * 색 처리 함수 [COLOR_PROCESSING]는 같은 문자열을 공유한다 → 프리뷰와 저장본 색감이 일치한다.
 */
object ShaderSources {

    /** 위치는 uMvpMatrix(회전·크롭), 텍스처 좌표는 uTexMatrix(SurfaceTexture 변환)로 변환한다. */
    const val VERTEX = """#version 300 es
layout(location = 0) in vec4 aPosition;
layout(location = 1) in vec4 aTexCoord;
uniform mat4 uMvpMatrix;
uniform mat4 uTexMatrix;
out vec2 vTexCoord;
void main() {
    gl_Position = uMvpMatrix * aPosition;
    vTexCoord = (uTexMatrix * aTexCoord).xy;
}
"""

    /** 색 처리 공용 함수. Phase 1은 패스스루. */
    private const val COLOR_PROCESSING = """
vec4 processColor(vec4 color) {
    return color;
}
"""

    private const val OES_HEADER = """#version 300 es
#extension GL_OES_EGL_image_external_essl3 : require
precision mediump float;
uniform samplerExternalOES uTexture;
in vec2 vTexCoord;
out vec4 fragColor;
"""

    private const val TEXTURE_2D_HEADER = """#version 300 es
precision mediump float;
uniform sampler2D uTexture;
in vec2 vTexCoord;
out vec4 fragColor;
"""

    private const val MAIN = """
void main() {
    fragColor = processColor(texture(uTexture, vTexCoord));
}
"""

    /** 카메라 프리뷰용 (samplerExternalOES). */
    const val FRAGMENT_OES = OES_HEADER + COLOR_PROCESSING + MAIN

    /** 촬영본·편집본 오프스크린 렌더링용 (sampler2D). */
    const val FRAGMENT_2D = TEXTURE_2D_HEADER + COLOR_PROCESSING + MAIN
}
