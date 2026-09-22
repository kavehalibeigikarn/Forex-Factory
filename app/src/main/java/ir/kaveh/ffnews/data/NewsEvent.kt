package ir.kaveh.ffnews.data

import org.json.JSONArray
import java.time.OffsetDateTime

data class NewsEvent(
    val title: String,
    val country: String,
    val epochMillis: Long,
    val impact: String,
    val forecast: String,
    val previous: String,
) {
    val isRed: Boolean get() = impact.equals("High", ignoreCase = true)
    val isOrange: Boolean get() = impact.equals("Medium", ignoreCase = true)
}

/** Parses the Forex Factory weekly JSON feed. Throws if the body is not a JSON array. */
fun parseEvents(json: String): List<NewsEvent> {
    val arr = JSONArray(json)
    val out = ArrayList<NewsEvent>(arr.length())
    for (i in 0 until arr.length()) {
        val o = arr.optJSONObject(i) ?: continue
        val t = runCatching {
            OffsetDateTime.parse(o.optString("date")).toInstant().toEpochMilli()
        }.getOrNull() ?: continue
        out += NewsEvent(
            title = o.optString("title"),
            country = o.optString("country"),
            epochMillis = t,
            impact = o.optString("impact"),
            forecast = o.optString("forecast"),
            previous = o.optString("previous"),
        )
    }
    return out.sortedBy { it.epochMillis }
}
