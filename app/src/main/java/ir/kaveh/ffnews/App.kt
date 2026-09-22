package ir.kaveh.ffnews

import android.app.Application
import ir.kaveh.ffnews.alert.Notifications
import ir.kaveh.ffnews.alert.RefreshWorker

class App : Application() {
    override fun onCreate() {
        super.onCreate()
        Notifications.createChannels(this)
        RefreshWorker.enqueue(this)
    }
}
