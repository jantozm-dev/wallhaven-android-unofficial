package dev.jantozm.wallhaven.data

import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.IOException
import java.util.concurrent.TimeUnit

class WallhavenApi {

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    data class Wallpaper(
        val id: String,
        val path: String,
        val width: Int,
        val height: Int,
        val thumbSmall: String,
        val thumbLarge: String,
        val author: String?
    )

    data class SearchResult(val wallpapers: List<Wallpaper>, val page: Int, val lastPage: Int)

    fun search(
        query: String,
        categories: Set<String>,
        sorting: String,
        page: Int,
        minWidth: Int = 0,
        minHeight: Int = 0
    ): SearchResult {
        val url = BASE_URL.toHttpUrl().newBuilder()
            .addQueryParameter("categories", categoriesParam(categories))
            .addQueryParameter("purity", "100")
            .addQueryParameter("sorting", sorting)
            .addQueryParameter("page", page.toString())
            .apply {
                if (query.isNotBlank()) addQueryParameter("q", query.trim())
                if (minWidth > 0 && minHeight > 0) {
                    addQueryParameter("atleast", "${minWidth}x$minHeight")
                }
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
            return parseSearch(body.string())
        }
    }

    fun download(url: String, destination: File): File {
        val request = Request.Builder()
            .url(url)
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

    private fun categoriesParam(categories: Set<String>): String {
        if (categories.isEmpty()) return "111"
        return buildString {
            append(if ("general" in categories) "1" else "0")
            append(if ("anime" in categories) "1" else "0")
            append(if ("people" in categories) "1" else "0")
        }
    }

    private fun parseSearch(json: String): SearchResult {
        val root = JSONObject(json)
        val data: JSONArray = root.optJSONArray("data") ?: JSONArray()
        val wallpapers = ArrayList<Wallpaper>(data.length())
        for (i in 0 until data.length()) {
            val item = data.optJSONObject(i) ?: continue
            val path = item.optString("path")
            if (path.isEmpty()) continue
            val thumbs = item.optJSONObject("thumbs")
            wallpapers.add(
                Wallpaper(
                    id = item.optString("id"),
                    path = path,
                    width = item.optInt("width"),
                    height = item.optInt("height"),
                    thumbSmall = thumbs?.optString("small").orEmpty(),
                    thumbLarge = thumbs?.optString("large").orEmpty(),
                    author = item.optJSONObject("uploader")?.optString("username")?.ifBlank { null }
                )
            )
        }
        val meta = root.optJSONObject("meta")
        return SearchResult(
            wallpapers = wallpapers,
            page = meta?.optInt("current_page", 1) ?: 1,
            lastPage = meta?.optInt("last_page", 1) ?: 1
        )
    }

    companion object {
        private const val BASE_URL = "https://wallhaven.cc/api/v1/search"
        private const val USER_AGENT = "wallhaven-android-unofficial/1.0"
    }
}
