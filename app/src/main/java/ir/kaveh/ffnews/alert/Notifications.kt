package ir.kaveh.ffnews.alert

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import ir.kaveh.ffnews.MainActivity
import ir.kaveh.ffnews.R
import ir.kaveh.ffnews.data.NewsEvent
import ir.kaveh.ffnews.data.Settings
import ir.kaveh.ffnews.util.timeFmt

object Notifications {
    private const val CH_WARN = "warn_v1"
    private const val CH_ALARM = "alarm_v1"

    /** How long the insistent alarm sound keeps playing */
    private const val SOUND_MS = 10_000L

    fun createChannels(ctx: Context) {
        val nm = ctx.getSystemService(NotificationManager::class.java)
        val warn = NotificationChannel(CH_WARN, "هشدار پیش از خبر", NotificationManager.IMPORTANCE_HIGH).apply {
            description = "یادآوری چند دقیقه قبل از انتشار خبر"
            enableVibration(true)
        }
        val alarmUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        val alarm = NotificationChannel(CH_ALARM, "هشدار صوتی نزدیک انتشار", NotificationManager.IMPORTANCE_HIGH).apply {
            description = "صدای چندثانیه‌ای قبل از انتشار خبر"
            setSound(
                alarmUri,
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
            )
            enableVibration(true)
            vibrationPattern = longArrayOf(0, 600, 300, 600, 300, 600)
        }
        nm.createNotificationChannels(listOf(warn, alarm))
    }

    fun show(ctx: Context, t: Long, type: Int, items: List<NewsEvent>, s: Settings, id: Int) {
        if (Build.VERSION.SDK_INT >= 33 &&
            ctx.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) return

        val isSound = type == AlertScheduler.TYPE_SOUND
        val mins = if (isSound) s.soundMinutes else s.warnMinutes
        val dot = if (items.any { it.isRed }) "🔴" else "🟠"
        val title = "$dot $mins دقیقه تا خبر — ساعت ${timeFmt(t, s.zone())}"
        val lines = items.map { e ->
            buildString {
                append(e.country).append(": ").append(e.title)
                if (e.forecast.isNotBlank()) append("  | F: ").append(e.forecast)
                if (e.previous.isNotBlank()) append("  P: ").append(e.previous)
            }
        }

        val open = PendingIntent.getActivity(
            ctx, 0,
            Intent(ctx, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_IMMUTABLE
        )

        val b = Notification.Builder(ctx, if (isSound) CH_ALARM else CH_WARN)
            .setSmallIcon(R.drawable.ic_stat_news)
            .setContentTitle(title)
            .setContentText(lines.joinToString(" • "))
            .setStyle(Notification.BigTextStyle().bigText(lines.joinToString("\n")))
            .setContentIntent(open)
            .setAutoCancel(true)
            .setCategory(Notification.CATEGORY_REMINDER)
            .setWhen(t)
            .setShowWhen(true)
        if (isSound) b.setTimeoutAfter(SOUND_MS) // removing the notification stops the sound

        val n = b.build()
        if (isSound) n.flags = n.flags or Notification.FLAG_INSISTENT // loop sound until timeout

        ctx.getSystemService(NotificationManager::class.java).notify(id, n)
    }

    fun showTest(ctx: Context, s: Settings) {
        val t = System.currentTimeMillis() + s.soundMinutes * 60_000L
        val fake = NewsEvent("Test — Non-Farm Employment Change", "USD", t, "High", "150K", "142K")
        show(ctx, t, AlertScheduler.TYPE_SOUND, listOf(fake), s, id = 1)
    }
}
