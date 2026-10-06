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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.aiguidecamera.analysis.SensorReader
import com.aiguidecamera.camera.CameraController
import com.aiguidecamera.capture.AutoCaptureState
import com.aiguidecamera.capture.Readiness
import com.aiguidecamera.guide.Advice
import com.aiguidecamera.guide.ShootingMode
import com.aiguidecamera.guide.rules.HorizonRule

/**
 * 카메라 메인 화면: 상단 모드 토글, 자동 촬영(AUTO) 토글, 4:3 프리뷰(+필터 조절 패널), 필터 피커, 셔터 버튼, 좌하단 최근 사진 썸네일.
 */
@Composable
fun CameraScreen(
    onThumbnailClick: () -> Unit,
    onSettingsClick: () -> Unit,
    viewModel: CameraViewModel = viewModel(),
) {
    val context = LocalContext.current
    val latestPhoto by viewModel.latestPhoto.collectAsStateWithLifecycle()
    val isCapturing by viewModel.isCapturing.collectAsStateWithLifecycle()
    val message by viewModel.message.collectAsStateWithLifecycle()
    val mode by viewModel.mode.collectAsStateWithLifecycle()
    val filterParams by viewModel.filterParams.collectAsStateWithLifecycle()
    val thumbnails by viewModel.thumbnails.collectAsStateWithLifecycle()
    val advice by viewModel.advice.collectAsStateWithLifecycle()
    val foodAngle by viewModel.foodAngle.collectAsStateWithLifecycle()
    val autoCaptureEnabled by viewModel.autoCaptureEnabled.collectAsStateWithLifecycle()
    val autoCaptureState by viewModel.autoCaptureState.collectAsStateWithLifecycle()
    val readiness by viewModel.readiness.collectAsStateWithLifecycle()
    val autoCaptureMessage = if (autoCaptureEnabled) autoCaptureMessage(autoCaptureState, readiness) else null
    val isHorizonIssue = (advice as? Advice.Problem)?.issue?.ruleId == HorizonRule.ID
    var showAdjustPanel by remember { mutableStateOf(false) }
    var showDebug by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(message) {
        val text = message ?: return@LaunchedEffect
        Toast.makeText(context, text, Toast.LENGTH_SHORT).show()
        viewModel.onMessageShown()
    }

    CaptureRotationTracker(viewModel.cameraController)
    SensorLifecycle(viewModel.sensorReader)

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
                .aspectRatio(PREVIEW_ASPECT_RATIO)
                .border(
                    width = 3.dp,
                    color = if (advice == Advice.Good) GOOD_GREEN else Color.Transparent,
                ),
        ) {
            CameraPreview(
                controller = viewModel.cameraController,
                filterParams = filterParams,
                faceBoxes = viewModel.faceBoxes,
                modifier = Modifier.fillMaxSize(),
            )
            if (isHorizonIssue) {
                HorizonLine(analysis = viewModel.analysis, modifier = Modifier.fillMaxSize())
            }
            if (showDebug) {
                DebugOverlay(
                    analysis = viewModel.analysis,
                    isMirrored = viewModel.cameraController.isFrontCamera,
                    modifier = Modifier.fillMaxSize(),
                )
            }
            ToggleChip(
                text = "DBG",
                checked = showDebug,
                onClick = { showDebug = !showDebug },
                modifier = Modifier.align(Alignment.TopStart),
            )
            ToggleChip(
                text = "AUTO",
                checked = autoCaptureEnabled,
                onClick = viewModel::onAutoCaptureToggle,
                modifier = Modifier.align(Alignment.TopEnd),
            )
            Column(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                ModeSwitch(mode = mode, onModeChange = viewModel::onModeChange)
                if (mode == ShootingMode.FOOD) {
                    FoodAngleSwitch(
                        angle = foodAngle,
                        onAngleChange = viewModel::onFoodAngleChange,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                }
            }
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                AdviceBanner(
                    advice = advice,
                    autoCaptureMessage = autoCaptureMessage,
                    modifier = Modifier.padding(bottom = 12.dp),
                )
                if (showAdjustPanel) {
                    Column(
                        modifier = Modifier
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
            Box(modifier = Modifier.size(THUMBNAIL_SIZE), contentAlignment = Alignment.Center) {
                IconButton(onClick = onSettingsClick) {
                    Icon(Icons.Filled.Settings, contentDescription = "설정", tint = Color.White)
                }
            }
        }
    }
}

/** 프리뷰 모서리의 작은 켜기/끄기 글자 버튼 (DBG, AUTO). 켜지면 노란색. */
@Composable
private fun ToggleChip(text: String, checked: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Text(
        text = text,
        color = if (checked) Color.Yellow else Color.White.copy(alpha = 0.6f),
        fontSize = 12.sp,
        modifier = modifier
            .padding(12.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(Color.Black.copy(alpha = 0.4f))
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 4.dp),
    )
}

/** 자동 촬영이 켜져 있고 구도가 맞았을 때 배너에 띄울 문구. null이면 기본 "좋아요" 문구. */
private fun autoCaptureMessage(state: AutoCaptureState, readiness: Readiness): String? = when (state) {
    AutoCaptureState.AIMING -> readiness.waitingMessage
    AutoCaptureState.STABILIZING -> "그대로 멈춰 주세요…"
    AutoCaptureState.CAPTURING -> "촬영 중…"
    AutoCaptureState.COOLDOWN -> "찍었어요! 구도를 바꾸면 다시 찍어요"
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

/** 화면이 보일 때만 센서를 켠다 (배터리). */
@Composable
private fun SensorLifecycle(sensorReader: SensorReader) {
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, sensorReader) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> sensorReader.start()
                Lifecycle.Event.ON_PAUSE -> sensorReader.stop()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            sensorReader.stop()
        }
    }
}

/** 프리뷰 영역 가로:세로 = 3:4 (4:3 센서를 세로로 본 비율). */
private const val PREVIEW_ASPECT_RATIO = 3f / 4f
private val THUMBNAIL_SIZE = 56.dp
