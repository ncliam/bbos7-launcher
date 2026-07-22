package com.q25.bbos7.ui

import android.content.Context
import android.graphics.Typeface
import android.util.AttributeSet
import android.view.Gravity
import android.view.KeyEvent
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.q25.bbos7.data.AppTab
import com.q25.bbos7.R

/**
 * Thanh tab kieu BBOS7: dai xanh gradient, tab dang chon nam chinh giua,
 * hai tab ke tran ra hai mep va bi cat cut.
 *
 * Thanh nay khong tu cuon: no bam theo ViewPager2 ben duoi qua [syncTo], nen
 * chu tab truot theo ngon tay dung nhip voi luoi icon. Moi o rong bang nua
 * chieu ngang khung — nho vay o giua can giua man hinh con hai o canh lo ra
 * dung mot nua, giong het anh chup may that.
 */
class TabCarouselView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : FrameLayout(context, attrs, defStyleAttr) {

    /** Bam vao mot tab ben canh: bao vi tri ao de pager truot toi dung o do. */
    var onTabClicked: ((Int) -> Unit)? = null

    /** Bam trackpad khi thanh tab dang duoc focus: gap hoac mo panel. */
    var onToggleRequested: (() -> Unit)? = null

    /** Phim trai/phai khi thanh tab dang duoc focus: -1 lui, +1 tien. */
    var onTabStep: ((Int) -> Unit)? = null

    /**
     * Phim xuong khi thanh tab dang duoc focus. Phai tu xu ly: de Android tim
     * duong xuong luoi thi no khong tim ra, vi luoi nam sau hai lop
     * RecyclerView long nhau cua ViewPager2.
     */
    var onFocusDown: (() -> Boolean)? = null

    private val tabs = AppTab.entries
    private val recycler = RecyclerView(context)
    private val layoutManager = object : LinearLayoutManager(context, HORIZONTAL, false) {
        // Chi bam theo pager, khong cho nguoi dung keo truc tiep.
        override fun canScrollHorizontally(): Boolean = false
    }

    private var itemWidth = 0
    private var pendingPosition = -1
    private var pendingOffset = 0f

    init {
        setBackgroundResource(R.drawable.tab_carousel_background)
        recycler.layoutManager = layoutManager
        recycler.adapter = Adapter()
        recycler.clipToPadding = false
        recycler.overScrollMode = View.OVER_SCROLL_NEVER
        addView(recycler, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT))

        // Ca thanh tab la mot diem dung cua trackpad: focus vao day roi bam
        // de gap/mo panel, hoac trai/phai de doi tab. Chan focus xuong tung
        // chu tab — cham tay van bam duoc vi day chi chan focus.
        isFocusable = true
        isClickable = true
        descendantFocusability = FOCUS_BLOCK_DESCENDANTS
        setOnClickListener { onToggleRequested?.invoke() }
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean = when (keyCode) {
        KeyEvent.KEYCODE_DPAD_LEFT -> {
            onTabStep?.invoke(-1)
            true
        }

        KeyEvent.KEYCODE_DPAD_RIGHT -> {
            onTabStep?.invoke(1)
            true
        }

        KeyEvent.KEYCODE_DPAD_DOWN -> onFocusDown?.invoke() ?: false

        else -> super.onKeyDown(keyCode, event)
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        if (w == 0) return
        itemWidth = w / 2
        val sidePadding = (w - itemWidth) / 2
        recycler.setPadding(sidePadding, 0, sidePadding, 0)
        recycler.adapter?.notifyDataSetChanged()
        if (pendingPosition >= 0) recycler.post { syncTo(pendingPosition, pendingOffset) }
    }

    /**
     * Dat vi tri theo pager. [offset] trong khoang 0..1 la phan da truot sang
     * trang ke tiep, nho no ma chu tab di chuyen lien tuc theo ngon tay.
     */
    fun syncTo(position: Int, offset: Float) {
        pendingPosition = position
        pendingOffset = offset
        if (itemWidth == 0) return
        layoutManager.scrollToPositionWithOffset(position, -(offset * itemWidth).toInt())
        updateSelection(position, offset)
    }

    private fun updateSelection(position: Int, offset: Float) {
        // Qua nua duong thi coi nhu trang ke tiep da la trang dang chon.
        val active = if (offset > 0.5f) position + 1 else position
        for (i in 0 until recycler.childCount) {
            val child = recycler.getChildAt(i)
            (child as? TextView)?.applyStyle(layoutManager.getPosition(child) == active)
        }
    }

    private fun TextView.applyStyle(isSelected: Boolean) {
        setTypeface(null, if (isSelected) Typeface.BOLD else Typeface.NORMAL)
        alpha = if (isSelected) 1f else 0.7f
    }

    private inner class Adapter : RecyclerView.Adapter<Holder>() {

        override fun getItemCount(): Int = VIRTUAL_COUNT

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
            val view = TextView(parent.context).apply {
                layoutParams = RecyclerView.LayoutParams(
                    if (itemWidth > 0) itemWidth else ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.MATCH_PARENT,
                )
                gravity = Gravity.CENTER
                maxLines = 1
                textSize = 15f
                setTextColor(0xFFFFFFFF.toInt())
                setShadowLayer(3f, 0f, 1f, 0xCC000000.toInt())
            }
            return Holder(view)
        }

        override fun onBindViewHolder(holder: Holder, position: Int) {
            val view = holder.itemView as TextView
            view.layoutParams = view.layoutParams.apply {
                if (itemWidth > 0) width = itemWidth
            }
            view.text = view.context.getString(tabs[position.mod(tabs.size)].titleRes)
            view.setOnClickListener { onTabClicked?.invoke(position) }
        }
    }

    private class Holder(view: View) : RecyclerView.ViewHolder(view)

    companion object {
        /** Du lon de nguoi dung khong bao gio cham day khi cuon vong. */
        const val VIRTUAL_COUNT = 5000

        /** Vi tri ao ung voi [tab], nam giua danh sach de cuon duoc ca hai huong. */
        fun centerPositionFor(tab: AppTab): Int {
            val size = AppTab.entries.size
            val base = VIRTUAL_COUNT / 2
            return base - base.mod(size) + tab.ordinal
        }
    }
}
