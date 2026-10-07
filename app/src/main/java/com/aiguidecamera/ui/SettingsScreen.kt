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
import androidx.compose.material3.TextButton
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
import com.aiguidecamera.ui.theme.AppColors
import com.aiguidecamera.update.UpdateState

/** 설정 화면: "원본도 함께 저장" 스위치, 앱 버전과 업데이트 확인. */
@Composable
fun SettingsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val settings = (context.applicationContext as AIGuideCameraApp).settings
    val saveOriginalToo by settings.saveOriginalToo.collectAsStateWithLifecycle()
    val versionName = remember { context.packageManager.getPackageInfo(context.packageName, 0).versionName.orEmpty() }
    val updateManager = (context.applicationContext as AIGuideCameraApp).updateManager
    val updateState by updateManager.state.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AppColors.Background)
            .systemBarsPadding(),
    ) {
        ScreenTopBar(title = "설정", onBack = onBack)
        SettingSwitchRow(
            title = "원본도 함께 저장",
            description = "켜면 필터 없는 원본도 같은 폴더에 저장돼요. 원본이 있어야 나중에 필터를 바꿀 수 있어요.",
            checked = saveOriginalToo,
            onCheckedChange = settings::setSaveOriginalToo,
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                Text(text = "버전 $versionName", color = Color.White, fontSize = 16.sp)
                updateStatusText(updateState)?.let { status ->
                    Text(text = status, color = AppColors.TextSecondary, fontSize = 13.sp, modifier = Modifier.padding(top = 2.dp))
                }
            }
            TextButton(
                onClick = { updateManager.checkForUpdate(silent = false) },
                enabled = updateState != UpdateState.Checking,
            ) {
                Text("업데이트 확인")
            }
        }
    }
}

/** 확인 결과만 여기서 보여준다. 새 버전이 있으면 대화상자(UpdateDialog)가 뜬다. */
private fun updateStatusText(state: UpdateState): String? = when (state) {
    UpdateState.Checking -> "확인 중…"
    UpdateState.UpToDate -> "최신 버전이에요"
    UpdateState.CheckFailed -> "확인하지 못했어요. 인터넷 연결을 확인해 주세요"
    else -> null
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
            Text(text = description, color = AppColors.TextSecondary, fontSize = 13.sp, modifier = Modifier.padding(top = 2.dp))
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
