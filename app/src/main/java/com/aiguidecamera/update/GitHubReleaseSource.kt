package com.aiguidecamera.update

import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

/** 배포된 최신 버전 정보. [apkUrl]은 내려받을 APK 주소. */
data class ReleaseInfo(
    val versionName: String,
    val apkUrl: String,
    val apkSizeBytes: Long,
    val notes: String,
)

/**
 * GitHub Releases에서 최신 릴리스(태그 = 버전)와 첨부된 APK를 찾는다. 네트워크 호출이므로 IO 스레드에서 부른다.
 * 릴리스가 아직 없거나 APK가 첨부되지 않았으면 null.
 */
class GitHubReleaseSource(
    private val owner: String = UpdateConfig.GITHUB_OWNER,
    private val repo: String = UpdateConfig.GITHUB_REPO,
) {

    fun fetchLatest(): ReleaseInfo? {
        val connection = URL("https://api.github.com/repos/$owner/$repo/releases/latest").openConnection() as HttpURLConnection
        try {
            connection.connectTimeout = UpdateConfig.TIMEOUT_MS
            connection.readTimeout = UpdateConfig.TIMEOUT_MS
            connection.setRequestProperty("Accept", "application/vnd.github+json")
            when (val code = connection.responseCode) {
                HttpURLConnection.HTTP_OK -> Unit
                HttpURLConnection.HTTP_NOT_FOUND -> return null
                else -> throw IOException("GitHub 응답 $code")
            }
            val body = connection.inputStream.bufferedReader().use { it.readText() }
            return parse(JSONObject(body))
        } finally {
            connection.disconnect()
        }
    }

    private fun parse(json: JSONObject): ReleaseInfo? {
        val assets = json.optJSONArray("assets") ?: return null
        for (i in 0 until assets.length()) {
            val asset = assets.getJSONObject(i)
            if (!asset.optString("name").endsWith(".apk", ignoreCase = true)) continue
            return ReleaseInfo(
                versionName = json.optString("tag_name").removePrefix("v"),
                apkUrl = asset.getString("browser_download_url"),
                apkSizeBytes = asset.optLong("size"),
                notes = json.optString("body"),
            )
        }
        return null
    }
}
