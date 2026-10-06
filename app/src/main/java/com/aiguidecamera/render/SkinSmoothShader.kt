package com.aiguidecamera.render

/**
 * 피부 보정 GLSL 조각. 얼굴 박스로 만든 부드러운 타원 마스크 × 피부색 가능도 영역에만
 * 간이 bilateral 블러(색 차이가 큰 이웃은 덜 섞음 → 눈·눈썹 윤곽 보존)를 적용한다.
 * 필터 셰이더(FRAGMENT_OES / FRAGMENT_2D)에 그대로 포함되어 별도 패스 없이 한 번에 그린다.
 */
object SkinSmoothShader {

    /** 블러 반경: 사진 짧은 변 대비 비율. 해상도와 무관하게 프리뷰·저장본이 같은 느낌이 되도록 비율로 둔다. */
    const val BLUR_RADIUS_RATIO = 0.004f

    const val GLSL = """
uniform float uSkinLevel;                     // 0~1
uniform int uFaceCount;
uniform vec4 uFaceBoxes[${ShaderSources.MAX_FACES}];  // left, top, right, bottom (imageCoord 기준)
uniform vec2 uBlurStep;                       // 텍스처 좌표 기준 블러 한 걸음

const float SKIN_COLOR_SIGMA = 0.10;
const float FACE_MASK_FEATHER = 0.35;

const vec2 BLUR_DIRECTIONS[8] = vec2[8](
    vec2(1.0, 0.0), vec2(0.7071, 0.7071), vec2(0.0, 1.0), vec2(-0.7071, 0.7071),
    vec2(-1.0, 0.0), vec2(-0.7071, -0.7071), vec2(0.0, -1.0), vec2(0.7071, -0.7071)
);

float faceMask(vec2 p) {
    float mask = 0.0;
    for (int i = 0; i < ${ShaderSources.MAX_FACES}; i++) {
        if (i >= uFaceCount) break;
        vec4 box = uFaceBoxes[i];
        vec2 center = vec2((box.x + box.z) * 0.5, (box.y + box.w) * 0.5);
        // 이마·턱까지 덮도록 박스보다 조금 크게 잡은 타원
        vec2 radius = vec2((box.z - box.x) * 0.6, (box.w - box.y) * 0.7);
        float d = length((p - center) / radius);
        mask = max(mask, 1.0 - smoothstep(1.0 - FACE_MASK_FEATHER, 1.0, d));
    }
    return mask;
}

// YCbCr 기준 피부색 범위에 얼마나 가까운지 0~1
float skinLikelihood(vec3 c) {
    float cb = 0.5 - 0.168736 * c.r - 0.331264 * c.g + 0.5 * c.b;
    float cr = 0.5 + 0.5 * c.r - 0.418688 * c.g - 0.081312 * c.b;
    float inCb = smoothstep(0.26, 0.32, cb) * (1.0 - smoothstep(0.50, 0.56, cb));
    float inCr = smoothstep(0.50, 0.54, cr) * (1.0 - smoothstep(0.68, 0.72, cr));
    return inCb * inCr;
}

vec3 bilateralBlur(vec3 center, vec2 uv) {
    vec3 sum = center;
    float weightSum = 1.0;
    for (int ring = 1; ring <= 2; ring++) {
        for (int i = 0; i < 8; i++) {
            vec3 neighbor = sampleSource(uv + BLUR_DIRECTIONS[i] * uBlurStep * float(ring)).rgb;
            vec3 diff = neighbor - center;
            float weight = exp(-dot(diff, diff) / (2.0 * SKIN_COLOR_SIGMA * SKIN_COLOR_SIGMA));
            sum += neighbor * weight;
            weightSum += weight;
        }
    }
    return sum / weightSum;
}

vec3 applySkinSmooth(vec3 color, vec2 uv, vec2 p) {
    if (uSkinLevel <= 0.0 || uFaceCount == 0) return color;
    float mask = faceMask(p);
    if (mask <= 0.001) return color;
    mask *= skinLikelihood(color) * uSkinLevel;
    if (mask <= 0.001) return color;
    return mix(color, bilateralBlur(color, uv), mask);
}
"""
}
