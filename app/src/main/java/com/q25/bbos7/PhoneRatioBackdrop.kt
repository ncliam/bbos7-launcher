package com.q25.bbos7

import android.app.Activity
import android.os.Bundle
import android.os.SystemClock
import android.view.MotionEvent
import android.view.WindowManager

/**
 * Nen den sau cua so ti le dien thoai, de hai ben khung la vien den nhu man ngoai Z Flip thay vi lo
 * launcher ra. Chay o task rieng, khong vao Gan day.
 *
 * Tu don minh trong hai truong hop:
 * - nguoi dung roi di (Home, Gan day): noHistory trong manifest lo, vi nen bi che va bi stop;
 * - app trong khung da dong: nen thanh activity tren cung — tru khi la do vua cham vao vien den.
 */
class PhoneRatioBackdrop : Activity() {

    private var topResumed = false
    private var lastTouch = 0L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Cham vao vien den khong duoc cuop focus: trackpad va ban phim phai o lai app trong khung.
        window.addFlags(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE)
    }

    override fun dispatchTouchEvent(event: MotionEvent): Boolean {
        lastTouch = SystemClock.uptimeMillis()
        return true
    }

    override fun onTopResumedActivityChanged(isTopResumedActivity: Boolean) {
        super.onTopResumedActivityChanged(isTopResumedActivity)
        topResumed = isTopResumedActivity
        if (!isTopResumedActivity) return
        // Cham vao vien den cung co the lam nen thanh top-resumed; su kien cham toi truoc hoac sau
        // mot nhip, nen doi het khoang do roi moi xet.
        val since = SystemClock.uptimeMillis()
        window.decorView.postDelayed({
            if (topResumed && lastTouch < since - TOUCH_GRACE_MS) finish()
        }, TOUCH_GRACE_MS)
    }

    private companion object {
        const val TOUCH_GRACE_MS = 400L
    }
}
