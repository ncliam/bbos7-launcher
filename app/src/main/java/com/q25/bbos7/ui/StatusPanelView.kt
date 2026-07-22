package com.q25.bbos7.ui

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioManager
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.q25.bbos7.R
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Vung dinh man hinh kieu BBOS7: dong ho lon o giua, icon ho so am thanh va
 * tim kiem tran sat hai mep.
 *
 * Ngay thang, nha mang, song va pin da bo khoi day — status bar cua Android
 * luon hien nen no da lo phan do, bay hai lan chi ton cho.
 */
class StatusPanelView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : FrameLayout(context, attrs, defStyleAttr) {

    /** Bam vao icon kinh lup. */
    var onSearchClick: (() -> Unit)? = null

    private val clock: TextView
    private val ringerButton: ImageView

    private val audio = context.getSystemService(AudioManager::class.java)
    private val timeFormat = SimpleDateFormat("HH:mm", VI)

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) = refresh()
    }

    init {
        setBackgroundResource(R.drawable.status_scrim)
        LayoutInflater.from(context).inflate(R.layout.view_status_panel, this, true)

        clock = findViewById(R.id.clock)
        ringerButton = findViewById(R.id.ringer_button)

        findViewById<View>(R.id.search_button).setOnClickListener { onSearchClick?.invoke() }
        ringerButton.setOnClickListener { cycleRinger() }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_TIME_TICK)
            addAction(Intent.ACTION_TIME_CHANGED)
            addAction(Intent.ACTION_TIMEZONE_CHANGED)
            addAction(AudioManager.RINGER_MODE_CHANGED_ACTION)
        }
        ContextCompat.registerReceiver(
            context, receiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED
        )
        refresh()
    }

    override fun onDetachedFromWindow() {
        runCatching { context.unregisterReceiver(receiver) }
        super.onDetachedFromWindow()
    }

    fun refresh() {
        clock.text = timeFormat.format(Date())
        ringerButton.setImageResource(ringerIconRes())
    }

    /**
     * Doi giua Chuong va Rung. Khong dung che do Im lang: tu Android 6 no doi
     * quyen truy cap Do Not Disturb, khong dang de xin chi cho mot nut tat.
     */
    private fun cycleRinger() {
        val am = audio ?: return
        am.ringerMode = when (am.ringerMode) {
            AudioManager.RINGER_MODE_NORMAL -> AudioManager.RINGER_MODE_VIBRATE
            else -> AudioManager.RINGER_MODE_NORMAL
        }
        refresh()
    }

    private fun ringerIconRes(): Int = when (audio?.ringerMode) {
        AudioManager.RINGER_MODE_SILENT -> R.drawable.ic_ringer_silent
        AudioManager.RINGER_MODE_VIBRATE -> R.drawable.ic_ringer_vibrate
        else -> R.drawable.ic_ringer_normal
    }

    private companion object {
        val VI: Locale = Locale("vi", "VN")
    }
}
