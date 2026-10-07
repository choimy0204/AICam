package com.aiguidecamera.ui

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** 뒤로 가기 버튼 + 제목 + (선택) 오른쪽 동작을 가진 단순한 상단 바. 갤러리·상세·편집·설정 화면이 같이 쓴다. */
@Composable
fun ScreenTopBar(
    title: String,
    onBack: () -> Unit,
    actions: @Composable () -> Unit = {},
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onBack) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "뒤로", tint = Color.White)
        }
        Text(text = title, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
        actions()
    }
}
