package com.aiguidecamera.filter

import android.content.Context
import com.aiguidecamera.guide.ShootingMode

/**
 * 모드별 마지막 선택 필터·강도와 피부 보정 강도를 기억한다 (SharedPreferences).
 * 저장된 값이 없으면 [FilterCatalog]의 모드별 기본값을 준다.
 */
class FilterPreferences(context: Context) {

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun filterFor(mode: ShootingMode): FilterPreset {
        val savedId = prefs.getString(filterKey(mode), null) ?: return FilterCatalog.defaultFor(mode)
        return FilterCatalog.byId(savedId)
    }

    fun intensityFor(mode: ShootingMode): Float =
        prefs.getFloat(intensityKey(mode), FilterCatalog.DEFAULT_INTENSITY)

    fun skinSmoothLevel(): Float = prefs.getFloat(KEY_SKIN_LEVEL, FilterCatalog.DEFAULT_SKIN_LEVEL)

    fun save(mode: ShootingMode, filter: FilterPreset, intensity: Float) {
        prefs.edit()
            .putString(filterKey(mode), filter.id)
            .putFloat(intensityKey(mode), intensity)
            .apply()
    }

    fun saveSkinSmoothLevel(level: Float) {
        prefs.edit().putFloat(KEY_SKIN_LEVEL, level).apply()
    }

    private fun filterKey(mode: ShootingMode) = "filter_${mode.name}"
    private fun intensityKey(mode: ShootingMode) = "intensity_${mode.name}"

    private companion object {
        const val PREFS_NAME = "filter_preferences"
        const val KEY_SKIN_LEVEL = "skin_smooth_level"
    }
}
