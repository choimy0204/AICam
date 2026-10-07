package com.aiguidecamera.camera

import androidx.camera.extensions.ExtensionMode

/** 지금 카메라에 걸린 노출 보정과 기기 촬영 모드(야간·HDR). 디버그 오버레이와 연사 장수 결정에 쓴다. */
data class CameraStatus(
    val exposureEv: Float = 0f,
    /** androidx.camera.extensions.ExtensionMode 값. 기기가 지원하지 않으면 NONE. */
    val extensionMode: Int = ExtensionMode.NONE,
) {
    val extensionLabel: String
        get() = when (extensionMode) {
            ExtensionMode.NIGHT -> "night"
            ExtensionMode.HDR -> "hdr"
            else -> "-"
        }
}
