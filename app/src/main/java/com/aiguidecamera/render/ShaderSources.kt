package com.aiguidecamera.render

/**
 * GLSL 소스 모음. 프리뷰(OES 외부 텍스처)와 저장(2D 텍스처)은 sampleSource() 선언만 다르고
 * 나머지 필터 본문 [FILTER_BODY]는 같은 문자열을 공유한다 → 프리뷰와 저장본 색감이 일치한다.
 *
 * 좌표계
 * - vTexCoord: 원본 텍스처 좌표 (프리뷰는 카메라 버퍼 방향, 저장은 똑바로 선 비트맵 방향)
 * - imageCoord(): 똑바로 선 사진 기준 정규화 좌표 (0,0 = 왼쪽 위). 얼굴 박스·비네팅·그레인이 쓴다.
 */
object ShaderSources {

    const val MAX_FACES = 4

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

    private const val OES_HEADER = """#version 300 es
#extension GL_OES_EGL_image_external_essl3 : require
precision highp float;
uniform samplerExternalOES uTexture;
vec4 sampleSource(vec2 uv) { return texture(uTexture, uv); }
"""

    private const val TEXTURE_2D_HEADER = """#version 300 es
precision highp float;
uniform sampler2D uTexture;
vec4 sampleSource(vec2 uv) { return texture(uTexture, uv); }
"""

    private const val FILTER_BODY = """
in vec2 vTexCoord;
out vec4 fragColor;

uniform highp sampler3D uLut;
uniform float uLutEnabled;      // 1.0 = LUT 적용, 0.0 = 원본 필터
uniform float uIntensity;       // 0~1, 원본과 필터 결과를 mix
uniform float uVignette;        // 0~1
uniform float uGrain;           // 0~1
uniform vec2 uViewSize;         // 그리는 대상(화면/FBO) 픽셀 크기
uniform vec4 uImageTransform;   // imageCoord = (gl_FragCoord.xy / uViewSize) * xy + zw

const float LUT_SIZE = 64.0;
const float GRAIN_CELLS = 700.0;  // 사진 가로를 이만큼의 그레인 셀로 나눈다 (해상도와 무관하게 같은 질감)

vec2 imageCoord() {
    vec2 screen = gl_FragCoord.xy / uViewSize;
    return screen * uImageTransform.xy + uImageTransform.zw;
}

vec3 applyLut(vec3 color) {
    vec3 coord = color * ((LUT_SIZE - 1.0) / LUT_SIZE) + 0.5 / LUT_SIZE;
    return texture(uLut, coord).rgb;
}

float vignetteFactor(vec2 p) {
    float distanceFromCenter = length(p - 0.5) * 1.4142;
    return 1.0 - smoothstep(0.35, 1.0, distanceFromCenter);
}

float hash(vec2 cell) {
    return fract(sin(dot(cell, vec2(12.9898, 78.233))) * 43758.5453);
}

float grainNoise(vec2 p) {
    vec2 q = p * GRAIN_CELLS;
    vec2 cell = floor(q);
    vec2 f = fract(q);
    f = f * f * (3.0 - 2.0 * f);
    float a = hash(cell);
    float b = hash(cell + vec2(1.0, 0.0));
    float c = hash(cell + vec2(0.0, 1.0));
    float d = hash(cell + vec2(1.0, 1.0));
    return mix(mix(a, b, f.x), mix(c, d, f.x), f.y);
}

void main() {
    vec2 p = imageCoord();
    vec3 color = sampleSource(vTexCoord).rgb;

    color = applySkinSmooth(color, vTexCoord, p);

    vec3 graded = uLutEnabled > 0.5 ? applyLut(color) : color;
    color = mix(color, graded, uIntensity);

    color *= mix(1.0, vignetteFactor(p), uVignette * uIntensity);
    color += (grainNoise(p) - 0.5) * uGrain * uIntensity;

    fragColor = vec4(clamp(color, 0.0, 1.0), 1.0);
}
"""

    /** 카메라 프리뷰용 (samplerExternalOES). */
    const val FRAGMENT_OES = OES_HEADER + SkinSmoothShader.GLSL + FILTER_BODY

    /** 촬영본·편집본 오프스크린 렌더링용 (sampler2D). */
    const val FRAGMENT_2D = TEXTURE_2D_HEADER + SkinSmoothShader.GLSL + FILTER_BODY
}
