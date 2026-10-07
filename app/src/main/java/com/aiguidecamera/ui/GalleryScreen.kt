package com.aiguidecamera.ui

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.aiguidecamera.AIGuideCameraApp
import com.aiguidecamera.storage.PhotoRecord
import com.aiguidecamera.ui.theme.AppColors

/** 앱 내 갤러리: PhotoRecord 목록을 최신순 3열 그리드로 보여준다. */
@Composable
fun GalleryScreen(onBack: () -> Unit, onPhotoClick: (Long) -> Unit) {
    val context = LocalContext.current
    val photosFlow = remember {
        (context.applicationContext as AIGuideCameraApp).database.photoRecordDao().observeAll()
    }
    val photos by photosFlow.collectAsState(initial = emptyList())

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AppColors.Background)
            .systemBarsPadding(),
    ) {
        ScreenTopBar(title = "갤러리", onBack = onBack)
        if (photos.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("아직 찍은 사진이 없어요", color = AppColors.TextSecondary)
            }
        } else {
            LazyVerticalGrid(columns = GridCells.Fixed(GRID_COLUMNS), modifier = Modifier.fillMaxSize()) {
                items(photos, key = { it.id }) { photo ->
                    GalleryCell(photo = photo, onClick = { onPhotoClick(photo.id) })
                }
            }
        }
    }
}

@Composable
private fun GalleryCell(photo: PhotoRecord, onClick: () -> Unit) {
    AsyncImage(
        model = Uri.parse(photo.filteredUri),
        contentDescription = null,
        contentScale = ContentScale.Crop,
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .padding(1.dp)
            .background(AppColors.SurfaceVariant)
            .clickable(onClick = onClick),
    )
}

private const val GRID_COLUMNS = 3
