package com.q25.bbos7.data

import android.content.Context
import android.content.Intent
import android.provider.AlarmClock
import android.provider.MediaStore
import android.provider.Settings

/**
 * Danh sach app ghim tren man hinh chinh, luu theo thu tu trong SharedPreferences.
 * Lan chay dau tien tu seed cac app loi tim thay tren may.
 */
class PinnedApps(context: Context) {

    private val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    /** Danh sach key (ComponentName.flattenToShortString) theo dung thu tu hien thi. */
    fun keys(): List<String> =
        prefs.getString(KEY_ORDER, null)
            ?.split(SEPARATOR)
            ?.filter { it.isNotBlank() }
            ?: emptyList()

    fun isPinned(entry: AppEntry): Boolean = entry.key in keys()

    fun pin(entry: AppEntry) {
        val current = keys()
        if (entry.key in current) return
        save(current + entry.key)
    }

    fun unpin(entry: AppEntry) {
        save(keys().filterNot { it == entry.key })
    }

    /** Sap xep [all] theo thu tu ghim, bo qua key tro toi app da go. */
    fun resolve(all: List<AppEntry>): List<AppEntry> {
        val byKey = all.associateBy { it.key }
        return keys().mapNotNull { byKey[it] }
    }

    /**
     * Seed lan dau: tim app loi qua intent tuong ung, bo qua cai nao khong co.
     * Chi chay khi chua tung ghi gi — nguoi dung go het icon roi thi khong seed lai.
     */
    fun seedIfEmpty(context: Context, all: List<AppEntry>) {
        if (prefs.contains(KEY_ORDER) || all.isEmpty()) return

        val byPackage = all.groupBy { it.component.packageName }
        val seeded = coreIntents(context)
            .mapNotNull { intent ->
                val pkg = context.packageManager
                    .resolveActivity(intent, 0)
                    ?.activityInfo
                    ?.packageName
                    ?: return@mapNotNull null
                // Activity xu ly intent thuong khac activity launcher cua cung app,
                // nen doi chieu theo package roi lay entry launcher.
                byPackage[pkg]?.firstOrNull()?.key
            }
            .distinct()

        save(seeded)
    }

    private fun save(keys: List<String>) {
        prefs.edit().putString(KEY_ORDER, keys.joinToString(SEPARATOR)).apply()
    }

    private fun coreIntents(context: Context): List<Intent> = listOf(
        Intent(Intent.ACTION_DIAL),
        Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_APP_MESSAGING),
        Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_APP_BROWSER),
        Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_APP_CONTACTS),
        Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_APP_CALENDAR),
        Intent(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA),
        Intent(AlarmClock.ACTION_SHOW_ALARMS),
        Intent(Settings.ACTION_SETTINGS),
    )

    private companion object {
        const val PREFS = "pinned_apps"
        const val KEY_ORDER = "order"
        const val SEPARATOR = "\n"
    }
}
