package com.aiguidecamera.render

import com.aiguidecamera.filter.FilterCatalog
import com.aiguidecamera.filter.FilterPreset

/** 렌더러에 넘기는 필터 설정 한 묶음. 프리뷰와 저장이 같은 값을 쓰면 같은 결과가 나온다. */
data class FilterParams(
    val preset: FilterPreset,
    val intensity: Float,
    val skinSmoothLevel: Float,
) {
    companion object {
        val ORIGINAL = FilterParams(FilterCatalog.ORIGINAL, intensity = 0f, skinSmoothLevel = 0f)
    }
}
