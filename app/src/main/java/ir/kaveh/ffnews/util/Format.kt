package ir.kaveh.ffnews.util

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private val FA: Locale = Locale.forLanguageTag("fa")
private val HHMM = DateTimeFormatter.ofPattern("HH:mm")
private val DAY = DateTimeFormatter.ofPattern("EEEE   yyyy/MM/dd", FA)
private val DAY_TIME = DateTimeFormatter.ofPattern("EEEE  HH:mm", FA)
private val FULL = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm")

fun timeFmt(t: Long, zone: ZoneId): String = HHMM.format(Instant.ofEpochMilli(t).atZone(zone))
fun dayFmt(d: LocalDate): String = DAY.format(d)
fun dayTimeFmt(t: Long, zone: ZoneId): String = DAY_TIME.format(Instant.ofEpochMilli(t).atZone(zone))
fun fullFmt(t: Long, zone: ZoneId): String = FULL.format(Instant.ofEpochMilli(t).atZone(zone))
fun localDate(t: Long, zone: ZoneId): LocalDate = Instant.ofEpochMilli(t).atZone(zone).toLocalDate()

fun countdown(ms: Long): String {
    val total = ms.coerceAtLeast(0) / 1000
    val d = total / 86_400
    val h = (total % 86_400) / 3_600
    val m = (total % 3_600) / 60
    val s = total % 60
    val hms = String.format(Locale.US, "%02d:%02d:%02d", h, m, s)
    return if (d > 0) "$d روز  $hms" else hms
}
