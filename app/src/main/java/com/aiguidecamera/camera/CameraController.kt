package com.aiguidecamera.camera

import android.content.Context
import android.graphics.Bitmap
import android.graphics.ImageFormat
import android.graphics.SurfaceTexture
import android.media.MediaActionSound
import android.util.Size
import android.view.Surface
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.core.SurfaceRequest
import androidx.camera.core.resolutionselector.AspectRatioStrategy
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.core.resolutionselector.ResolutionStrategy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.aiguidecamera.render.FrameRateMeter
import kotlinx.coroutines.suspendCancellableCoroutine
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * CameraX 바인딩과 촬영을 담당한다. 프리뷰는 GLRenderer가 만든 SurfaceTexture로 보내고,
 * 촬영은 고해상도 원본을 똑바로 세운 Bitmap으로 돌려준다. 프리뷰·촬영 모두 4:3.
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

    /** 메인 스레드에서만 접근. 마지막으로 바인딩한 렌즈. */
    private var lensFacing = CameraSelector.LENS_FACING_BACK

    /** 프리뷰 렌더러가 그린 프레임 수로 fps를 잰다 (성능 점검용). */
    val previewFrameRate = FrameRateMeter()

    /** 메인 스레드에서만 접근. 지금 GL 렌더러가 그리고 있는 SurfaceTexture. */
    private var currentSurfaceTexture: SurfaceTexture? = null

    /** 프리뷰 버퍼 크기·회전이 정해지면 호출된다 (width, height, rotationDegrees, mirror, 카메라 변환이 텍스처 행렬에 포함됐는지). */
    var previewGeometryListener: ((Int, Int, Int, Boolean, Boolean) -> Unit)? = null

    /** 카메라를 열지 못하면(없음·다른 앱이 사용 중 등) 예외를 던진다. */
    suspend fun bind(lifecycleOwner: LifecycleOwner, useFrontCamera: Boolean) {
        val provider = awaitCameraProvider()
        lensFacing = if (useFrontCamera) CameraSelector.LENS_FACING_FRONT else CameraSelector.LENS_FACING_BACK
        val selector = CameraSelector.Builder().requireLensFacing(lensFacing).build()
        provider.unbindAll()
        provider.bindToLifecycle(lifecycleOwner, selector, preview, imageCapture, imageAnalysis)
    }

    suspend fun hasFrontCamera(): Boolean = awaitCameraProvider().hasCamera(CameraSelector.DEFAULT_FRONT_CAMERA)

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

    private suspend fun awaitCameraProvider(): ProcessCameraProvider = suspendCancellableCoroutine { continuation ->
        val future = ProcessCameraProvider.getInstance(appContext)
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
        /** 분석 프레임 목표 크기. 포즈·얼굴 검출에 충분하고 10fps 처리에 부담이 적은 크기. */
        val ANALYSIS_TARGET_SIZE = Size(640, 480)
    }
}
