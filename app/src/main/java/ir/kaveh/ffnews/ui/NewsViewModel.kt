package ir.kaveh.ffnews.ui

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import ir.kaveh.ffnews.alert.AlertScheduler
import ir.kaveh.ffnews.alert.Notifications
import ir.kaveh.ffnews.data.NewsEvent
import ir.kaveh.ffnews.data.NewsRepository
import ir.kaveh.ffnews.data.Prefs
import ir.kaveh.ffnews.data.Settings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class NewsViewModel(app: Application) : AndroidViewModel(app) {
    private val prefs = Prefs(app)

    var events by mutableStateOf(NewsRepository.loadCached(app))
        private set
    var settings by mutableStateOf(prefs.load())
        private set
    var loading by mutableStateOf(false)
        private set
    var message by mutableStateOf<String?>(null)
        private set
    var lastFetch by mutableLongStateOf(prefs.lastFetch)
        private set

    private var lastAttempt = 0L

    /** Called on every onResume: refresh if data is stale, otherwise just re-arm alarms. */
    fun onResumed() {
        val now = System.currentTimeMillis()
        val weekOver = events.isNotEmpty() && events.last().epochMillis < now
        val stale = events.isEmpty() || weekOver || now - prefs.lastFetch > 2 * 3_600_000L
        if (stale) refresh(manual = false) else rescheduleAsync()
    }

    fun refresh(manual: Boolean = true) {
        if (loading) return
        val now = System.currentTimeMillis()
        // The feed server blocks frequent requests
        if (now - lastAttempt < 120_000L) {
            if (manual) message = "برای جلوگیری از مسدود شدن، حداقل ۲ دقیقه بین هر به‌روزرسانی فاصله بده."
            return
        }
        lastAttempt = now
        loading = true
        viewModelScope.launch {
            val r = withContext(Dispatchers.IO) { NewsRepository.fetch(getApplication()) }
            r.onSuccess {
                events = it
                message = null
                lastFetch = prefs.lastFetch
            }.onFailure {
                message = "دریافت نشد: ${it.message}. اطلاعات ذخیره‌شده نمایش داده می‌شود."
            }
            withContext(Dispatchers.IO) { AlertScheduler.reschedule(getApplication(), events) }
            loading = false
        }
    }

    fun update(new: Settings) {
        settings = new
        prefs.save(new)
        rescheduleAsync()
    }

    fun testSound() = Notifications.showTest(getApplication(), settings)

    fun dismissMessage() { message = null }

    private fun rescheduleAsync() {
        val snapshot: List<NewsEvent> = events
        viewModelScope.launch(Dispatchers.IO) { AlertScheduler.reschedule(getApplication(), snapshot) }
    }
}
