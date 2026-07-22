package com.q25.bbos7.data

import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.Collator
import java.util.Locale

/**
 * Nguon du lieu duy nhat cho danh sach ung dung.
 *
 * Luu y: khoi <queries> trong AndroidManifest la bat buoc voi targetSdk 30+,
 * thieu no thi queryIntentActivities tra ve rong.
 */
class AppRepository(
    private val context: Context,
    private val scope: CoroutineScope,
) {
    private val pm: PackageManager = context.packageManager

    private val _apps = MutableStateFlow<List<AppEntry>>(emptyList())
    val apps: StateFlow<List<AppEntry>> = _apps.asStateFlow()

    private val counts = context.getSharedPreferences(PREFS_COUNTS, Context.MODE_PRIVATE)

    private val collator: Collator = Collator.getInstance(Locale("vi", "VN")).apply {
        strength = Collator.PRIMARY
    }

    private val packageReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            // App vua cai lai co the doi han icon, nen ban da dung de khong con dung nua.
            IconPack.clear()
            scope.launch { reload() }
        }
    }

    private var registered = false

    fun start() {
        if (!registered) {
            val filter = IntentFilter().apply {
                addAction(Intent.ACTION_PACKAGE_ADDED)
                addAction(Intent.ACTION_PACKAGE_REMOVED)
                addAction(Intent.ACTION_PACKAGE_CHANGED)
                addAction(Intent.ACTION_PACKAGE_REPLACED)
                addDataScheme("package")
            }
            // targetSdk 34+ bat buoc chi dinh co exported khi dang ky receiver.
            ContextCompat.registerReceiver(
                context, packageReceiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED
            )
            registered = true
        }
        scope.launch { reload() }
    }

    fun stop() {
        if (registered) {
            context.unregisterReceiver(packageReceiver)
            registered = false
        }
    }

    suspend fun reload() {
        val loaded = withContext(Dispatchers.IO) { queryLauncherApps() }
        _apps.value = loaded
    }

    private fun queryLauncherApps(): List<AppEntry> {
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        val resolved = pm.queryIntentActivities(intent, 0)
        val self = context.packageName
        return resolved.asSequence()
            .filter { it.activityInfo.packageName != self }
            .map { info ->
                val ai = info.activityInfo
                val component = ComponentName(ai.packageName, ai.name)
                AppEntry(
                    component = component,
                    label = info.loadLabel(pm).toString(),
                    icon = IconPack.iconFor(context, component, info.loadIcon(pm)),
                    isSystem = (ai.applicationInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0,
                    category = ai.applicationInfo.category,
                )
            }
            .sortedWith { a, b -> collator.compare(a.label, b.label) }
            .toList()
    }

    /**
     * Dem so lan mo app, luu cuc bo. Khong dung UsageStatsManager vi no doi
     * quyen dac biet — trai voi muc tieu rieng tu cua may nay.
     * Dot 2 (tab Frequent) se doc so lieu nay.
     */
    fun recordLaunch(entry: AppEntry) {
        val current = counts.getInt(entry.key, 0)
        counts.edit().putInt(entry.key, current + 1).apply()
    }

    fun launchCount(entry: AppEntry): Int = counts.getInt(entry.key, 0)

    private companion object {
        const val PREFS_COUNTS = "launch_counts"
    }
}
