package ir.kaveh.ffnews.alert

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import ir.kaveh.ffnews.data.NewsEvent
import ir.kaveh.ffnews.data.NewsRepository
import ir.kaveh.ffnews.data.Prefs

object AlertScheduler {
    const val TYPE_WARN = 0
    const val TYPE_SOUND = 1
    const val EXTRA_T = "t"
    const val EXTRA_TYPE = "type"
    private const val ACTION = "ir.kaveh.ffnews.ALERT"

    /** Unique per (release minute, alert type). Epoch-minutes*2 still fits in Int. */
    fun code(t: Long, type: Int): Int = (t / 60_000L).toInt() * 2 + type

    private fun intent(ctx: Context) = Intent(ctx, AlertReceiver::class.java).setAction(ACTION)

    @Synchronized
    fun reschedule(ctx: Context, events: List<NewsEvent> = NewsRepository.loadCached(ctx)) {
        val app = ctx.applicationContext
        val prefs = Prefs(app)
        val s = prefs.load()
        val am = app.getSystemService(AlarmManager::class.java)

        // 1) cancel everything scheduled before
        for (c in prefs.scheduledCodes) {
            val code = c.toIntOrNull() ?: continue
            PendingIntent.getBroadcast(
                app, code, intent(app),
                PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
            )?.let { am.cancel(it); it.cancel() }
        }

        // 2) schedule upcoming, one alarm per release time (events at same minute are merged)
        val now = System.currentTimeMillis()
        val codes = mutableSetOf<String>()
        val times = events.filter { s.alertMatch(it) }.map { it.epochMillis }.distinct()
        for (t in times) {
            for ((type, mins) in listOf(TYPE_WARN to s.warnMinutes, TYPE_SOUND to s.soundMinutes)) {
                if (mins <= 0) continue
                val at = t - mins * 60_000L
                if (at <= now) continue
                val code = code(t, type)
                val pi = PendingIntent.getBroadcast(
                    app, code,
                    intent(app).putExtra(EXTRA_T, t).putExtra(EXTRA_TYPE, type),
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                setAlarm(am, at, pi)
                codes += code.toString()
            }
        }
        prefs.scheduledCodes = codes
    }

    fun canExact(ctx: Context): Boolean =
        Build.VERSION.SDK_INT < 31 ||
            ctx.getSystemService(AlarmManager::class.java).canScheduleExactAlarms()

    private fun setAlarm(am: AlarmManager, at: Long, pi: PendingIntent) {
        val exact = Build.VERSION.SDK_INT < 31 || am.canScheduleExactAlarms()
        try {
            if (exact) am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi)
            else am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi)
        } catch (_: SecurityException) {
            am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi)
        }
    }
}
