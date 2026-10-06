package com.aiguidecamera.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aiguidecamera.AIGuideCameraApp

/** 설정 화면: "원본도 함께 저장" 스위치와 앱 버전. */
@Composable
fun SettingsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val settings = (context.applicationContext as AIGuideCameraApp).settings
    val saveOriginalToo by settings.saveOriginalToo.collectAsStateWithLifecycle()
    val versionName = remember { context.packageManager.getPackageInfo(context.packageName, 0).versionName.orEmpty() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .systemBarsPadding(),
    ) {
        ScreenTopBar(title = "설정", onBack = onBack)
        SettingSwitchRow(
            title = "원본도 함께 저장",
            description = "켜면 필터 없는 원본도 같은 폴더에 저장돼요. 원본이 있어야 나중에 필터를 바꿀 수 있어요.",
            checked = saveOriginalToo,
            onCheckedChange = settings::setSaveOriginalToo,
        )
        Text(
            text = "버전 $versionName",
            color = Color.Gray,
            fontSize = 13.sp,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 24.dp),
        )
    }
}

@Composable
private fun SettingSwitchRow(
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
            Text(text = title, color = Color.White, fontSize = 16.sp)
            Text(text = description, color = Color.LightGray, fontSize = 13.sp, modifier = Modifier.padding(top = 2.dp))
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
