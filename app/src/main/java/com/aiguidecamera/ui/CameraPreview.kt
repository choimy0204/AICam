package com.aiguidecamera.ui

import android.graphics.RectF
import android.opengl.GLSurfaceView
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.aiguidecamera.AIGuideCameraApp
import com.aiguidecamera.camera.CameraController
import com.aiguidecamera.render.FilterParams
import com.aiguidecamera.render.GLRenderer
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.StateFlow

/**
 * GLSurfaceView를 Compose에 임베드하고 CameraController와 GLRenderer를 잇는다.
 * 화면 수명주기(onResume/onPause)를 GLSurfaceView에 전달하고, 카메라를 같은 수명주기에 바인딩한다.
 * [filterParams]가 바뀌면 즉시 프리뷰 셰이더에 반영하고, [isFrontCamera]·[extensionMode]가 바뀌면 카메라를 다시 바인딩한다.
 */
@Composable
fun CameraPreview(
    controller: CameraController,
    filterParams: FilterParams,
    faceBoxes: StateFlow<List<RectF>>,
    isFrontCamera: Boolean,
    /** androidx.camera.extensions.ExtensionMode. 바뀌면 다시 바인딩한다. */
    extensionMode: Int,
    onCameraError: (Exception) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val glView = remember { GLSurfaceView(context) }
    val renderer = remember {
        val lutLoader = (context.applicationContext as AIGuideCameraApp).lutLoader
        GLRenderer(glView, lutLoader, controller.previewFrameRate) { surfaceTexture -> controller.attachPreviewSurface(surfaceTexture) }.also {
            glView.setEGLContextClientVersion(GLES_VERSION)
            glView.preserveEGLContextOnPause = true
            glView.setRenderer(it)
            glView.renderMode = GLSurfaceView.RENDERMODE_WHEN_DIRTY
        }
    }

    DisposableEffect(lifecycleOwner, glView) {
        controller.previewGeometryListener = renderer::setPreviewGeometry
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> glView.onResume()
                Lifecycle.Event.ON_PAUSE -> glView.onPause()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            controller.previewGeometryListener = null
        }
    }

    LaunchedEffect(renderer, filterParams) {
        renderer.setFilterParams(filterParams)
    }

    // 분석 주기(10fps)로 바뀌므로 리컴포지션 없이 직접 수집해 렌더러로 넘긴다.
    LaunchedEffect(renderer, faceBoxes) {
        faceBoxes.collect { renderer.setFaceBoxes(it) }
    }

    LaunchedEffect(lifecycleOwner, isFrontCamera, extensionMode) {
        try {
            controller.bind(lifecycleOwner, isFrontCamera, extensionMode)
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            onCameraError(error)
        }
    }

    AndroidView(factory = { glView }, modifier = modifier)
}

private const val GLES_VERSION = 3
