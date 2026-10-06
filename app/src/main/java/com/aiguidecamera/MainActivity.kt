package com.aiguidecamera

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
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
import androidx.core.content.ContextCompat
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

/** 앱 진입점. 권한을 확인한 뒤 화면 이동(카메라 → 갤러리 → 상세 → 필터 편집, 설정)을 구성한다. */
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme(colorScheme = darkColorScheme()) {
                PermissionGate {
                    AppNavHost()
                }
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

/** 카메라(+ API 28 이하 저장소) 권한이 모두 있을 때만 [content]를 보여준다. */
@Composable
private fun PermissionGate(content: @Composable () -> Unit) {
    val context = LocalContext.current
    val required = remember { requiredPermissions() }
    var granted by remember {
        mutableStateOf(required.all { ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED })
    }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result ->
        granted = required.all { result[it] == true }
    }

    LaunchedEffect(Unit) {
        if (!granted) launcher.launch(required)
    }

    if (granted) {
        content()
    } else {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
                .padding(32.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text("사진을 찍으려면 카메라 권한이 필요해요", color = Color.White)
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = { launcher.launch(required) }) {
                Text("권한 허용하기")
            }
        }
    }
}

private fun requiredPermissions(): Array<String> =
    if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
        arrayOf(Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE)
    } else {
        arrayOf(Manifest.permission.CAMERA)
    }
