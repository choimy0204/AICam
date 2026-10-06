package com.aiguidecamera.update

import android.content.Context
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File

/** 업데이트 진행 상태. 대화상자(Available 이후)와 설정 화면(Checking·UpToDate·CheckFailed)이 나눠 보여준다. */
sealed interface UpdateState {
    data object Idle : UpdateState
    data object Checking : UpdateState
    data object UpToDate : UpdateState
    data object CheckFailed : UpdateState
    data class Available(val release: ReleaseInfo) : UpdateState
    data class Downloading(val release: ReleaseInfo, val progress: Float) : UpdateState
    data class ReadyToInstall(val release: ReleaseInfo, val apk: File) : UpdateState
    data class DownloadFailed(val release: ReleaseInfo) : UpdateState
}

/**
 * 최신 버전 확인 → APK 다운로드 → 설치 화면 넘기기까지의 흐름을 관리한다. 앱 전체에서 하나.
 * 자동 확인([silent])은 새 버전이 있을 때만 상태를 바꾸고, 실패나 최신이면 조용히 넘어간다.
 */
class UpdateManager(
    context: Context,
    private val currentVersion: String,
    private val releaseSource: GitHubReleaseSource = GitHubReleaseSource(),
    private val downloader: ApkDownloader = ApkDownloader(),
) {
    private val downloadFile = File(File(context.cacheDir, UpdateConfig.DOWNLOAD_DIR), UpdateConfig.DOWNLOAD_FILE_NAME)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var job: Job? = null

    private val _state = MutableStateFlow<UpdateState>(UpdateState.Idle)
    val state: StateFlow<UpdateState> = _state.asStateFlow()

    fun checkForUpdate(silent: Boolean) {
        if (job?.isActive == true) return
        if (!silent) _state.value = UpdateState.Checking
        job = scope.launch {
            val release = try {
                releaseSource.fetchLatest()
            } catch (error: Exception) {
                Log.w(TAG, "업데이트 확인 실패", error)
                if (!silent) _state.value = UpdateState.CheckFailed
                return@launch
            }
            _state.value = when {
                release != null && AppVersion.isNewer(release.versionName, currentVersion) -> UpdateState.Available(release)
                silent -> UpdateState.Idle
                else -> UpdateState.UpToDate
            }
        }
    }

    fun download() {
        val release = when (val current = _state.value) {
            is UpdateState.Available -> current.release
            is UpdateState.DownloadFailed -> current.release
            else -> return
        }
        if (job?.isActive == true) return
        _state.value = UpdateState.Downloading(release, 0f)
        job = scope.launch {
            try {
                downloader.download(release.apkUrl, downloadFile, release.apkSizeBytes) { progress ->
                    _state.value = UpdateState.Downloading(release, progress)
                }
                _state.value = UpdateState.ReadyToInstall(release, downloadFile)
            } catch (error: Exception) {
                Log.w(TAG, "업데이트 다운로드 실패", error)
                _state.value = UpdateState.DownloadFailed(release)
            }
        }
    }

    /** "나중에". 진행 중인 다운로드도 멈춘다. */
    fun dismiss() {
        job?.cancel()
        _state.value = UpdateState.Idle
    }

    private companion object {
        const val TAG = "UpdateManager"
    }
}
