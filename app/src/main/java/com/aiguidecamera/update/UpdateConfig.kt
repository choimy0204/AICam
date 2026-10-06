package com.aiguidecamera.update

/** 업데이트 확인·다운로드에 쓰는 배포 위치와 네트워크 설정. */
object UpdateConfig {
    const val GITHUB_OWNER = "choimy0204"
    const val GITHUB_REPO = "AICam"

    /** 연결·읽기 타임아웃. */
    const val TIMEOUT_MS = 15_000

    /** 내려받은 APK를 두는 캐시 하위 폴더 (res/xml/file_paths.xml과 같아야 한다). */
    const val DOWNLOAD_DIR = "updates"
    const val DOWNLOAD_FILE_NAME = "AICam-update.apk"
}
