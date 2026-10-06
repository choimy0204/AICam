package com.aiguidecamera.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aiguidecamera.AIGuideCameraApp
import com.aiguidecamera.update.ApkInstaller
import com.aiguidecamera.update.ReleaseInfo
import com.aiguidecamera.update.UpdateState

/** 새 버전이 있을 때 뜨는 대화상자: 안내 → 다운로드 진행률 → 설치(시스템 설치 화면). 어느 화면 위에서도 뜬다. */
@Composable
fun UpdateDialog() {
    val context = LocalContext.current
    val manager = (context.applicationContext as AIGuideCameraApp).updateManager
    val state by manager.state.collectAsStateWithLifecycle()

    when (val current = state) {
        is UpdateState.Available -> UpdateAlert(
            release = current.release,
            message = "새 버전 ${current.release.versionName}이 나왔어요.",
            confirmText = "업데이트",
            onConfirm = manager::download,
            onDismiss = manager::dismiss,
        )
        is UpdateState.Downloading -> UpdateAlert(
            release = current.release,
            message = "내려받는 중… ${(current.progress * 100).toInt()}%",
            progress = current.progress,
            confirmText = null,
            onConfirm = {},
            onDismiss = manager::dismiss,
        )
        is UpdateState.ReadyToInstall -> UpdateAlert(
            release = current.release,
            // 허용 여부는 설정에서 돌아온 뒤 바뀔 수 있어 문구는 고정하고, 버튼을 누를 때 확인한다.
            message = "다운로드가 끝났어요. 설치를 눌러 주세요.\n" +
                "처음이면 \"출처를 알 수 없는 앱 설치\" 허용 화면이 먼저 열려요. 허용한 뒤 다시 설치를 눌러 주세요.",
            confirmText = "설치",
            onConfirm = {
                if (ApkInstaller.canInstall(context)) {
                    ApkInstaller.install(context, current.apk)
                } else {
                    ApkInstaller.openInstallPermissionSettings(context)
                }
            },
            onDismiss = manager::dismiss,
        )
        is UpdateState.DownloadFailed -> UpdateAlert(
            release = current.release,
            message = "내려받지 못했어요. 인터넷 연결을 확인하고 다시 시도해 주세요.",
            confirmText = "다시 시도",
            onConfirm = manager::download,
            onDismiss = manager::dismiss,
        )
        else -> Unit
    }
}

@Composable
private fun UpdateAlert(
    release: ReleaseInfo,
    message: String,
    confirmText: String?,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    progress: Float? = null,
) {
    AlertDialog(
        onDismissRequest = {},
        title = { Text("업데이트 ${release.versionName}") },
        text = {
            Column {
                Text(message)
                if (progress != null) {
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                    )
                }
                if (release.notes.isNotBlank()) {
                    Text(release.notes, modifier = Modifier.padding(top = 12.dp))
                }
            }
        },
        confirmButton = {
            if (confirmText != null) TextButton(onClick = onConfirm) { Text(confirmText) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(if (progress != null) "취소" else "나중에") }
        },
    )
}
