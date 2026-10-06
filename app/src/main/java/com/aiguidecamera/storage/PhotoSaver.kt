package com.aiguidecamera.storage

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 비트맵을 JPEG로 Pictures/AIGuideCamera 에 저장하고 content Uri를 돌려준다.
 * 파일명: IMG_yyyyMMdd_HHmmss.jpg (필터본), IMG_yyyyMMdd_HHmmss_orig.jpg (원본).
 */
class PhotoSaver(context: Context) {

    private val resolver = context.applicationContext.contentResolver

    /** 촬영 시각으로 파일명 앞부분(IMG_yyyyMMdd_HHmmss)을 만든다. 필터본·원본이 같은 이름을 공유한다. */
    fun baseNameFor(timeMillis: Long): String {
        val format = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US)
        return "IMG_" + format.format(Date(timeMillis))
    }

    fun filteredFileName(baseName: String) = "$baseName.jpg"

    fun originalFileName(baseName: String) = "${baseName}_orig.jpg"

    suspend fun saveJpeg(bitmap: Bitmap, fileName: String): Uri = withContext(Dispatchers.IO) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            saveWithMediaStore(bitmap, fileName)
        } else {
            saveToPublicDirectory(bitmap, fileName)
        }
    }

    /** API 29+: RELATIVE_PATH + IS_PENDING. 이름이 겹치면 MediaStore가 알아서 뒤에 번호를 붙인다. */
    private fun saveWithMediaStore(bitmap: Bitmap, fileName: String): Uri {
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
            put(MediaStore.Images.Media.MIME_TYPE, MIME_JPEG)
            put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/" + ALBUM_NAME)
            put(MediaStore.Images.Media.IS_PENDING, 1)
        }
        val uri = checkNotNull(resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)) {
            "MediaStore에 항목을 만들지 못했습니다"
        }
        try {
            resolver.openOutputStream(uri).use { stream ->
                checkNotNull(stream) { "출력 스트림을 열지 못했습니다" }
                check(bitmap.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, stream)) { "JPEG 인코딩 실패" }
            }
            values.clear()
            values.put(MediaStore.Images.Media.IS_PENDING, 0)
            resolver.update(uri, values, null, null)
            return uri
        } catch (error: Exception) {
            resolver.delete(uri, null, null)
            throw error
        }
    }

    /** API 26~28: Pictures 폴더에 직접 쓰고 MediaStore에 등록한다 (WRITE_EXTERNAL_STORAGE 필요). */
    @Suppress("DEPRECATION")
    private fun saveToPublicDirectory(bitmap: Bitmap, fileName: String): Uri {
        val album = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), ALBUM_NAME)
        if (!album.exists()) check(album.mkdirs()) { "앨범 폴더를 만들지 못했습니다" }
        val file = uniqueFile(album, fileName)
        FileOutputStream(file).use { stream ->
            check(bitmap.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, stream)) { "JPEG 인코딩 실패" }
        }
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, file.name)
            put(MediaStore.Images.Media.MIME_TYPE, MIME_JPEG)
            put(MediaStore.Images.Media.DATA, file.absolutePath)
        }
        return checkNotNull(resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)) {
            "MediaStore 등록 실패"
        }
    }

    private fun uniqueFile(directory: File, fileName: String): File {
        var candidate = File(directory, fileName)
        var index = 1
        while (candidate.exists()) {
            candidate = File(directory, fileName.removeSuffix(".jpg") + "_$index.jpg")
            index++
        }
        return candidate
    }

    private companion object {
        const val ALBUM_NAME = "AIGuideCamera"
        const val MIME_JPEG = "image/jpeg"
        const val JPEG_QUALITY = 95
    }
}
