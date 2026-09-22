package ir.kaveh.ffnews.alert

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import ir.kaveh.ffnews.data.NewsRepository
import ir.kaveh.ffnews.data.Prefs

class AlertReceiver : BroadcastReceiver() {
    override fun onReceive(ctx: Context, intent: Intent) {
        val t = intent.getLongExtra(AlertScheduler.EXTRA_T, 0L)
        val type = intent.getIntExtra(AlertScheduler.EXTRA_TYPE, AlertScheduler.TYPE_WARN)
        if (t == 0L) return
        val s = Prefs(ctx).load()
        val items = NewsRepository.loadCached(ctx).filter { it.epochMillis == t && s.alertMatch(it) }
        if (items.isEmpty()) return
        Notifications.show(ctx, t, type, items, s, AlertScheduler.code(t, type))
    }
}
