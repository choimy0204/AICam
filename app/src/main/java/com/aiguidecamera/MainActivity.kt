package com.aiguidecamera

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.aiguidecamera.ui.CameraScreen
import com.aiguidecamera.ui.FilterEditScreen
import com.aiguidecamera.ui.FilterEditViewModel
import com.aiguidecamera.ui.GalleryScreen
import com.aiguidecamera.ui.PhotoDetailScreen
import com.aiguidecamera.ui.SettingsScreen
import com.aiguidecamera.ui.UpdateDialog
import com.aiguidecamera.ui.theme.AIGuideCameraTheme
import com.aiguidecamera.ui.theme.AppColors

/** 앱 진입점. 권한을 확인한 뒤 화면 이동(카메라 → 갤러리 → 상세 → 필터 편집, 설정)을 구성하고, 시작할 때 업데이트를 확인한다. */
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // 앱을 새로 열 때만 확인한다. 새 버전이 없거나 실패하면 아무것도 띄우지 않는다.
        if (savedInstanceState == null) (application as AIGuideCameraApp).updateManager.checkForUpdate(silent = true)
        setContent {
            AIGuideCameraTheme {
                PermissionGate {
                    AppNavHost()
                }
                UpdateDialog()
            }
        }
    }
}

@Composable
private fun AppNavHost() {
    val navController = rememberNavController()
    val photoIdArgument = listOf(navArgument(FilterEditViewModel.ARG_PHOTO_ID) { type = NavType.LongType })

    NavHost(navController = navController, startDestination = Routes.CAMERA) {
        composable(Routes.CAMERA) {
            CameraScreen(
                onThumbnailClick = { navController.navigate(Routes.GALLERY) },
                onSettingsClick = { navController.navigate(Routes.SETTINGS) },
            )
        }
        composable(Routes.GALLERY) {
            GalleryScreen(
                onBack = { navController.popBackStack() },
                onPhotoClick = { id -> navController.navigate(Routes.detail(id)) },
            )
        }
        composable(Routes.DETAIL, arguments = photoIdArgument) { entry ->
            PhotoDetailScreen(
                photoId = entry.arguments?.getLong(FilterEditViewModel.ARG_PHOTO_ID) ?: 0L,
                onBack = { navController.popBackStack() },
                onEditClick = { id -> navController.navigate(Routes.edit(id)) },
            )
        }
        composable(Routes.EDIT, arguments = photoIdArgument) {
            FilterEditScreen(
                onBack = { navController.popBackStack() },
                // 새 사진이 갤러리 맨 앞에 생기므로 편집·상세를 닫고 갤러리로 돌아간다.
                onSaved = { navController.popBackStack(Routes.GALLERY, inclusive = false) },
            )
        }
        composable(Routes.SETTINGS) {
            SettingsScreen(onBack = { navController.popBackStack() })
        }
    }
}

private object Routes {
    const val CAMERA = "camera"
    const val GALLERY = "gallery"
    const val DETAIL = "detail/{${FilterEditViewModel.ARG_PHOTO_ID}}"
    const val EDIT = "edit/{${FilterEditViewModel.ARG_PHOTO_ID}}"
    const val SETTINGS = "settings"

    fun detail(id: Long) = "detail/$id"
    fun edit(id: Long) = "edit/$id"
}

/**
 * 카메라(+ API 28 이하 저장소) 권한이 모두 있을 때만 [content]를 보여준다.
 * "다시 묻지 않음"으로 거절됐으면 앱 설정 화면으로 안내하고, 설정에서 돌아오면(ON_RESUME) 다시 확인한다.
 */
@Composable
private fun PermissionGate(content: @Composable () -> Unit) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val required = remember { requiredPermissions() }
    fun allGranted() = required.all { ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED }

    var granted by remember { mutableStateOf(allGranted()) }
    var permanentlyDenied by remember { mutableStateOf(false) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        granted = allGranted()
        val activity = context as? Activity
        // 거절 직후 rationale을 보여줄 수 없다면 시스템이 더 이상 묻지 않는 상태다.
        permanentlyDenied = !granted && activity != null && required.any {
            ContextCompat.checkSelfPermission(context, it) != PackageManager.PERMISSION_GRANTED &&
                !ActivityCompat.shouldShowRequestPermissionRationale(activity, it)
        }
    }

    LaunchedEffect(Unit) {
        if (!granted) launcher.launch(required)
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) granted = allGranted()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    if (granted) {
        content()
    } else {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(AppColors.Background)
                .padding(32.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text("사진을 찍으려면 카메라 권한이 필요해요", color = Color.White)
            Spacer(modifier = Modifier.height(16.dp))
            if (permanentlyDenied) {
                Text("설정 > 권한에서 카메라를 허용해 주세요", color = AppColors.TextSecondary)
                Spacer(modifier = Modifier.height(12.dp))
                Button(onClick = { openAppSettings(context) }) {
                    Text("앱 설정 열기")
                }
            } else {
                Button(onClick = { launcher.launch(required) }) {
                    Text("권한 허용하기")
                }
            }
        }
    }
}

private fun openAppSettings(context: Context) {
    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", context.packageName, null))
    context.startActivity(intent)
}

private fun requiredPermissions(): Array<String> =
    if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
        arrayOf(Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE)
    } else {
        arrayOf(Manifest.permission.CAMERA)
    }
