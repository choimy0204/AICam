package com.aiguidecamera

import android.app.Application
import com.aiguidecamera.analysis.StillFaceDetector
import com.aiguidecamera.filter.FilterPreferences
import com.aiguidecamera.render.LutLoader
import com.aiguidecamera.render.OffscreenFilterRenderer
import com.aiguidecamera.storage.AppDatabase
import com.aiguidecamera.storage.AppSettings
import com.aiguidecamera.storage.PhotoLoader
import com.aiguidecamera.storage.PhotoSaver

/** 앱 전역에서 하나씩만 필요한 객체(DB, 저장기, LUT 로더, 오프스크린 렌더러 등)를 들고 있는 Application. */
class AIGuideCameraApp : Application() {

    val database: AppDatabase by lazy { AppDatabase.get(this) }

    val photoSaver: PhotoSaver by lazy { PhotoSaver(this) }

    val photoLoader: PhotoLoader by lazy { PhotoLoader(this) }

    val settings: AppSettings by lazy { AppSettings(this) }

    /** 프리뷰와 오프스크린 렌더러가 디코딩된 LUT를 공유한다. */
    val lutLoader: LutLoader by lazy { LutLoader(assets) }

    /** 촬영 저장과 사후 편집이 공유하는 단 하나의 렌더러. */
    val offscreenRenderer: OffscreenFilterRenderer by lazy { OffscreenFilterRenderer(lutLoader) }

    val stillFaceDetector: StillFaceDetector by lazy { StillFaceDetector() }

    val filterPreferences: FilterPreferences by lazy { FilterPreferences(this) }
}
