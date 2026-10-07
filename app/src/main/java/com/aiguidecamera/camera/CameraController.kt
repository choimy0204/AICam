package com.aiguidecamera.camera

import android.content.Context
import android.graphics.Bitmap
import android.graphics.ImageFormat
import android.graphics.SurfaceTexture
import android.media.MediaActionSound
import android.util.Size
import android.view.Surface
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.FocusMeteringAction
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.core.SurfaceOrientedMeteringPointFactory
import androidx.camera.core.SurfaceRequest
import androidx.camera.extensions.ExtensionMode
import androidx.camera.extensions.ExtensionsManager
import androidx.camera.core.resolutionselector.AspectRatioStrategy
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.core.resolutionselector.ResolutionStrategy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.aiguidecamera.analysis.NormRect
import com.aiguidecamera.render.FrameRateMeter
import com.google.common.util.concurrent.ListenableFuture
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.math.roundToInt

/**
 * CameraX 바인딩과 촬영을 담당한다. 프리뷰는 GLRenderer가 만든 SurfaceTexture로 보내고,
 * 촬영은 고해상도 원본을 똑바로 세운 Bitmap으로 돌려준다. 프리뷰·촬영 모두 4:3.
 * 노출 보정, 얼굴 초점·노출 측정, 기기 야간·HDR 모드(지원할 때만)도 여기서 건다.
 */
class CameraController(context: Context) {

    private val appContext = context.applicationContext
    private val mainExecutor = ContextCompat.getMainExecutor(appContext)
    private val captureExecutor = Executors.newSingleThreadExecutor()
    private val shutterSound = MediaActionSound().apply { load(MediaActionSound.SHUTTER_CLICK) }

    private val resolutionSelector = ResolutionSelector.Builder()
        .setAspectRatioStrategy(AspectRatioStrategy.RATIO_4_3_FALLBACK_AUTO_STRATEGY)
        .build()

    private val preview = Preview.Builder()
        .setResolutionSelector(resolutionSelector)
        .build()

    private val imageCapture = ImageCapture.Builder()
        .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
        .setResolutionSelector(resolutionSelector)
        .build()

    /** 실시간 분석용 저해상도 프레임. 늦으면 최신 프레임만 남긴다. */
    private val imageAnalysis = ImageAnalysis.Builder()
        .setResolutionSelector(
            ResolutionSelector.Builder()
                .setAspectRatioStrategy(AspectRatioStrategy.RATIO_4_3_FALLBACK_AUTO_STRATEGY)
                .setResolutionStrategy(
                    ResolutionStrategy(ANALYSIS_TARGET_SIZE, ResolutionStrategy.FALLBACK_RULE_CLOSEST_HIGHER_THEN_LOWER),
                )
                .build(),
        )
        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
        .build()

    /** 분석기와 ML Kit 완료 콜백이 함께 쓰는 단일 스레드. */
    val analysisExecutor: ExecutorService = Executors.newSingleThreadExecutor()

    /** 마지막으로 바인딩한 카메라. 메인 스레드에서 쓰고, 얼굴 측정은 분석 스레드에서 읽는다. */
    @Volatile private var camera: Camera? = null

    /** 메인 스레드에서만 접근. 처음 기기 모드를 요청할 때 만든다. */
    private var extensionsManager: ExtensionsManager? = null

    private val _status = MutableStateFlow(CameraStatus())
    val status: StateFlow<CameraStatus> = _status.asStateFlow()

    /** 메인 스레드에서만 접근. 카메라를 다시 바인딩해도 유지할 노출 보정값(EV). */
    private var exposureEv = 0f

    /** 메인 스레드에서만 접근. 마지막으로 바인딩한 렌즈. */
    private var lensFacing = CameraSelector.LENS_FACING_BACK

    /** 프리뷰 렌더러가 그린 프레임 수로 fps를 잰다 (성능 점검용). */
    val previewFrameRate = FrameRateMeter()

    /** 메인 스레드에서만 접근. 지금 GL 렌더러가 그리고 있는 SurfaceTexture. */
    private var currentSurfaceTexture: SurfaceTexture? = null

    /** 프리뷰 버퍼 크기·회전이 정해지면 호출된다 (width, height, rotationDegrees, mirror, 카메라 변환이 텍스처 행렬에 포함됐는지). */
    var previewGeometryListener: ((Int, Int, Int, Boolean, Boolean) -> Unit)? = null

    /**
     * 카메라를 열지 못하면(없음·다른 앱이 사용 중 등) 예외를 던진다.
     * [extensionMode](ExtensionMode)는 기기가 지원하고 분석 프레임도 함께 받을 수 있을 때만 쓰고, 아니면 일반 카메라로 연다.
     */
    suspend fun bind(lifecycleOwner: LifecycleOwner, useFrontCamera: Boolean, extensionMode: Int = ExtensionMode.NONE) {
        val provider = ProcessCameraProvider.getInstance(appContext).await()
        lensFacing = if (useFrontCamera) CameraSelector.LENS_FACING_FRONT else CameraSelector.LENS_FACING_BACK
        val baseSelector = CameraSelector.Builder().requireLensFacing(lensFacing).build()
        val extensionSelector = extensionSelectorOrNull(provider, baseSelector, extensionMode)
        provider.unbindAll()
        var activeExtension = ExtensionMode.NONE
        camera = if (extensionSelector != null) {
            try {
                provider.bindToLifecycle(lifecycleOwner, extensionSelector, preview, imageCapture, imageAnalysis)
                    .also { activeExtension = extensionMode }
            } catch (error: IllegalArgumentException) {
                Log.w(TAG, "기기 촬영 모드 바인딩 실패, 일반 카메라로 엽니다", error)
                provider.unbindAll()
                provider.bindToLifecycle(lifecycleOwner, baseSelector, preview, imageCapture, imageAnalysis)
            }
        } else {
            provider.bindToLifecycle(lifecycleOwner, baseSelector, preview, imageCapture, imageAnalysis)
        }
        _status.value = _status.value.copy(extensionMode = activeExtension)
        applyExposure()
    }

    /** 기기 야간·HDR 모드를 쓸 수 있으면 그 선택자를, 아니면 null. 확인 중 오류도 "지원 안 함"으로 본다. */
    private suspend fun extensionSelectorOrNull(
        provider: ProcessCameraProvider,
        baseSelector: CameraSelector,
        extensionMode: Int,
    ): CameraSelector? {
        if (extensionMode == ExtensionMode.NONE) return null
        return try {
            val manager = extensionsManager
                ?: ExtensionsManager.getInstanceAsync(appContext, provider).await().also { extensionsManager = it }
            val usable = manager.isExtensionAvailable(baseSelector, extensionMode) &&
                manager.isImageAnalysisSupported(baseSelector, extensionMode)
            if (usable) manager.getExtensionEnabledCameraSelector(baseSelector, extensionMode) else null
        } catch (error: Exception) {
            Log.w(TAG, "기기 촬영 모드 확인 실패", error)
            null
        }
    }

    /**
     * 분석 스레드에서 호출 가능. 얼굴([face], 똑바로 선 사진 기준 0~1)에 초점과 노출 측정을 건다.
     * 얼굴이 사라져 [cancelFaceMetering]을 부를 때까지 유지한다.
     */
    fun meterOnFace(face: NormRect) {
        val current = camera ?: return
        val rotation = current.cameraInfo.getSensorRotationDegrees(imageAnalysis.targetRotation)
        val uprightX = (face.left + face.right) / 2f
        val uprightY = (face.top + face.bottom) / 2f
        // 분석 버퍼를 rotation만큼 시계 방향으로 돌린 것이 똑바로 선 사진이므로, 거꾸로 돌려 버퍼 좌표로 바꾼다.
        val (bufferX, bufferY) = when (rotation) {
            90 -> uprightY to 1f - uprightX
            180 -> 1f - uprightX to 1f - uprightY
            270 -> 1f - uprightY to uprightX
            else -> uprightX to uprightY
        }
        val point = SurfaceOrientedMeteringPointFactory(1f, 1f, imageAnalysis)
            .createPoint(bufferX, bufferY, maxOf(face.width, face.height))
        val action = FocusMeteringAction.Builder(point, FocusMeteringAction.FLAG_AF or FocusMeteringAction.FLAG_AE)
            .disableAutoCancel()
            .build()
        current.cameraControl.startFocusAndMetering(action)
    }

    /** 분석 스레드에서 호출 가능. 얼굴 측정을 풀고 카메라 자동 초점·노출로 돌아간다. */
    fun cancelFaceMetering() {
        camera?.cameraControl?.cancelFocusAndMetering()
    }

    /** 노출 보정을 [ev]에 가장 가까운 단계로 맞춘다. 지원하지 않는 기기에서는 아무것도 하지 않는다. */
    fun setExposureEv(ev: Float) {
        exposureEv = ev
        _status.value = _status.value.copy(exposureEv = ev)
        applyExposure()
    }

    private fun applyExposure() {
        val current = camera ?: return
        val state = current.cameraInfo.exposureState
        if (!state.isExposureCompensationSupported) return
        val range = state.exposureCompensationRange
        val index = (exposureEv / state.exposureCompensationStep.toFloat()).roundToInt().coerceIn(range.lower, range.upper)
        current.cameraControl.setExposureCompensationIndex(index)
    }

    suspend fun hasFrontCamera(): Boolean = ProcessCameraProvider.getInstance(appContext).await().hasCamera(CameraSelector.DEFAULT_FRONT_CAMERA)

    fun setAnalyzer(analyzer: ImageAnalysis.Analyzer) {
        imageAnalysis.setAnalyzer(analysisExecutor, analyzer)
    }

    fun clearAnalyzer() {
        imageAnalysis.clearAnalyzer()
    }

    /**
     * GL 렌더러가 새 SurfaceTexture를 만들 때마다 호출한다(메인 스레드).
     * Surface provider를 다시 걸어서 CameraX가 새 Surface를 요청하게 한다.
     */
    fun attachPreviewSurface(surfaceTexture: SurfaceTexture) {
        currentSurfaceTexture = surfaceTexture
        preview.setSurfaceProvider(mainExecutor, ::onSurfaceRequested)
    }

    /** 폰을 돌려 잡은 방향(Surface.ROTATION_*)을 촬영본에 반영한다. 화면은 세로 고정이다. */
    fun setCaptureRotation(rotation: Int) {
        imageCapture.targetRotation = rotation
    }

    /**
     * 연사용: 디코딩하지 않은 JPEG를 돌려준다. 여러 장을 빠르게 찍고, 채점 후 고른 한 장만 크게 디코딩한다.
     * 고해상도 Bitmap 여러 장을 동시에 들고 있지 않기 위해서다.
     */
    suspend fun takeJpeg(playSound: Boolean): CapturedJpeg = suspendCancellableCoroutine { continuation ->
        if (playSound) shutterSound.play(MediaActionSound.SHUTTER_CLICK)
        imageCapture.takePicture(captureExecutor, object : ImageCapture.OnImageCapturedCallback() {
            override fun onCaptureSuccess(image: ImageProxy) {
                val result = runCatching { image.use { toCapturedJpeg(it) } }
                result.onSuccess { continuation.resume(it) }
                result.onFailure { continuation.resumeWithException(it) }
            }

            override fun onError(exception: ImageCaptureException) {
                continuation.resumeWithException(exception)
            }
        })
    }

    /** 셔터음을 내고 한 장 찍어 똑바로 세운 원본 Bitmap을 돌려준다. */
    suspend fun takePicture(): Bitmap = suspendCancellableCoroutine { continuation ->
        shutterSound.play(MediaActionSound.SHUTTER_CLICK)
        imageCapture.takePicture(captureExecutor, object : ImageCapture.OnImageCapturedCallback() {
            override fun onCaptureSuccess(image: ImageProxy) {
                val result = runCatching { image.use { toUprightBitmap(it) } }
                result.onSuccess { continuation.resume(it) }
                result.onFailure { continuation.resumeWithException(it) }
            }

            override fun onError(exception: ImageCaptureException) {
                continuation.resumeWithException(exception)
            }
        })
    }

    private suspend fun <T> ListenableFuture<T>.await(): T = suspendCancellableCoroutine { continuation ->
        val future = this
        future.addListener({
            val result = runCatching { future.get() }
            result.onSuccess { continuation.resume(it) }
            result.onFailure { continuation.resumeWithException(it) }
        }, mainExecutor)
    }

    private fun onSurfaceRequested(request: SurfaceRequest) {
        val surfaceTexture = currentSurfaceTexture
        if (surfaceTexture == null) {
            request.willNotProvideSurface()
            return
        }
        val resolution = request.resolution
        surfaceTexture.setDefaultBufferSize(resolution.width, resolution.height)
        val surface = Surface(surfaceTexture)
        val isFrontCamera = lensFacing == CameraSelector.LENS_FACING_FRONT

        request.setTransformationInfoListener(mainExecutor) { info ->
            previewGeometryListener?.invoke(
                resolution.width, resolution.height, info.rotationDegrees, isFrontCamera, info.hasCameraTransform(),
            )
        }
        request.provideSurface(surface, mainExecutor) {
            surface.release()
            // GL 컨텍스트가 바뀌어 더 이상 쓰지 않는 SurfaceTexture면 여기서 정리한다.
            if (surfaceTexture !== currentSurfaceTexture) surfaceTexture.release()
        }
    }

    private fun toUprightBitmap(image: ImageProxy): Bitmap =
        JpegDecoder.rotateUpright(image.toBitmap(), image.imageInfo.rotationDegrees)

    private fun toCapturedJpeg(image: ImageProxy): CapturedJpeg {
        check(image.format == ImageFormat.JPEG) { "JPEG가 아닌 촬영 결과: ${image.format}" }
        val buffer = image.planes[0].buffer
        val bytes = ByteArray(buffer.remaining())
        buffer.get(bytes)
        return CapturedJpeg(bytes, image.imageInfo.rotationDegrees)
    }

    private companion object {
        const val TAG = "CameraController"

        /** 분석 프레임 목표 크기. 포즈·얼굴 검출에 충분하고 10fps 처리에 부담이 적은 크기. */
        val ANALYSIS_TARGET_SIZE = Size(640, 480)
    }
}
