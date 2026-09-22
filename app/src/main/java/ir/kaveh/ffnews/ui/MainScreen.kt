@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)

package ir.kaveh.ffnews.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings as AndroidSettings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import ir.kaveh.ffnews.alert.AlertScheduler
import ir.kaveh.ffnews.data.ALL_CURRENCIES
import ir.kaveh.ffnews.data.NewsEvent
import ir.kaveh.ffnews.data.Settings
import ir.kaveh.ffnews.data.ZONES
import ir.kaveh.ffnews.util.countdown
import ir.kaveh.ffnews.util.dayFmt
import ir.kaveh.ffnews.util.dayTimeFmt
import ir.kaveh.ffnews.util.fullFmt
import ir.kaveh.ffnews.util.localDate
import ir.kaveh.ffnews.util.timeFmt
import kotlinx.coroutines.delay
import java.time.ZoneId

private val RED = Color(0xFFE53935)
private val ORANGE = Color(0xFFFB8C00)
private fun impactColor(e: NewsEvent) = if (e.isRed) RED else ORANGE
private fun dot(e: NewsEvent) = if (e.isRed) "🔴" else "🟠"

@Composable
fun MainScreen(resumeTick: Int, vm: NewsViewModel = viewModel()) {
    val ctx = LocalContext.current
    val s = vm.settings
    val zone = s.zone()

    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) { now = System.currentTimeMillis(); delay(1000) }
    }

    // ---- permissions ----
    val notifLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }
    val notifGranted = remember(resumeTick) {
        Build.VERSION.SDK_INT < 33 ||
            ctx.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
    }
    val exactOk = remember(resumeTick) { AlertScheduler.canExact(ctx) }
    LaunchedEffect(Unit) {
        if (!notifGranted && Build.VERSION.SDK_INT >= 33) notifLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
    }
    LaunchedEffect(resumeTick) { vm.onResumed() }

    // ---- data ----
    val visible = vm.events.filter { s.showMatch(it) }
    val nextTime = visible.firstOrNull { it.epochMillis >= now }?.epochMillis
    val nextGroup = if (nextTime == null) emptyList() else visible.filter { it.epochMillis == nextTime }
    val byDay = visible.groupBy { localDate(it.epochMillis, zone) }

    LazyColumn(
        modifier = Modifier.fillMaxSize().systemBarsPadding(),
        contentPadding = PaddingValues(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item {
            Header(
                loading = vm.loading,
                lastFetch = vm.lastFetch,
                zone = zone,
                onRefresh = { vm.refresh(manual = true) },
            )
        }
        vm.message?.let { msg ->
            item { InfoCard(msg, "باشه") { vm.dismissMessage() } }
        }
        if (!notifGranted) {
            item {
                InfoCard("اجازه نمایش نوتیفیکیشن داده نشده؛ هشدارها نمایش داده نمی‌شوند.", "تنظیمات") {
                    ctx.startActivity(
                        Intent(AndroidSettings.ACTION_APP_NOTIFICATION_SETTINGS)
                            .putExtra(AndroidSettings.EXTRA_APP_PACKAGE, ctx.packageName)
                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    )
                }
            }
        }
        if (!exactOk && Build.VERSION.SDK_INT >= 31) {
            item {
                InfoCard("آلارم دقیق غیرفعال است؛ هشدارها ممکن است با تأخیر بیایند.", "فعال‌سازی") {
                    ctx.startActivity(
                        Intent(AndroidSettings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, Uri.parse("package:${ctx.packageName}"))
                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    )
                }
            }
        }
        item { NextCard(nextGroup, now, zone) }
        item { SettingsCard(s, onChange = vm::update, onTest = vm::testSound) }

        if (visible.isEmpty() && !vm.loading) {
            item {
                Text(
                    "خبری با این فیلترها پیدا نشد.",
                    modifier = Modifier.fillMaxWidth().padding(24.dp),
                    style = MaterialTheme.typography.bodyLarge,
                )
            }
        }
        byDay.forEach { (day, list) ->
            item {
                Text(
                    dayFmt(day),
                    modifier = Modifier.padding(top = 10.dp, bottom = 2.dp),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            items(list) { e -> EventRow(e, now, zone) }
        }
        item { Spacer(Modifier.height(24.dp)) }
    }
}

@Composable
private fun Header(loading: Boolean, lastFetch: Long, zone: ZoneId, onRefresh: () -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text("اخبار قرمز و نارنجی", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text(
                if (lastFetch > 0) "آخرین به‌روزرسانی: ${fullFmt(lastFetch, zone)}" else "هنوز دریافت نشده",
                style = MaterialTheme.typography.bodySmall,
            )
        }
        if (loading) CircularProgressIndicator(Modifier.padding(8.dp))
        else Button(onClick = onRefresh) { Text("به‌روزرسانی") }
    }
}

@Composable
private fun InfoCard(text: String, action: String, onAction: () -> Unit) {
    Card(
        Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
    ) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(text, Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
            TextButton(onClick = onAction) { Text(action) }
        }
    }
}

@Composable
private fun NextCard(next: List<NewsEvent>, now: Long, zone: ZoneId) {
    Card(
        Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
    ) {
        Column(
            Modifier.fillMaxWidth().padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            if (next.isEmpty()) {
                Text("خبر دیگری تا پایان این هفته نیست", style = MaterialTheme.typography.bodyLarge)
                return@Column
            }
            val t = next.first().epochMillis
            Text("خبر بعدی", style = MaterialTheme.typography.labelLarge)
            Text(countdown(t - now), fontSize = 38.sp, fontWeight = FontWeight.Bold)
            Text(dayTimeFmt(t, zone), style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(6.dp))
            next.forEach { e ->
                Text("${dot(e)} ${e.country} — ${e.title}", style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

@Composable
private fun EventRow(e: NewsEvent, now: Long, zone: ZoneId) {
    val past = e.epochMillis < now
    Card(Modifier.fillMaxWidth().alpha(if (past) 0.45f else 1f)) {
        Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
            Box(Modifier.width(6.dp).fillMaxHeight().background(impactColor(e)))
            Column(Modifier.weight(1f).padding(10.dp)) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(timeFmt(e.epochMillis, zone), fontWeight = FontWeight.Bold)
                    Spacer(Modifier.width(10.dp))
                    Text(e.country, fontWeight = FontWeight.Bold, color = impactColor(e))
                    Spacer(Modifier.weight(1f))
                    if (!past && e.epochMillis - now < 24 * 3_600_000L) {
                        Text(countdown(e.epochMillis - now), style = MaterialTheme.typography.labelMedium)
                    }
                }
                Text(e.title, style = MaterialTheme.typography.bodyMedium)
                if (e.forecast.isNotBlank() || e.previous.isNotBlank()) {
                    Text(
                        "پیش‌بینی: ${e.forecast.ifBlank { "—" }}     قبلی: ${e.previous.ifBlank { "—" }}",
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        }
    }
}

@Composable
private fun SettingsCard(s: Settings, onChange: (Settings) -> Unit, onTest: () -> Unit) {
    var open by rememberSaveable { mutableStateOf(false) }
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("فیلترها و هشدارها", Modifier.weight(1f), fontWeight = FontWeight.Bold)
                TextButton(onClick = { open = !open }) { Text(if (open) "بستن" else "بیشتر") }
            }
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                FilterChip(
                    selected = s.showRed,
                    onClick = { onChange(s.copy(showRed = !s.showRed)) },
                    label = { Text("🔴 قرمز") },
                )
                FilterChip(
                    selected = s.showOrange,
                    onClick = { onChange(s.copy(showOrange = !s.showOrange)) },
                    label = { Text("🟠 نارنجی") },
                )
            }
            if (open) {
                SectionLabel("ارزها")
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    ALL_CURRENCIES.forEach { c ->
                        val on = c in s.currencies
                        FilterChip(
                            selected = on,
                            onClick = { onChange(s.copy(currencies = if (on) s.currencies - c else s.currencies + c)) },
                            label = { Text(c) },
                        )
                    }
                }
                SectionLabel("منطقه زمانی نمایش")
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    ZONES.forEach { (id, name) ->
                        FilterChip(
                            selected = s.zoneId == id,
                            onClick = { onChange(s.copy(zoneId = id)) },
                            label = { Text(name) },
                        )
                    }
                }
                SectionLabel("هشدار برای")
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    FilterChip(
                        selected = s.alertRed,
                        onClick = { onChange(s.copy(alertRed = !s.alertRed)) },
                        label = { Text("🔴 خبرهای قرمز") },
                    )
                    FilterChip(
                        selected = s.alertOrange,
                        onClick = { onChange(s.copy(alertOrange = !s.alertOrange)) },
                        label = { Text("🟠 خبرهای نارنجی") },
                    )
                }
                Stepper("یادآوری اول (دقیقه قبل)", s.warnMinutes, step = 5, max = 120) {
                    onChange(s.copy(warnMinutes = it))
                }
                Stepper("هشدار صوتی (دقیقه قبل)", s.soundMinutes, step = 1, max = 30) {
                    onChange(s.copy(soundMinutes = it))
                }
                Text(
                    "صفر = غیرفعال. صدای هشدار حدود ۱۰ ثانیه پخش می‌شود.",
                    style = MaterialTheme.typography.bodySmall,
                )
                OutlinedButton(onClick = onTest) { Text("تست صدای هشدار") }
            }
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text,
        modifier = Modifier.padding(top = 6.dp),
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
    )
}

@Composable
private fun Stepper(label: String, value: Int, step: Int, max: Int, onValue: (Int) -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(label, Modifier.weight(1f))
        TextButton(onClick = { onValue((value - step).coerceIn(0, max)) }) { Text("−", fontSize = 20.sp) }
        Text("$value", fontWeight = FontWeight.Bold, modifier = Modifier.width(36.dp))
        TextButton(onClick = { onValue((value + step).coerceIn(0, max)) }) { Text("+", fontSize = 20.sp) }
    }
}
