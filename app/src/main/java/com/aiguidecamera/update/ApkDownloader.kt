package com.aiguidecamera.update

import java.io.File
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

/** APK를 파일로 내려받으며 진행률(0~1)을 알린다. 네트워크 호출이므로 IO 스레드에서 부른다. */
class ApkDownloader {

    fun download(url: String, destination: File, expectedSizeBytes: Long, onProgress: (Float) -> Unit) {
        val connection = URL(url).openConnection() as HttpURLConnection
        try {
            connection.connectTimeout = UpdateConfig.TIMEOUT_MS
            connection.readTimeout = UpdateConfig.TIMEOUT_MS
            if (connection.responseCode != HttpURLConnection.HTTP_OK) {
                throw IOException("다운로드 응답 ${connection.responseCode}")
            }
            val totalBytes = connection.contentLengthLong.takeIf { it > 0 } ?: expectedSizeBytes
            destination.parentFile?.mkdirs()
            val partial = File(destination.parentFile, destination.name + ".part")
            connection.inputStream.use { input ->
                partial.outputStream().use { output ->
                    val buffer = ByteArray(BUFFER_SIZE)
                    var written = 0L
                    while (true) {
                        val read = input.read(buffer)
                        if (read < 0) break
                        output.write(buffer, 0, read)
                        written += read
                        if (totalBytes > 0) onProgress((written.toFloat() / totalBytes).coerceAtMost(1f))
                    }
                }
            }
            // 끝까지 받은 파일만 설치 대상이 되도록 마지막에 이름을 바꾼다.
            destination.delete()
            if (!partial.renameTo(destination)) throw IOException("다운로드 파일 저장 실패")
        } finally {
            connection.disconnect()
        }
    }

    private companion object {
        const val BUFFER_SIZE = 64 * 1024
    }
}
