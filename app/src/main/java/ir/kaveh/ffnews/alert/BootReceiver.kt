package ir.kaveh.ffnews.alert

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/** Re-creates alarms after reboot, app update, time/zone change or exact-alarm permission change. */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(ctx: Context, intent: Intent) {
        val pending = goAsync()
        Thread {
            try {
                AlertScheduler.reschedule(ctx.applicationContext)
            } finally {
                pending.finish()
            }
        }.start()
    }
}
