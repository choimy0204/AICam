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
import com.aiguidecamera.analysis.StillFace
import androidx.camera.extensions.ExtensionMode
import com.aiguidecamera.camera.AutoExposureTuner
import com.aiguidecamera.camera.CameraController
import com.aiguidecamera.camera.CapturedJpeg
import com.aiguidecamera.camera.FaceMeteringPolicy
import com.aiguidecamera.camera.HorizonStraightener
import com.aiguidecamera.camera.StraightenMath
import com.aiguidecamera.camera.JpegDecoder
import com.aiguidecamera.capture.AutoCaptureState
import com.aiguidecamera.capture.AutoCaptureStateMachine
import com.aiguidecamera.capture.BestShotSelector
import com.aiguidecamera.capture.CaptureReadiness
import com.aiguidecamera.capture.Readiness
import com.aiguidecamera.capture.SharpnessMeter
import com.aiguidecamera.capture.ShotScore
import com.aiguidecamera.filter.FilterPreset
import com.aiguidecamera.filter.FilterThumbnailFactory
import com.aiguidecamera.guide.Advice
import com.aiguidecamera.guide.FoodAngle
import com.aiguidecamera.guide.GuideConstants
import com.aiguidecamera.guide.IssueStabilizer
import com.aiguidecamera.guide.RuleEngine
import com.aiguidecamera.guide.ShootingMode
import com.aiguidecamera.render.FilterParams
import com.aiguidecamera.storage.PhotoRecord
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.RejectedExecutionException

/**
 * 카메라 화면 상태(모드·필터·강도·조언·자동 촬영·촬영 보정)와 "촬영 → 얼굴 검출 → 오프스크린 렌더 → 저장 → PhotoRecord 기록" 흐름을 관리한다.
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

    private val _isFrontCamera = MutableStateFlow(false)
    val isFrontCamera: StateFlow<Boolean> = _isFrontCamera.asStateFlow()

    /** 전면 카메라가 있는 기기에서만 전환 버튼을 보여준다. */
    private val _canSwitchCamera = MutableStateFlow(false)
    val canSwitchCamera: StateFlow<Boolean> = _canSwitchCamera.asStateFlow()

    /** 모드별 기기 촬영 모드(ExtensionMode). 지원 여부는 CameraController가 확인한다. */
    private val _cameraExtension = MutableStateFlow(ExtensionMode.NONE)
    val cameraExtension: StateFlow<Int> = _cameraExtension.asStateFlow()

    /** 인물 얼굴 측정·풍경 자동 노출. 분석 스레드에서만 쓴다. */
    private val faceMetering = FaceMeteringPolicy()
    private val exposureTuner = AutoExposureTuner()

    private val _foodAngle = MutableStateFlow(FoodAngle.TOP_VIEW)
    val foodAngle: StateFlow<FoodAngle> = _foodAngle.asStateFlow()

    /** 안정화를 거친 조언 1개. 아래 엔진·안정화기는 분석 스레드에서만 쓴다. */
    private val _advice = MutableStateFlow<Advice>(Advice.Pending)
    val advice: StateFlow<Advice> = _advice.asStateFlow()
    private val ruleEngine = RuleEngine()
    private val issueStabilizer = IssueStabilizer()

    /** 자동 촬영. 기본값 OFF. 상태 머신은 분석 스레드에서만 쓴다. */
    private val _autoCaptureEnabled = MutableStateFlow(false)
    val autoCaptureEnabled: StateFlow<Boolean> = _autoCaptureEnabled.asStateFlow()
    private val _autoCaptureState = MutableStateFlow(AutoCaptureState.AIMING)
    val autoCaptureState: StateFlow<AutoCaptureState> = _autoCaptureState.asStateFlow()
    private val _readiness = MutableStateFlow(Readiness.COMPOSITION_NOT_READY)
    val readiness: StateFlow<Readiness> = _readiness.asStateFlow()
    private val autoCapture = AutoCaptureStateMachine()

    private val frameAnalyzer = FrameAnalyzer(sensorReader, cameraController.analysisExecutor) { result ->
        _analysis.value = result
        val issues = ruleEngine.evaluate(result, _mode.value, _foodAngle.value)
        val advice = issueStabilizer.update(issues)
        _advice.value = advice
        if (_autoCaptureEnabled.value) updateAutoCapture(advice, result)
        updateCameraAssist(result)
        if (result.faces.isNotEmpty() || _faceBoxes.value.isNotEmpty()) {
            _faceBoxes.value = result.faces.map { RectF(it.box.left, it.box.top, it.box.right, it.box.bottom) }
        }
    }

    init {
        cameraController.setAnalyzer(frameAnalyzer)
        viewModelScope.launch {
            _canSwitchCamera.value = try {
                cameraController.hasFrontCamera()
            } catch (error: Exception) {
                Log.w(TAG, "전면 카메라 확인 실패", error)
                false
            }
        }
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
        cameraController.setExposureEv(if (newMode == ShootingMode.NIGHT) GuideConstants.NIGHT_EXPOSURE_EV else 0f)
        _cameraExtension.value = extensionFor(newMode)
        _filterParams.value = paramsFor(newMode)
        resetAdvice()
    }

    fun onFoodAngleChange(angle: FoodAngle) {
        if (angle == _foodAngle.value) return
        _foodAngle.value = angle
        resetAdvice()
    }

    /** 촬영 중에는 전환하지 않는다. 실제 재바인딩은 CameraPreview가 [isFrontCamera]를 보고 한다. */
    fun onSwitchCamera() {
        if (_isCapturing.value) return
        setFrontCamera(!_isFrontCamera.value)
    }

    /** 카메라를 열지 못했을 때. 전면이었다면 후면으로 되돌린다. */
    fun onCameraError(error: Exception) {
        Log.e(TAG, "카메라 바인딩 실패", error)
        if (_isFrontCamera.value) {
            _message.value = "전면 카메라를 열 수 없어 후면 카메라로 돌아갈게요"
            setFrontCamera(false)
        } else {
            _message.value = "카메라를 열 수 없어요. 다른 앱이 카메라를 쓰고 있는지 확인해 주세요"
        }
    }

    private fun setFrontCamera(front: Boolean) {
        _isFrontCamera.value = front
        frameAnalyzer.isFrontCamera = front
        _faceBoxes.value = emptyList()
        resetAdvice()
    }

    fun onAutoCaptureToggle() {
        _autoCaptureEnabled.value = !_autoCaptureEnabled.value
        runOnAnalysisThread { resetAutoCapture() }
    }

    /** 규칙 묶음이 바뀌었으니 이전 프레임 기록을 버린다. 안정화기·상태 머신은 분석 스레드에서 초기화한다. */
    private fun resetAdvice() {
        _advice.value = Advice.Pending
        runOnAnalysisThread {
            issueStabilizer.reset()
            resetAutoCapture()
            faceMetering.reset()
            exposureTuner.reset()
            cameraController.cancelFaceMetering()
        }
    }

    /**
     * 분석 스레드 전용. 인물은 얼굴에 초점·노출을 맞추고, 풍경은 하늘이 날아가지 않게 노출을 조절한다.
     * 노출은 메인 스레드에서 바꾸며, 그 사이 모드가 바뀌었으면 버린다.
     */
    private fun updateCameraAssist(result: FrameAnalysisResult) {
        when (_mode.value) {
            ShootingMode.PORTRAIT -> {
                val face = result.primaryFace?.box
                when (faceMetering.update(face, result.timestampMs)) {
                    FaceMeteringPolicy.Action.METER -> if (face != null) cameraController.meterOnFace(face)
                    FaceMeteringPolicy.Action.CANCEL -> cameraController.cancelFaceMetering()
                    FaceMeteringPolicy.Action.NONE -> Unit
                }
            }
            ShootingMode.LANDSCAPE -> if (exposureTuner.update(result.highlightClipRatio, result.timestampMs)) {
                val ev = exposureTuner.ev
                viewModelScope.launch {
                    if (_mode.value == ShootingMode.LANDSCAPE) cameraController.setExposureEv(ev)
                }
            }
            else -> Unit
        }
    }

    /** 야경은 여러 장을 합쳐 밝히는 기기 야간 모드, 풍경은 역광에 강한 HDR. 기기가 지원할 때만 실제로 켜진다. */
    private fun extensionFor(mode: ShootingMode): Int = when (mode) {
        ShootingMode.NIGHT -> ExtensionMode.NIGHT
        ShootingMode.LANDSCAPE -> ExtensionMode.HDR
        else -> ExtensionMode.NONE
    }

    /** 분석 스레드 전용. */
    private fun resetAutoCapture() {
        autoCapture.reset()
        _autoCaptureState.value = autoCapture.state
        _readiness.value = Readiness.COMPOSITION_NOT_READY
    }

    /** 분석 스레드 전용. 조건이 700ms 유지되면 상태 머신이 촬영을 지시한다. */
    private fun updateAutoCapture(advice: Advice, result: FrameAnalysisResult) {
        val readiness = CaptureReadiness.evaluate(advice, result, _mode.value)
        _readiness.value = readiness
        val shouldCapture = autoCapture.onFrame(readiness == Readiness.READY, result)
        _autoCaptureState.value = autoCapture.state
        if (shouldCapture) viewModelScope.launch { runAutoCapture() }
    }

    private suspend fun runAutoCapture() {
        // 수동 촬영이 진행 중이면 이번 기회는 건너뛰고 쿨다운으로 넘긴다.
        if (_isCapturing.compareAndSet(expect = false, update = true)) {
            try {
                burstCaptureAndSave()
            } catch (error: Exception) {
                _message.value = "사진을 저장하지 못했어요 (${error.message})"
            } finally {
                _isCapturing.value = false
            }
        }
        runOnAnalysisThread {
            autoCapture.onCaptureFinished()
            _autoCaptureState.value = autoCapture.state
        }
    }

    /** ViewModel 정리 뒤 실행기가 닫혀 있으면 조용히 버린다. */
    private fun runOnAnalysisThread(block: () -> Unit) {
        try {
            cameraController.analysisExecutor.execute(block)
        } catch (_: RejectedExecutionException) {
            // 화면이 닫히는 중
        }
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
        if (!_isCapturing.compareAndSet(expect = false, update = true)) return
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
        val straightenDeg = straightenAngleFor(shotMode)
        val original = straightenIfNeeded(cameraController.takePicture(), straightenDeg)
        val faceBoxes = if (params.skinSmoothLevel > 0f) detectFacesSafely(original) else emptyList()
        renderAndSave(original, faceBoxes, params, shotMode, createdAt)
    }

    /**
     * 셔터음 한 번에 [GuideConstants.BURST_COUNT]장을 연달아 찍고, 선명도 + 눈 뜸 점수로 1장을 골라 저장한다.
     * 채점은 줄인 사본으로 하고, 고른 한 장만 원본 크기로 디코딩한다.
     */
    private suspend fun burstCaptureAndSave() {
        val createdAt = System.currentTimeMillis()
        val params = _filterParams.value
        val shotMode = _mode.value
        val straightenDeg = straightenAngleFor(shotMode)
        // 기기 야간·HDR 모드는 한 장에 여러 프레임을 합쳐 오래 걸리므로 한 장만 찍는다.
        val shotCount = if (cameraController.status.value.extensionMode == ExtensionMode.NONE) GuideConstants.BURST_COUNT else 1
        val shots = ArrayList<CapturedJpeg>(shotCount)
        repeat(shotCount) { index ->
            shots += cameraController.takeJpeg(playSound = index == 0)
        }

        val scores = ArrayList<ShotScore>(shots.size)
        val faceBoxesPerShot = ArrayList<List<RectF>>(shots.size)
        for (shot in shots) {
            val preview = withContext(Dispatchers.Default) {
                JpegDecoder.decodeUpright(shot, GuideConstants.BEST_SHOT_ANALYSIS_SIDE_PX)
            }
            try {
                val sharpness = withContext(Dispatchers.Default) { SharpnessMeter.measure(preview) }
                val faces = if (shotMode == ShootingMode.PORTRAIT) detectStillFacesSafely(preview) else emptyList()
                scores += ShotScore(sharpness, faces.mapNotNull { it.eyesOpen }.minOrNull())
                faceBoxesPerShot += faces.map { it.box }
            } finally {
                preview.recycle()
            }
        }

        val best = BestShotSelector.bestIndex(scores)
        Log.d(TAG, "연사 채점 $scores -> ${best}번")
        val original = straightenIfNeeded(withContext(Dispatchers.Default) { JpegDecoder.decodeUpright(shots[best]) }, straightenDeg)
        val faceBoxes = if (params.skinSmoothLevel > 0f) faceBoxesPerShot[best] else emptyList()
        renderAndSave(original, faceBoxes, params, shotMode, createdAt)
    }

    /**
     * 찍는 순간 보정할 기울기. 수평이 중요한 풍경·야경의 후면 촬영만 보정한다 (얼굴 박스를 쓰는 인물은 건드리지 않는다).
     * 보정하지 않으면 null.
     */
    private fun straightenAngleFor(mode: ShootingMode): Float? {
        if (!app.settings.autoStraighten.value || _isFrontCamera.value) return null
        if (mode != ShootingMode.LANDSCAPE && mode != ShootingMode.NIGHT) return null
        val roll = _analysis.value?.rollDeg ?: return null
        return roll.takeIf { StraightenMath.shouldStraighten(it) }
    }

    /** [rollDeg]가 있으면 돌려서 잘라낸 새 Bitmap을 돌려주고 입력은 해제한다. */
    private suspend fun straightenIfNeeded(photo: Bitmap, rollDeg: Float?): Bitmap {
        if (rollDeg == null) return photo
        return withContext(Dispatchers.Default) { HorizonStraightener.straighten(photo, rollDeg) }
    }

    /** 원본에 필터를 입혀 저장하고 기록을 남긴다. [original]은 여기서 해제한다. */
    private suspend fun renderAndSave(
        original: Bitmap,
        faceBoxes: List<RectF>,
        params: FilterParams,
        shotMode: ShootingMode,
        createdAt: Long,
    ) {
        var filtered: Bitmap? = null
        try {
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
    private suspend fun detectFacesSafely(bitmap: Bitmap): List<RectF> = detectStillFacesSafely(bitmap).map { it.box }

    private suspend fun detectStillFacesSafely(bitmap: Bitmap): List<StillFace> = try {
        app.stillFaceDetector.detectFaces(bitmap)
    } catch (error: Exception) {
        Log.w(TAG, "얼굴 검출 실패, 피부 보정·눈 뜸 점수 없이 진행합니다", error)
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
