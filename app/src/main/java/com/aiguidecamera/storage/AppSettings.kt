package com.aiguidecamera.storage

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** 앱 설정 값(SharedPreferences). 화면은 [com.aiguidecamera.ui.SettingsScreen]. */
class AppSettings(context: Context) {

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _saveOriginalToo = MutableStateFlow(prefs.getBoolean(KEY_SAVE_ORIGINAL, false))

    /** "원본도 함께 저장" (기본 OFF). 켜야 나중에 필터를 바꿀 수 있다. */
    val saveOriginalToo: StateFlow<Boolean> = _saveOriginalToo.asStateFlow()

    fun setSaveOriginalToo(enabled: Boolean) {
        _saveOriginalToo.value = enabled
        prefs.edit().putBoolean(KEY_SAVE_ORIGINAL, enabled).apply()
    }

    private val _autoStraighten = MutableStateFlow(prefs.getBoolean(KEY_AUTO_STRAIGHTEN, true))

    /** "수평 자동 보정" (기본 ON). 풍경·야경 사진을 찍은 순간의 기울기만큼 돌려 저장한다. */
    val autoStraighten: StateFlow<Boolean> = _autoStraighten.asStateFlow()

    fun setAutoStraighten(enabled: Boolean) {
        _autoStraighten.value = enabled
        prefs.edit().putBoolean(KEY_AUTO_STRAIGHTEN, enabled).apply()
    }

    private companion object {
        const val PREFS_NAME = "app_settings"
        const val KEY_SAVE_ORIGINAL = "save_original_too"
        const val KEY_AUTO_STRAIGHTEN = "auto_straighten"
    }
}
