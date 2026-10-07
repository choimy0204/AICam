package com.aiguidecamera.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/** 앱 전체 색. 어두운 카메라 화면에 따뜻한 앰버 하나만 포인트로 쓴다 (런처 아이콘·res/values/colors.xml과 같은 색). */
object AppColors {
    val Background = Color(0xFF0C0E14)
    val Surface = Color(0xFF161922)
    val SurfaceVariant = Color(0xFF232733)
    val Accent = Color(0xFFFFC94D)
    val OnAccent = Color(0xFF1A1300)
    val Good = Color(0xFF4CD964)
    val TextPrimary = Color.White
    val TextSecondary = Color(0xFFA9AEBB)

    /** 프리뷰 위에 뜨는 반투명 컨트롤 배경. */
    val Scrim = Color(0xB30C0E14)
}

/** Material 컴포넌트(스위치, 버튼, 대화상자 등)가 앱 색을 따르게 한다. */
@Composable
fun AIGuideCameraTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = AppColors.Accent,
            onPrimary = AppColors.OnAccent,
            secondary = AppColors.Accent,
            onSecondary = AppColors.OnAccent,
            background = AppColors.Background,
            onBackground = AppColors.TextPrimary,
            surface = AppColors.Surface,
            onSurface = AppColors.TextPrimary,
            surfaceVariant = AppColors.SurfaceVariant,
            onSurfaceVariant = AppColors.TextSecondary,
            surfaceContainerHigh = AppColors.SurfaceVariant,
        ),
        content = content,
    )
}
