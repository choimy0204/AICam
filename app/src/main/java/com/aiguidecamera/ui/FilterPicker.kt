package com.aiguidecamera.ui

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aiguidecamera.filter.FilterCatalog
import com.aiguidecamera.filter.FilterPreset
import com.aiguidecamera.ui.theme.AppColors
import kotlin.math.roundToInt

/**
 * 하단 필터 피커: 썸네일 가로 스크롤. 탭하면 즉시 선택되고,
 * 이미 선택된 필터를 다시 탭하면 [onSelectedTap](조절 패널 열고 닫기)을 부른다.
 * 카메라 화면과 사후 편집 화면이 함께 쓴다.
 */
@Composable
fun FilterPicker(
    selectedId: String,
    thumbnails: Map<String, Bitmap>,
    onSelect: (FilterPreset) -> Unit,
    onSelectedTap: () -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyRow(
        modifier = modifier,
        contentPadding = PaddingValues(horizontal = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(FilterCatalog.all, key = { it.id }) { preset ->
            val selected = preset.id == selectedId
            FilterThumbnail(
                preset = preset,
                thumbnail = thumbnails[preset.id],
                selected = selected,
                onClick = { if (selected) onSelectedTap() else onSelect(preset) },
            )
        }
    }
}

@Composable
private fun FilterThumbnail(
    preset: FilterPreset,
    thumbnail: Bitmap?,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(10.dp)
    Column(
        modifier = Modifier
            .width(THUMBNAIL_SIZE)
            .clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .size(THUMBNAIL_SIZE)
                .clip(shape)
                .background(AppColors.SurfaceVariant)
                .border(if (selected) 2.dp else 0.dp, if (selected) AppColors.Accent else Color.Transparent, shape),
        ) {
            if (thumbnail != null) {
                Image(
                    bitmap = thumbnail.asImageBitmap(),
                    contentDescription = preset.displayName,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
        Text(
            text = preset.displayName,
            color = if (selected) AppColors.Accent else AppColors.TextSecondary,
            fontSize = 11.sp,
            textAlign = TextAlign.Center,
            maxLines = 1,
            modifier = Modifier.padding(top = 4.dp),
        )
    }
}

/** 0~1 값을 조절하는 라벨 달린 슬라이더 (필터 강도, 피부 보정). */
@Composable
fun LabeledSlider(
    label: String,
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        Text(text = label, color = Color.White, fontSize = 13.sp, modifier = Modifier.width(64.dp))
        Slider(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.weight(1f),
            colors = SliderDefaults.colors(thumbColor = AppColors.Accent, activeTrackColor = AppColors.Accent),
        )
        Text(
            text = "${(value * 100).roundToInt()}%",
            color = Color.White,
            fontSize = 13.sp,
            textAlign = TextAlign.End,
            modifier = Modifier.width(44.dp),
        )
    }
}

private val THUMBNAIL_SIZE = 60.dp
