package com.aiguidecamera.ui

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.aiguidecamera.AIGuideCameraApp
import com.aiguidecamera.filter.FilterCatalog
import com.aiguidecamera.storage.PhotoRecord
import com.aiguidecamera.ui.theme.AppColors

/** 사진 상세: 크게 보기 + "필터 변경" 버튼. 원본이 저장된 사진만 버튼이 켜진다. */
@Composable
fun PhotoDetailScreen(photoId: Long, onBack: () -> Unit, onEditClick: (Long) -> Unit) {
    val context = LocalContext.current
    val record by produceState<PhotoRecord?>(initialValue = null, photoId) {
        value = (context.applicationContext as AIGuideCameraApp).database.photoRecordDao().getById(photoId)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AppColors.Background)
            .systemBarsPadding(),
    ) {
        ScreenTopBar(title = record?.let { FilterCatalog.byId(it.filterId).displayName } ?: "", onBack = onBack)
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentAlignment = Alignment.Center,
        ) {
            record?.let { photo ->
                AsyncImage(
                    model = Uri.parse(photo.filteredUri),
                    contentDescription = "사진",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }

        val canEdit = record?.originalUri != null
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Button(
                onClick = { record?.let { onEditClick(it.id) } },
                enabled = canEdit,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("필터 변경")
            }
            if (record != null && !canEdit) {
                Text(
                    text = "원본이 저장된 사진만 필터를 바꿀 수 있어요",
                    color = AppColors.TextSecondary,
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
        }
    }
}
