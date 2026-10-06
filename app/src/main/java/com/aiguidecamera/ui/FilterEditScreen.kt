package com.aiguidecamera.ui

import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel

/** 사후 필터 편집: 원본 미리보기 + 필터 피커 + 강도·피부 보정 슬라이더 + 저장(새 파일). */
@Composable
fun FilterEditScreen(
    onBack: () -> Unit,
    onSaved: () -> Unit,
    viewModel: FilterEditViewModel = viewModel(),
) {
    val context = LocalContext.current
    val params by viewModel.params.collectAsStateWithLifecycle()
    val preview by viewModel.previewBitmap.collectAsStateWithLifecycle()
    val thumbnails by viewModel.thumbnails.collectAsStateWithLifecycle()
    val isSaving by viewModel.isSaving.collectAsStateWithLifecycle()
    val saved by viewModel.saved.collectAsStateWithLifecycle()
    val message by viewModel.message.collectAsStateWithLifecycle()

    LaunchedEffect(saved) {
        if (saved) {
            Toast.makeText(context, "새 사진으로 저장했어요", Toast.LENGTH_SHORT).show()
            onSaved()
        }
    }
    LaunchedEffect(message) {
        val text = message ?: return@LaunchedEffect
        Toast.makeText(context, text, Toast.LENGTH_SHORT).show()
        viewModel.onMessageShown()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .systemBarsPadding(),
    ) {
        ScreenTopBar(title = "필터 변경", onBack = onBack) {
            TextButton(onClick = viewModel::onSaveClick, enabled = params != null && !isSaving) {
                Text("저장", color = Color.White)
            }
        }
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentAlignment = Alignment.Center,
        ) {
            val bitmap = preview
            if (bitmap != null) {
                Image(
                    bitmap = bitmap.asImageBitmap(),
                    contentDescription = "편집 미리보기",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize(),
                )
            }
            if (bitmap == null || isSaving) {
                CircularProgressIndicator(color = Color.White)
            }
        }

        val current = params ?: return@Column
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
            LabeledSlider(label = "필터 강도", value = current.intensity, onValueChange = viewModel::onIntensityChange)
            LabeledSlider(
                label = "피부 보정",
                value = current.skinSmoothLevel,
                onValueChange = viewModel::onSkinSmoothLevelChange,
            )
        }
        FilterPicker(
            selectedId = current.preset.id,
            thumbnails = thumbnails,
            onSelect = viewModel::onFilterSelect,
            onSelectedTap = {},
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
        )
    }
}
