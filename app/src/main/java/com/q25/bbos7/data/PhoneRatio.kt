package com.q25.bbos7.data

import android.app.ActivityOptions
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Rect
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.WindowInsets
import android.view.WindowMetrics

/**
 * Mo app dang "ti le dien thoai", giong nut tren man ngoai Z Flip: man Q25 vuong nen nhieu app
 * tuong minh dang nam ngang va vo layout. Cho app mot khung dung giua man — hep hon chieu cao mot
 * chut la app chuyen sang giao dien doc. Cham icon binh thuong luon mo toan man; che do nay chi
 * bat theo tung lan mo tu menu giu icon.
 *
 * Khong can adb, Tro nang hay Tuy chon nha phat trien (app ngan hang chan ca ba).
 */
object PhoneRatio {

    /**
     * Cach mo app. Hai kieu khung dung song song vi moi kieu vuong mot loi cua he thong:
     * - [FRAMED] (freeform): WM Shell gan them thanh tieu de (nut thu nho/phong to/dong);
     * - [BORDERLESS] (multi-window): khong co thanh tieu de, nhung Quickstep cua ROM Q25 coi moi
     *   task multi-window la chia doi man hinh va crash khi bam Da nhiem.
     */
    enum class Mode(val windowingMode: Int) {
        FULLSCREEN(1),
        FRAMED(5),
        BORDERLESS(6),
    }

    /** Ghi windowing mode vao Bundle can Android 11+ (WindowMetrics); Q25 la Android 14. */
    fun isSupported(context: Context, mode: Mode): Boolean = when {
        Build.VERSION.SDK_INT < Build.VERSION_CODES.R -> false
        mode != Mode.FRAMED -> true
        // Tat "Tuy chon nha phat trien" qua giao dien Settings se reset co freeform ve 0.
        else -> context.packageManager.hasSystemFeature(PackageManager.FEATURE_FREEFORM_WINDOW_MANAGEMENT) ||
            Settings.Global.getInt(context.contentResolver, FREEFORM_SETTING, 0) != 0
    }

    /**
     * Gan che do vao [options] va tra ve Bundle de truyen cho startActivity.
     *
     * Chi setLaunchBounds la chua du: man Q25 khong o che do freeform nen he thong van mo toan man.
     * Setter cua windowing mode la API an, nhung ActivityOptions doc mode tu khoa
     * [KEY_WINDOWING_MODE] trong Bundle nen ghi thang vao. Lan mo thuong ghi fullscreen: task moi se
     * toan man (task cu dang trong khung thi van o trong khung).
     */
    fun applyTo(options: ActivityOptions, mode: Mode, metrics: WindowMetrics): Bundle {
        if (mode != Mode.FULLSCREEN) options.setLaunchBounds(phoneBounds(metrics))
        return options.toBundle().apply { putInt(KEY_WINDOWING_MODE, mode.windowingMode) }
    }

    /**
     * Task da vao khung thi o luon trong khung: Android 14 khong cho dua task freeform/multi-window
     * ve fullscreen khi mo lai (ca CLEAR_TASK cung khong), chi vuot bo khoi Gan day moi het. Nen
     * phai nho app nao tung mo trong khung de lan cham icon sau van dung nen den sau no.
     */
    fun remember(context: Context, packageName: String) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val opened = prefs.getStringSet(KEY_OPENED, emptySet())!! + packageName
        prefs.edit().putStringSet(KEY_OPENED, opened).apply()
    }

    fun wasOpened(context: Context, packageName: String): Boolean =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getStringSet(KEY_OPENED, emptySet())!!
            .contains(packageName)

    private const val PREFS = "phone_ratio"
    private const val KEY_OPENED = "opened_packages"

    /** ActivityOptions.KEY_LAUNCH_WINDOWING_MODE (@hide); gia tri la WindowConfiguration.WINDOWING_MODE_*. */
    private const val KEY_WINDOWING_MODE = "android.activity.windowingMode"

    /** Settings.Global.DEVELOPMENT_ENABLE_FREEFORM_WINDOWS_SUPPORT — @hide nen ghi tay. */
    private const val FREEFORM_SETTING = "enable_freeform_support"

    /**
     * Chieu rong khung so voi chieu cao. Rong 600px tren cao 685px: van la man doc nen app dung
     * giao dien dien thoai, ma chu giu nguyen co. Rong bang chieu cao thi app lai coi la nam ngang.
     * Kieu FRAMED con mat ~60px cho thanh tieu de, phan app con 600x625 — van dung.
     */
    private const val WIDTH_TO_HEIGHT = 0.875f

    /**
     * Khung nam duoi thanh trang thai, can giua theo chieu ngang. Neu de top = 0, he thong tu day
     * cua so xuong duoi thanh trang thai va day mep duoi ra ngoai man.
     */
    private fun phoneBounds(metrics: WindowMetrics): Rect {
        val screen = metrics.bounds
        val insets = metrics.windowInsets.getInsetsIgnoringVisibility(
            WindowInsets.Type.statusBars() or WindowInsets.Type.displayCutout()
        )
        val top = screen.top + insets.top
        val width = ((screen.bottom - top) * WIDTH_TO_HEIGHT).toInt()
        val left = screen.left + (screen.width() - width) / 2
        return Rect(left, top, left + width, screen.bottom)
    }
}
