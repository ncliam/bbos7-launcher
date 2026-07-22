package com.q25.bbos7.data

import android.content.Context

/**
 * Nguoi dung tu quyet app nao thuoc tab nao, de len tren ket qua phan loai tu dong.
 *
 * Phai ghi nho ca HAI chieu chu khong chi mot danh sach "da them": phan loai tu dong cung co luc
 * xep nham (mot app doc sach bi coi la giai tri chang han), nen can go ra duoc. Neu chi luu chieu
 * them thi lan sau mo may no lai tu chui vao cho cu.
 *
 * Khong luu gi khi lua chon cua nguoi dung trung voi ket qua tu dong — nho vay danh sach tu dong
 * co the mo rong sau nay ma khong bi cac ban ghi thua chan mat.
 */
class TabOverrides(context: Context) {

    private val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    /** true/false neu nguoi dung da quyet, null neu de mac phan loai tu dong. */
    fun stateOf(entry: AppEntry, tab: AppTab): Boolean? = when (entry.key) {
        in read(tab, ADDED) -> true
        in read(tab, REMOVED) -> false
        else -> null
    }

    fun set(entry: AppEntry, tab: AppTab, member: Boolean, auto: Boolean) {
        val added = read(tab, ADDED).toMutableSet()
        val removed = read(tab, REMOVED).toMutableSet()
        added -= entry.key
        removed -= entry.key

        // Chi ghi khi khac voi ket qua tu dong; trung thi de trong cho tu dong lo.
        if (member != auto) {
            if (member) added += entry.key else removed += entry.key
        }

        prefs.edit()
            .putStringSet(key(tab, ADDED), added)
            .putStringSet(key(tab, REMOVED), removed)
            .apply()
    }

    private fun read(tab: AppTab, suffix: String): Set<String> =
        prefs.getStringSet(key(tab, suffix), emptySet()) ?: emptySet()

    private fun key(tab: AppTab, suffix: String) = "${tab.name}.$suffix"

    private companion object {
        const val PREFS = "tab_overrides"
        const val ADDED = "added"
        const val REMOVED = "removed"
    }
}
