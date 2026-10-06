package com.aiguidecamera.filter

import com.aiguidecamera.guide.ShootingMode

/**
 * 앱이 제공하는 필터 목록과 모드별 기본 필터.
 * 색 보정 값(색온도·채도 등)은 tools/generate_luts.py의 표에, 그레인·비네팅 값은 이 표에 있다.
 */
object FilterCatalog {

    const val ORIGINAL_ID = "none"

    val ORIGINAL = FilterPreset(ORIGINAL_ID, "원본", lutAssetPath = null, vignette = 0f, grain = 0f)

    val all: List<FilterPreset> = listOf(
        ORIGINAL,
        //     id              이름          비네팅  그레인
        preset("clear_skin",   "맑은 피부",   0.00f, 0.00f),
        preset("warm_film",    "따뜻한 필름", 0.25f, 0.06f),
        preset("cool_film",    "차가운 필름", 0.25f, 0.06f),
        preset("pastel",       "파스텔",      0.00f, 0.00f),
        preset("vintage_fade", "빈티지",      0.35f, 0.10f),
        preset("mono",         "흑백",        0.30f, 0.08f),
        preset("vivid",        "생기",        0.00f, 0.00f),
        preset("food_warm",    "음식 따뜻함", 0.15f, 0.00f),
        preset("food_crisp",   "음식 선명",   0.00f, 0.00f),
        preset("cafe_mood",    "카페 무드",   0.30f, 0.05f),
    )

    const val DEFAULT_INTENSITY = 0.8f
    const val DEFAULT_SKIN_LEVEL = 0.4f

    private const val PORTRAIT_DEFAULT_ID = "clear_skin"
    private const val FOOD_DEFAULT_ID = "food_warm"

    fun byId(id: String): FilterPreset = all.firstOrNull { it.id == id } ?: ORIGINAL

    fun defaultFor(mode: ShootingMode): FilterPreset = when (mode) {
        ShootingMode.PORTRAIT -> byId(PORTRAIT_DEFAULT_ID)
        ShootingMode.FOOD -> byId(FOOD_DEFAULT_ID)
    }

    private fun preset(id: String, name: String, vignette: Float, grain: Float) =
        FilterPreset(id, name, lutAssetPath = "luts/$id.png", vignette = vignette, grain = grain)
}
