package com.aiguidecamera.filter

/**
 * 필터 하나의 정의. 색감은 LUT PNG(tools/generate_luts.py가 생성)가, 그레인·비네팅은 셰이더 파라미터가 담당한다.
 * [lutAssetPath]가 null이면 색 변환 없는 "원본" 필터다.
 */
data class FilterPreset(
    val id: String,
    val displayName: String,
    val lutAssetPath: String?,
    val vignette: Float,
    val grain: Float,
)
