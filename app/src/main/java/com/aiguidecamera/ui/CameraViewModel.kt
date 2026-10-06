package com.aiguidecamera.ui

import android.app.Application
import android.graphics.Bitmap
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.aiguidecamera.AIGuideCameraApp
import com.aiguidecamera.camera.CameraController
import com.aiguidecamera.guide.ShootingMode
import com.aiguidecamera.storage.PhotoRecord
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * 카메라 화면 상태와 "촬영 → 오프스크린 렌더 → 저장 → PhotoRecord 기록" 흐름을 관리한다.
 */
class CameraViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as AIGuideCameraApp
    private val photoDao = app.database.photoRecordDao()

    val cameraController = CameraController(application)

    val latestPhoto: StateFlow<PhotoRecord?> = photoDao.observeLatest()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), null)

    private val _isCapturing = MutableStateFlow(false)
    val isCapturing: StateFlow<Boolean> = _isCapturing.asStateFlow()

    /** 한 번 보여주고 [onMessageShown]으로 지우는 사용자 안내 문구. */
    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    private val mode = ShootingMode.PORTRAIT

    /** "원본도 함께 저장" 설정. 설정 화면은 Phase 6에서 연결한다. */
    private val saveOriginalToo = false

    fun onShutterClick() {
        if (_isCapturing.value) return
        _isCapturing.value = true
        viewModelScope.launch {
            try {
                captureAndSave()
            } catch (error: Exception) {
                _message.value = "사진을 저장하지 못했어요 (${error.message})"
            } finally {
                _isCapturing.value = false
            }
        }
    }

    fun onMessageShown() {
        _message.value = null
    }

    private suspend fun captureAndSave() {
        val createdAt = System.currentTimeMillis()
        val original = cameraController.takePicture()
        var filtered: Bitmap? = null
        try {
            filtered = app.offscreenRenderer.render(original)

            val saver = app.photoSaver
            val baseName = saver.baseNameFor(createdAt)
            val filteredUri = saver.saveJpeg(filtered, saver.filteredFileName(baseName))
            val originalUri = if (saveOriginalToo) {
                saver.saveJpeg(original, saver.originalFileName(baseName))
            } else {
                null
            }

            photoDao.insert(
                PhotoRecord(
                    filteredUri = filteredUri.toString(),
                    originalUri = originalUri?.toString(),
                    filterId = FILTER_ID_NONE,
                    filterIntensity = 0f,
                    skinSmoothLevel = 0f,
                    mode = mode.name,
                    createdAt = createdAt,
                ),
            )
        } finally {
            original.recycle()
            filtered?.recycle()
        }
    }

    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L
        const val FILTER_ID_NONE = "none"
    }
}
