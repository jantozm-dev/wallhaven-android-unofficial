package dev.jantozm.wallhaven.data

import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

class WallhavenApi {

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    data class Wallpaper(val id: String, val path: String)

    fun searchRandomSfw(query: String, minWidth: Int, minHeight: Int): List<Wallpaper> {
        val url = BASE_URL.toHttpUrl().newBuilder()
            .addQueryParameter("categories", "100")
            .addQueryParameter("purity", "100")
            .addQueryParameter("sorting", "random")
            .addQueryParameter("atleast", "${minWidth}x$minHeight")
            .addQueryParameter("page", "1")
            .apply {
                if (query.isNotBlank()) addQueryParameter("q", query.trim())
            }
            .build()

        val request = Request.Builder()
            .url(url)
            .header("User-Agent", USER_AGENT)
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw IOException("Wallhaven search failed: HTTP ${response.code}")
            }
            val body = response.body ?: throw IOException("Wallhaven returned an empty body")
            return parseWallpapers(body.string())
        }
    }

    fun download(path: String, destination: java.io.File): java.io.File {
        val request = Request.Builder()
            .url(path)
            .header("User-Agent", USER_AGENT)
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw IOException("Wallhaven download failed: HTTP ${response.code}")
            }
            val source = response.body ?: throw IOException("Wallhaven returned an empty body")
            destination.outputStream().use { out ->
                source.byteStream().copyTo(out)
            }
        }
        return destination
    }

    private fun parseWallpapers(json: String): List<Wallpaper> {
        val root = JSONObject(json)
        val data = root.optJSONArray("data") ?: return emptyList()
        val result = ArrayList<Wallpaper>(data.length())
        for (i in 0 until data.length()) {
            val item = data.optJSONObject(i) ?: continue
            val path = item.optString("path")
            if (path.isNotEmpty()) {
                result.add(Wallpaper(id = item.optString("id"), path = path))
            }
        }
        return result
    }

    companion object {
        private const val BASE_URL = "https://wallhaven.cc/api/v1/search"
        private const val USER_AGENT = "wallhaven-android-unofficial/1.0"
    }
}
