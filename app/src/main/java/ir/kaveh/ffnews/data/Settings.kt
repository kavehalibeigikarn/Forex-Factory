package ir.kaveh.ffnews.data

import android.content.Context
import java.time.ZoneId

val ALL_CURRENCIES = listOf("USD", "EUR", "GBP", "JPY", "AUD", "NZD", "CAD", "CHF", "CNY")
val DEFAULT_CURRENCIES = setOf("USD", "EUR", "GBP", "JPY", "NZD", "CAD")

/** "" = device time zone */
val ZONES = listOf(
    "" to "ساعت گوشی",
    "Asia/Tehran" to "تهران",
    "Europe/London" to "لندن",
    "America/New_York" to "نیویورک",
    "UTC" to "UTC",
)

data class Settings(
    val showRed: Boolean = true,
    val showOrange: Boolean = true,
    val alertRed: Boolean = true,
    val alertOrange: Boolean = false,
    val currencies: Set<String> = DEFAULT_CURRENCIES,
    val zoneId: String = "",
    val warnMinutes: Int = 15,
    val soundMinutes: Int = 1,
) {
    fun zone(): ZoneId =
        if (zoneId.isBlank()) ZoneId.systemDefault()
        else runCatching { ZoneId.of(zoneId) }.getOrDefault(ZoneId.systemDefault())

    fun showMatch(e: NewsEvent) =
        e.country in currencies && ((e.isRed && showRed) || (e.isOrange && showOrange))

    fun alertMatch(e: NewsEvent) =
        e.country in currencies && ((e.isRed && alertRed) || (e.isOrange && alertOrange))
}

class Prefs(ctx: Context) {
    private val sp = ctx.applicationContext.getSharedPreferences("ffnews", Context.MODE_PRIVATE)

    fun load() = Settings(
        showRed = sp.getBoolean("showRed", true),
        showOrange = sp.getBoolean("showOrange", true),
        alertRed = sp.getBoolean("alertRed", true),
        alertOrange = sp.getBoolean("alertOrange", false),
        currencies = sp.getStringSet("currencies", null)?.toSet() ?: DEFAULT_CURRENCIES,
        zoneId = sp.getString("zoneId", "") ?: "",
        warnMinutes = sp.getInt("warnMinutes", 15),
        soundMinutes = sp.getInt("soundMinutes", 1),
    )

    fun save(s: Settings) {
        sp.edit()
            .putBoolean("showRed", s.showRed)
            .putBoolean("showOrange", s.showOrange)
            .putBoolean("alertRed", s.alertRed)
            .putBoolean("alertOrange", s.alertOrange)
            .putStringSet("currencies", s.currencies.toSet())
            .putString("zoneId", s.zoneId)
            .putInt("warnMinutes", s.warnMinutes)
            .putInt("soundMinutes", s.soundMinutes)
            .apply()
    }

    var lastFetch: Long
        get() = sp.getLong("lastFetch", 0L)
        set(v) { sp.edit().putLong("lastFetch", v).apply() }

    var scheduledCodes: Set<String>
        get() = sp.getStringSet("codes", null)?.toSet() ?: emptySet()
        set(v) { sp.edit().putStringSet("codes", v.toSet()).apply() }
}
