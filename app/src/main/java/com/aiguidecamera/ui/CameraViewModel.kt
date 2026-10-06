package com.aiguidecamera.ui

import android.app.Application
import android.graphics.Bitmap
import android.graphics.RectF
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.aiguidecamera.AIGuideCameraApp
import com.aiguidecamera.analysis.FrameAnalysisResult
import com.aiguidecamera.analysis.FrameAnalyzer
import com.aiguidecamera.analysis.SensorReader
import com.aiguidecamera.camera.CameraController
import com.aiguidecamera.filter.FilterPreset
import com.aiguidecamera.filter.FilterThumbnailFactory
import com.aiguidecamera.guide.ShootingMode
import com.aiguidecamera.render.FilterParams
import com.aiguidecamera.storage.PhotoRecord
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * 카메라 화면 상태(모드·필터·강도)와 "촬영 → 얼굴 검출 → 오프스크린 렌더 → 저장 → PhotoRecord 기록" 흐름을 관리한다.
 */
class CameraViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as AIGuideCameraApp
    private val photoDao = app.database.photoRecordDao()
    private val filterPreferences = app.filterPreferences

    val cameraController = CameraController(application)

    val latestPhoto: StateFlow<PhotoRecord?> = photoDao.observeLatest()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), null)

    private val _isCapturing = MutableStateFlow(false)
    val isCapturing: StateFlow<Boolean> = _isCapturing.asStateFlow()

    /** 한 번 보여주고 [onMessageShown]으로 지우는 사용자 안내 문구. */
    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    private val _mode = MutableStateFlow(ShootingMode.PORTRAIT)
    val mode: StateFlow<ShootingMode> = _mode.asStateFlow()

    private val _filterParams = MutableStateFlow(paramsFor(ShootingMode.PORTRAIT))
    val filterParams: StateFlow<FilterParams> = _filterParams.asStateFlow()

    /** 필터 id → 피커 썸네일. 앱 시작 직후 한 번 만든다. */
    private val _thumbnails = MutableStateFlow<Map<String, Bitmap>>(emptyMap())
    val thumbnails: StateFlow<Map<String, Bitmap>> = _thumbnails.asStateFlow()

    val sensorReader = SensorReader(application)

    /** 가장 최근 분석 프레임 결과. 분석 스레드에서 갱신된다. */
    private val _analysis = MutableStateFlow<FrameAnalysisResult?>(null)
    val analysis: StateFlow<FrameAnalysisResult?> = _analysis.asStateFlow()

    /** 실시간 프리뷰 피부 보정에 쓰는 얼굴 박스(똑바로 선 이미지 기준 0~1). */
    private val _faceBoxes = MutableStateFlow<List<RectF>>(emptyList())
    val faceBoxes: StateFlow<List<RectF>> = _faceBoxes.asStateFlow()

    private val frameAnalyzer = FrameAnalyzer(sensorReader, cameraController.analysisExecutor) { result ->
        _analysis.value = result
        if (result.faces.isNotEmpty() || _faceBoxes.value.isNotEmpty()) {
            _faceBoxes.value = result.faces.map { it.box }
        }
    }

    init {
        cameraController.setAnalyzer(frameAnalyzer)
        viewModelScope.launch {
            try {
                _thumbnails.value = FilterThumbnailFactory(app.offscreenRenderer).createAll()
            } catch (error: Exception) {
                Log.w(TAG, "필터 썸네일 생성 실패", error)
            }
        }
    }

    fun onModeChange(newMode: ShootingMode) {
        if (newMode == _mode.value) return
        _mode.value = newMode
        frameAnalyzer.mode = newMode
        _filterParams.value = paramsFor(newMode)
    }

    fun onFilterSelect(preset: FilterPreset) {
        updateParams(_filterParams.value.copy(preset = preset))
    }

    fun onIntensityChange(intensity: Float) {
        updateParams(_filterParams.value.copy(intensity = intensity))
    }

    fun onSkinSmoothLevelChange(level: Float) {
        _filterParams.value = _filterParams.value.copy(skinSmoothLevel = level)
        filterPreferences.saveSkinSmoothLevel(level)
    }

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

    private fun updateParams(params: FilterParams) {
        _filterParams.value = params
        filterPreferences.save(_mode.value, params.preset, params.intensity)
    }

    /** 피부 보정은 인물 모드에서만 쓴다. 음식 모드는 0으로 둔다. */
    private fun paramsFor(mode: ShootingMode) = FilterParams(
        preset = filterPreferences.filterFor(mode),
        intensity = filterPreferences.intensityFor(mode),
        skinSmoothLevel = if (mode == ShootingMode.PORTRAIT) filterPreferences.skinSmoothLevel() else 0f,
    )

    private suspend fun captureAndSave() {
        val createdAt = System.currentTimeMillis()
        val params = _filterParams.value
        val shotMode = _mode.value
        val original = cameraController.takePicture()
        var filtered: Bitmap? = null
        try {
            val faceBoxes = if (params.skinSmoothLevel > 0f) detectFacesSafely(original) else emptyList()
            filtered = app.offscreenRenderer.render(original, params, faceBoxes)

            val saver = app.photoSaver
            val baseName = saver.baseNameFor(createdAt)
            val filteredUri = saver.saveJpeg(filtered, saver.filteredFileName(baseName))
            val originalUri = if (app.settings.saveOriginalToo.value) {
                saver.saveJpeg(original, saver.originalFileName(baseName))
            } else {
                null
            }

            photoDao.insert(
                PhotoRecord(
                    filteredUri = filteredUri.toString(),
                    originalUri = originalUri?.toString(),
                    filterId = params.preset.id,
                    filterIntensity = params.intensity,
                    skinSmoothLevel = params.skinSmoothLevel,
                    mode = shotMode.name,
                    createdAt = createdAt,
                ),
            )
        } finally {
            original.recycle()
            filtered?.recycle()
        }
    }

    /** 얼굴 검출이 실패해도 사진은 저장되어야 하므로, 실패하면 피부 보정 없이 진행한다. */
    private suspend fun detectFacesSafely(bitmap: Bitmap): List<RectF> = try {
        app.stillFaceDetector.detect(bitmap)
    } catch (error: Exception) {
        Log.w(TAG, "얼굴 검출 실패, 피부 보정 없이 저장합니다", error)
        emptyList()
    }

    override fun onCleared() {
        cameraController.clearAnalyzer()
        sensorReader.stop()
        // 진행 중인 ML Kit 콜백이 끝난 뒤 검출기를 닫도록 같은 실행기에 넣는다.
        cameraController.analysisExecutor.execute { frameAnalyzer.close() }
        cameraController.analysisExecutor.shutdown()
    }

    private companion object {
        const val TAG = "CameraViewModel"
        const val STOP_TIMEOUT_MS = 5_000L
    }
}
