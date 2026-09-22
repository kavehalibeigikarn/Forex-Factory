package ir.kaveh.ffnews.data

import android.content.Context
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

object NewsRepository {
    // Public weekly calendar feed of Forex Factory (current week only)
    private const val FEED_URL = "https://nfs.faireconomy.media/ff_calendar_thisweek.json"

    private fun cacheFile(ctx: Context) = File(ctx.applicationContext.filesDir, "calendar.json")

    fun loadCached(ctx: Context): List<NewsEvent> =
        runCatching { parseEvents(cacheFile(ctx).readText()) }.getOrDefault(emptyList())

    /** Downloads the feed; on success replaces the cache. Blocking — call from a background thread. */
    fun fetch(ctx: Context): Result<List<NewsEvent>> = runCatching {
        val conn = (URL(FEED_URL).openConnection() as HttpURLConnection).apply {
            connectTimeout = 15_000
            readTimeout = 20_000
            setRequestProperty("User-Agent", "Mozilla/5.0 (Linux; Android) FFNews/1.0")
            setRequestProperty("Accept", "application/json")
        }
        try {
            val code = conn.responseCode
            if (code != 200) error("HTTP $code")
            val body = conn.inputStream.bufferedReader().use { it.readText() }
            // If the server rate-limits, it returns an HTML page -> parse throws
            val events = runCatching { parseEvents(body) }
                .getOrElse { error("پاسخ سرور معتبر نبود (احتمالاً محدودیت درخواست)") }
            if (events.isEmpty()) error("فید خالی بود")
            cacheFile(ctx).writeText(body)
            Prefs(ctx).lastFetch = System.currentTimeMillis()
            events
        } finally {
            conn.disconnect()
        }
    }
}
