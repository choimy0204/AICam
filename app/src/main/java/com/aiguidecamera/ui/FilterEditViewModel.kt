package com.aiguidecamera.ui

import android.app.Application
import android.graphics.Bitmap
import android.graphics.RectF
import android.net.Uri
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.aiguidecamera.AIGuideCameraApp
import com.aiguidecamera.filter.FilterCatalog
import com.aiguidecamera.filter.FilterPreset
import com.aiguidecamera.filter.FilterThumbnailFactory
import com.aiguidecamera.render.FilterParams
import com.aiguidecamera.storage.PhotoRecord
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.launch

/**
 * 사후 필터 편집: 원본을 불러와 촬영 때와 같은 [com.aiguidecamera.render.OffscreenFilterRenderer]로 미리보기를 만들고,
 * 저장하면 기존 파일은 그대로 두고 새 필터본 + 새 PhotoRecord(같은 originalUri)를 만든다.
 */
class FilterEditViewModel(
    application: Application,
    savedStateHandle: SavedStateHandle,
) : AndroidViewModel(application) {

    private val app = application as AIGuideCameraApp
    private val photoDao = app.database.photoRecordDao()
    private val photoId: Long = checkNotNull(savedStateHandle[ARG_PHOTO_ID])

    private var record: PhotoRecord? = null
    private var previewSource: Bitmap? = null
    private var faceBoxes: List<RectF> = emptyList()

    private val _params = MutableStateFlow<FilterParams?>(null)
    val params: StateFlow<FilterParams?> = _params.asStateFlow()

    private val _previewBitmap = MutableStateFlow<Bitmap?>(null)
    val previewBitmap: StateFlow<Bitmap?> = _previewBitmap.asStateFlow()

    private val _thumbnails = MutableStateFlow<Map<String, Bitmap>>(emptyMap())
    val thumbnails: StateFlow<Map<String, Bitmap>> = _thumbnails.asStateFlow()

    private val _isSaving = MutableStateFlow(false)
    val isSaving: StateFlow<Boolean> = _isSaving.asStateFlow()

    /** 저장이 끝나면 true → 화면이 닫힌다. */
    private val _saved = MutableStateFlow(false)
    val saved: StateFlow<Boolean> = _saved.asStateFlow()

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    init {
        viewModelScope.launch { loadOriginal() }
        viewModelScope.launch { renderPreviewOnChange() }
        viewModelScope.launch {
            try {
                _thumbnails.value = FilterThumbnailFactory(app.offscreenRenderer).createAll()
            } catch (error: Exception) {
                Log.w(TAG, "필터 썸네일 생성 실패", error)
            }
        }
    }

    fun onFilterSelect(preset: FilterPreset) {
        _params.value = _params.value?.copy(preset = preset)
    }

    fun onIntensityChange(intensity: Float) {
        _params.value = _params.value?.copy(intensity = intensity)
    }

    fun onSkinSmoothLevelChange(level: Float) {
        _params.value = _params.value?.copy(skinSmoothLevel = level)
    }

    fun onMessageShown() {
        _message.value = null
    }

    fun onSaveClick() {
        val currentRecord = record ?: return
        val currentParams = _params.value ?: return
        val originalUri = currentRecord.originalUri ?: return
        if (_isSaving.value) return
        _isSaving.value = true
        viewModelScope.launch {
            try {
                saveNewVersion(currentRecord, Uri.parse(originalUri), currentParams)
                _saved.value = true
            } catch (error: Exception) {
                _message.value = "저장하지 못했어요 (${error.message})"
            } finally {
                _isSaving.value = false
            }
        }
    }

    private suspend fun loadOriginal() {
        try {
            val loaded = checkNotNull(photoDao.getById(photoId)) { "사진 기록이 없습니다" }
            val originalUri = checkNotNull(loaded.originalUri) { "원본이 저장되지 않은 사진입니다" }
            record = loaded
            val source = app.photoLoader.load(Uri.parse(originalUri), PREVIEW_MAX_SIDE)
            previewSource = source
            faceBoxes = detectFacesSafely(source)
            _params.value = FilterParams(
                preset = FilterCatalog.byId(loaded.filterId),
                intensity = loaded.filterIntensity,
                skinSmoothLevel = loaded.skinSmoothLevel,
            )
        } catch (error: Exception) {
            _message.value = "원본을 불러오지 못했어요 (${error.message})"
        }
    }

    /** 값이 연달아 바뀌면(슬라이더 드래그) 이전 렌더링 결과는 버리고 마지막 값만 그린다. */
    private suspend fun renderPreviewOnChange() {
        _params.filterNotNull().collectLatest { params ->
            val source = previewSource ?: return@collectLatest
            _previewBitmap.value = app.offscreenRenderer.render(source, params, faceBoxes)
        }
    }

    private suspend fun saveNewVersion(source: PhotoRecord, originalUri: Uri, params: FilterParams) {
        val createdAt = System.currentTimeMillis()
        val original = app.photoLoader.load(originalUri)
        var filtered: Bitmap? = null
        try {
            filtered = app.offscreenRenderer.render(original, params, faceBoxes)
            val saver = app.photoSaver
            val filteredUri = saver.saveJpeg(filtered, saver.filteredFileName(saver.baseNameFor(createdAt)))
            photoDao.insert(
                PhotoRecord(
                    filteredUri = filteredUri.toString(),
                    originalUri = source.originalUri,
                    filterId = params.preset.id,
                    filterIntensity = params.intensity,
                    skinSmoothLevel = params.skinSmoothLevel,
                    mode = source.mode,
                    createdAt = createdAt,
                ),
            )
        } finally {
            original.recycle()
            filtered?.recycle()
        }
    }

    private suspend fun detectFacesSafely(bitmap: Bitmap): List<RectF> = try {
        app.stillFaceDetector.detect(bitmap)
    } catch (error: Exception) {
        Log.w(TAG, "얼굴 검출 실패, 피부 보정 없이 진행합니다", error)
        emptyList()
    }

    companion object {
        const val ARG_PHOTO_ID = "photoId"
        private const val TAG = "FilterEditViewModel"

        /** 편집 미리보기용 원본 축소 크기(긴 변). 슬라이더를 움직일 때 바로 다시 그릴 수 있을 만큼 작게. */
        private const val PREVIEW_MAX_SIDE = 1600
    }
}
