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

    private companion object {
        const val PREFS_NAME = "app_settings"
        const val KEY_SAVE_ORIGINAL = "save_original_too"
    }
}
