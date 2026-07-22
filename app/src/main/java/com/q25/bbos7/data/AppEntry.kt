package com.q25.bbos7.data

import android.content.ComponentName
import android.graphics.drawable.Drawable

/** Mot ung dung co launcher activity, da load san nhan va icon. */
data class AppEntry(
    val component: ComponentName,
    val label: String,
    val icon: Drawable,
    val isSystem: Boolean,
    /**
     * [android.content.pm.ApplicationInfo.category] do chinh app khai bao, hoac
     * [android.content.pm.ApplicationInfo.CATEGORY_UNDEFINED] neu app khong khai gi.
     */
    val category: Int,
) {
    /** Khoa on dinh dung lam id cho RecyclerView va lam key trong SharedPreferences. */
    val key: String get() = component.flattenToShortString()
}
