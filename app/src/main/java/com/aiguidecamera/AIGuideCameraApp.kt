package com.aiguidecamera

import android.app.Application
import com.aiguidecamera.render.OffscreenFilterRenderer
import com.aiguidecamera.storage.AppDatabase
import com.aiguidecamera.storage.PhotoSaver

/** 앱 전역에서 하나씩만 필요한 객체(DB, 저장기, 오프스크린 렌더러)를 들고 있는 Application. */
class AIGuideCameraApp : Application() {

    val database: AppDatabase by lazy { AppDatabase.get(this) }

    val photoSaver: PhotoSaver by lazy { PhotoSaver(this) }

    /** 촬영 저장과 사후 편집이 공유하는 단 하나의 렌더러. */
    val offscreenRenderer: OffscreenFilterRenderer by lazy { OffscreenFilterRenderer() }
}
