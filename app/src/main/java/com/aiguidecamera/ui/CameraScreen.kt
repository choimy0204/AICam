package com.aiguidecamera.ui

import android.net.Uri
import android.view.OrientationEventListener
import android.view.Surface
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.aiguidecamera.camera.CameraController
import com.aiguidecamera.guide.ShootingMode

/**
 * 카메라 메인 화면: 상단 모드 토글, 4:3 프리뷰(+필터 조절 패널), 필터 피커, 셔터 버튼, 좌하단 최근 사진 썸네일.
 */
@Composable
fun CameraScreen(
    onThumbnailClick: () -> Unit,
    viewModel: CameraViewModel = viewModel(),
) {
    val context = LocalContext.current
    val latestPhoto by viewModel.latestPhoto.collectAsStateWithLifecycle()
    val isCapturing by viewModel.isCapturing.collectAsStateWithLifecycle()
    val message by viewModel.message.collectAsStateWithLifecycle()
    val mode by viewModel.mode.collectAsStateWithLifecycle()
    val filterParams by viewModel.filterParams.collectAsStateWithLifecycle()
    val thumbnails by viewModel.thumbnails.collectAsStateWithLifecycle()
    var showAdjustPanel by remember { mutableStateOf(false) }

    LaunchedEffect(message) {
        val text = message ?: return@LaunchedEffect
        Toast.makeText(context, text, Toast.LENGTH_SHORT).show()
        viewModel.onMessageShown()
    }

    CaptureRotationTracker(viewModel.cameraController)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .systemBarsPadding(),
    ) {
        Spacer(modifier = Modifier.weight(1f))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(PREVIEW_ASPECT_RATIO),
        ) {
            CameraPreview(
                controller = viewModel.cameraController,
                filterParams = filterParams,
                modifier = Modifier.fillMaxSize(),
            )
            ModeSwitch(
                mode = mode,
                onModeChange = viewModel::onModeChange,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 12.dp),
            )
            if (showAdjustPanel) {
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .background(Color.Black.copy(alpha = 0.5f))
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                ) {
                    LabeledSlider(label = "필터 강도", value = filterParams.intensity, onValueChange = viewModel::onIntensityChange)
                    if (mode == ShootingMode.PORTRAIT) {
                        LabeledSlider(
                            label = "피부 보정",
                            value = filterParams.skinSmoothLevel,
                            onValueChange = viewModel::onSkinSmoothLevelChange,
                        )
                    }
                }
            }
        }
        Spacer(modifier = Modifier.weight(1f))
        FilterPicker(
            selectedId = filterParams.preset.id,
            thumbnails = thumbnails,
            onSelect = viewModel::onFilterSelect,
            onSelectedTap = { showAdjustPanel = !showAdjustPanel },
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 32.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            LatestThumbnail(uri = latestPhoto?.filteredUri, onClick = onThumbnailClick)
            ShutterButton(enabled = !isCapturing, onClick = viewModel::onShutterClick)
            Spacer(modifier = Modifier.size(THUMBNAIL_SIZE))
        }
    }
}

@Composable
private fun LatestThumbnail(uri: String?, onClick: () -> Unit) {
    val shape = RoundedCornerShape(12.dp)
    Box(
        modifier = Modifier
            .size(THUMBNAIL_SIZE)
            .clip(shape)
            .background(Color.DarkGray)
            .border(2.dp, Color.White, shape)
            .clickable(enabled = uri != null, onClick = onClick),
    ) {
        if (uri != null) {
            AsyncImage(
                model = Uri.parse(uri),
                contentDescription = "최근 사진",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

@Composable
private fun ShutterButton(enabled: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(76.dp)
            .border(4.dp, Color.White, CircleShape)
            .padding(8.dp)
            .clip(CircleShape)
            .background(if (enabled) Color.White else Color.Gray)
            .clickable(enabled = enabled, onClick = onClick),
    )
}

/** 화면은 세로 고정이므로, 폰을 가로로 돌려 잡으면 촬영본 방향만 따로 맞춘다. */
@Composable
private fun CaptureRotationTracker(controller: CameraController) {
    val context = LocalContext.current
    DisposableEffect(controller) {
        val listener = object : OrientationEventListener(context) {
            override fun onOrientationChanged(orientation: Int) {
                if (orientation == ORIENTATION_UNKNOWN) return
                val rotation = when (orientation) {
                    in 45 until 135 -> Surface.ROTATION_270
                    in 135 until 225 -> Surface.ROTATION_180
                    in 225 until 315 -> Surface.ROTATION_90
                    else -> Surface.ROTATION_0
                }
                controller.setCaptureRotation(rotation)
            }
        }
        listener.enable()
        onDispose { listener.disable() }
    }
}

/** 프리뷰 영역 가로:세로 = 3:4 (4:3 센서를 세로로 본 비율). */
private const val PREVIEW_ASPECT_RATIO = 3f / 4f
private val THUMBNAIL_SIZE = 56.dp
