package com.q25.bbos7.data

import android.content.Context
import android.view.KeyEvent

/**
 * Phim tat mo app: Alt + mot chu cai, kieu "application shortcut keys" cua BBOS7.
 *
 * Gan theo MA PHIM chu khong theo ky tu ma phim do sinh ra. Tren Q25, Alt+D thuc chat cho ra so
 * '5' (xem /system/usr/keychars/Q25_keyboard.kcm) — nhung nguoi dung nhin xuong ban phim chi thay
 * chu D, nen nho "Alt+D mo Danh ba" moi la thu tu nhien.
 */
class Shortcuts(context: Context) {

    private val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    /** Ma phim da gan cho [entry], hoac null neu chua gan. */
    fun keyCodeOf(entry: AppEntry): Int? = prefs.all.entries
        .firstOrNull { it.value == entry.key }
        ?.key
        ?.removePrefix(KEY_PREFIX)
        ?.toIntOrNull()

    /** Khoa component gan voi [keyCode], hoac null. */
    fun componentKeyOf(keyCode: Int): String? = prefs.getString(KEY_PREFIX + keyCode, null)

    /**
     * Gan [keyCode] cho [entry]. Mot app chi giu mot phim, va mot phim chi mo mot app — nen phai
     * go ca hai chieu truoc khi ghi, neu khong se con lai ban ghi mo coi tro toi app cu.
     */
    fun assign(keyCode: Int, entry: AppEntry) {
        val editor = prefs.edit()
        keyCodeOf(entry)?.let { editor.remove(KEY_PREFIX + it) }
        editor.putString(KEY_PREFIX + keyCode, entry.key).apply()
    }

    fun clear(entry: AppEntry) {
        keyCodeOf(entry)?.let { prefs.edit().remove(KEY_PREFIX + it).apply() }
    }

    companion object {
        private const val PREFS = "shortcuts"
        private const val KEY_PREFIX = "key_"

        /** Chi nhan phim chu: phim so va ky hieu tren Q25 deu phai bam kem Alt san roi. */
        fun isLetterKey(keyCode: Int): Boolean =
            keyCode in KeyEvent.KEYCODE_A..KeyEvent.KEYCODE_Z

        /** "D" cho KEYCODE_D — de hien trong menu. */
        fun label(keyCode: Int): String = ('A' + (keyCode - KeyEvent.KEYCODE_A)).toString()
    }
}
