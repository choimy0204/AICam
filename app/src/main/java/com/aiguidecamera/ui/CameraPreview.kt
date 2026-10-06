package com.aiguidecamera.ui

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
import com.aiguidecamera.camera.CameraController
import com.aiguidecamera.render.GLRenderer

/**
 * GLSurfaceView를 Compose에 임베드하고 CameraController와 GLRenderer를 잇는다.
 * 화면 수명주기(onResume/onPause)를 GLSurfaceView에 전달하고, 카메라를 같은 수명주기에 바인딩한다.
 */
@Composable
fun CameraPreview(controller: CameraController, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val glView = remember {
        GLSurfaceView(context).apply {
            setEGLContextClientVersion(GLES_VERSION)
            preserveEGLContextOnPause = true
            val renderer = GLRenderer(this) { surfaceTexture -> controller.attachPreviewSurface(surfaceTexture) }
            controller.previewGeometryListener = renderer::setPreviewGeometry
            setRenderer(renderer)
            renderMode = GLSurfaceView.RENDERMODE_WHEN_DIRTY
        }
    }

    DisposableEffect(lifecycleOwner, glView) {
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

    LaunchedEffect(lifecycleOwner) {
        controller.bind(lifecycleOwner)
    }

    AndroidView(factory = { glView }, modifier = modifier)
}

private const val GLES_VERSION = 3
