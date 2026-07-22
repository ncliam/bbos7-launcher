package com.q25.bbos7.data

import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.provider.MediaStore
import com.q25.bbos7.R

/**
 * Cac tab trong panel. FAVORITES la tab mac dinh: o nac thu gon no chinh la
 * "man hinh chinh".
 *
 * Tab chi la loi tat chu khong phai chi muc day du — app nao khong roi vao nhom
 * nao thi van tim duoc o tab Tat ca hoac bang o tim kiem. Vi vay khong can co ep
 * moi app deu phai thuoc ve mot tab.
 */
enum class AppTab(val titleRes: Int) {
    ALL(R.string.tab_all),
    FAVORITES(R.string.tab_favorites),
    FINANCE(R.string.tab_finance),
    SOCIAL(R.string.tab_social),
    ENTERTAINMENT(R.string.tab_entertainment),
    FREQUENT(R.string.tab_frequent);

    companion object {
        /**
         * Cac tab nguoi dung tu sap xep duoc. ALL/FAVORITES/FREQUENT thi khong: mot cai la tat ca,
         * mot cai da co ghim lo, con mot cai la so lieu tu dem.
         */
        val assignable: List<AppTab> = listOf(FINANCE, SOCIAL, ENTERTAINMENT)
    }
}

/**
 * Phan loai app theo tab.
 *
 * Uu tien [ApplicationInfo.category] do chinh app khai bao, vi do la thu dung nhat
 * co the co. Nhung Android KHONG co category nao cho tai chinh, va rat nhieu app
 * Viet Nam khong khai bao gi ca — nen phai co them danh sach package va vai tu khoa
 * bu vao.
 */
class AppClassifier(context: Context) {

    /** App media do bang chinh intent category cua he thong thay vi doan theo ten package. */
    private val mediaPackages: Set<String> = buildSet {
        val pm = context.packageManager
        val intents = listOf(
            Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_APP_MUSIC),
            Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_APP_GALLERY),
            Intent(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA),
            Intent(MediaStore.INTENT_ACTION_VIDEO_CAMERA),
            Intent(Intent.ACTION_VIEW).setType("video/*"),
            Intent(Intent.ACTION_VIEW).setType("audio/*"),
            Intent(Intent.ACTION_VIEW).setType("image/*"),
        )
        for (intent in intents) {
            pm.queryIntentActivities(intent, 0).forEach { add(it.activityInfo.packageName) }
        }
    }

    fun filter(
        tab: AppTab,
        all: List<AppEntry>,
        pinned: PinnedApps,
        overrides: TabOverrides,
        launchCount: (AppEntry) -> Int,
    ): List<AppEntry> = when (tab) {
        AppTab.FAVORITES -> pinned.resolve(all)
        AppTab.ALL -> all
        AppTab.FREQUENT -> all.filter { launchCount(it) > 0 }
            .sortedByDescending { launchCount(it) }
            .take(FREQUENT_LIMIT)

        else -> all.filter { entry ->
            overrides.stateOf(entry, tab) ?: belongsAutomatically(entry, tab)
        }
    }

    /** Ket qua phan loai tu dong, chua tinh den lua chon cua nguoi dung. */
    fun belongsAutomatically(entry: AppEntry, tab: AppTab): Boolean = when (tab) {
        AppTab.FINANCE -> isFinance(entry)
        AppTab.SOCIAL -> isSocial(entry)
        AppTab.ENTERTAINMENT -> isEntertainment(entry)
        else -> false
    }

    /**
     * Ngan hang va vi dien tu. Khong co category he thong nao cho nhom nay nen phai
     * do bang ten package: danh sach cu the truoc, roi den vai tu khoa.
     */
    private fun isFinance(entry: AppEntry): Boolean {
        val pkg = entry.component.packageName.lowercase()
        return pkg in FINANCE_PACKAGES || FINANCE_KEYWORDS.any { pkg.contains(it) }
    }

    private fun isSocial(entry: AppEntry): Boolean =
        entry.category == ApplicationInfo.CATEGORY_SOCIAL ||
            entry.component.packageName.lowercase() in SOCIAL_PACKAGES

    /**
     * Gom ca game, nhac, phim va anh — tab nay thay cho ca "Da phuong tien" cu, nen
     * giu luon tap media do duoc bang intent.
     */
    private fun isEntertainment(entry: AppEntry): Boolean {
        if (entry.category in ENTERTAINMENT_CATEGORIES) return true
        val pkg = entry.component.packageName
        return pkg in mediaPackages || pkg.lowercase() in ENTERTAINMENT_PACKAGES
    }

    private companion object {
        const val FREQUENT_LIMIT = 24

        val ENTERTAINMENT_CATEGORIES = setOf(
            ApplicationInfo.CATEGORY_GAME,
            ApplicationInfo.CATEGORY_AUDIO,
            ApplicationInfo.CATEGORY_VIDEO,
            ApplicationInfo.CATEGORY_IMAGE,
        )

        /** Ngan hang / vi dien tu hay gap o Viet Nam. */
        val FINANCE_PACKAGES = setOf(
            "com.msb.digibank.retail",              // MSB
            "com.mbmobile",                         // MB Bank
            "com.vietinbank.ipay",                  // VietinBank
            "com.vcb",                              // Vietcombank
            "vn.com.techcombank.bb.app",            // Techcombank
            "com.bidv.smartbanking",                // BIDV
            "com.tpb.mb.gprsandroid",               // TPBank
            "com.vpbankonline",                     // VPBank
            "com.sacombank.ewallet",                // Sacombank
            "com.acb.mobile",                       // ACB
            "com.ocb.omniapp",                      // OCB
            "vn.com.vng.zalopay",                   // ZaloPay
            "com.mservice.momotransfer",            // MoMo
            "com.vttm.viettelpay",                  // Viettel Money
            "com.uob.mightyvn",                     // UOB
            "com.google.android.apps.walletnfcrel", // Google Wallet
        )

        /**
         * Chi dat nhung tu du dac trung. Khong dung "pay" tran lan: nhieu app chang
         * lien quan gi den tai chinh cung co chu do trong ten package.
         *
         * "bank" bat gan het ngan hang trong nuoc, nhung ngan hang ngoai thi ten
         * package thuong chi co thuong hieu (UOB la com.uob.mightyvn) nen phai ke rieng.
         */
        val FINANCE_KEYWORDS = listOf(
            "bank", "vnpay", "momo", "finance",
            "uob", "hsbc", "shinhan", "woori", "citi", "standardchartered",
        )

        /** Bu cho app khong khai bao CATEGORY_SOCIAL. */
        val SOCIAL_PACKAGES = setOf(
            "com.zing.zalo",
            "com.facebook.katana",
            "com.facebook.orca",
            "com.facebook.lite",
            "com.instagram.android",
            "com.whatsapp",
            "com.discord",
            "org.telegram.messenger",
            "com.viber.voip",
            "com.twitter.android",
            "com.zhiliaoapp.musically",              // TikTok
            "com.linkedin.android",
            "com.snapchat.android",
            "com.reddit.frontpage",
            "com.google.android.apps.tachyon",       // Google Meet
        )

        /** Bu cho app khong khai bao category giai tri. */
        val ENTERTAINMENT_PACKAGES = setOf(
            "com.google.android.youtube",
            "com.google.android.apps.youtube.music",
            "com.google.android.videos",
            "com.google.android.play.games",
            "com.zing.mp3",
            "com.spotify.music",
            "com.netflix.mediaclient",
            "com.tns.cotuongup",                     // Ky Vuong
            "com.android.fmradio",
            "org.readera",
        )
    }
}
