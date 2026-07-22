package com.q25.bbos7.ui

import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.q25.bbos7.data.AppEntry
import com.q25.bbos7.data.AppTab

/**
 * Moi trang cua pager la mot luoi icon cua mot tab. Danh sach dai gia
 * [TabCarouselView.VIRTUAL_COUNT] de vuot ngang cuon vong duoc mai, vi tri
 * that lay bang position.mod(so tab).
 */
class TabPagerAdapter(
    private val columns: Int,
    private val itemsFor: (AppTab) -> List<AppEntry>,
    private val onClick: (AppEntry, View) -> Unit,
    private val onLongClick: (AppEntry, View) -> Unit,
    private val onHighlight: (AppEntry?) -> Unit,
    private val onScrolled: () -> Unit,
) : RecyclerView.Adapter<TabPagerAdapter.PageHolder>() {

    private val tabs = AppTab.entries

    /** Luoi cua trang dang hien, de tray biet danh sach da cuon toi dinh chua. */
    var currentGrid: RecyclerView? = null
        private set

    override fun getItemCount(): Int = TabCarouselView.VIRTUAL_COUNT

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PageHolder {
        val grid = RecyclerView(parent.context).apply {
            layoutParams = RecyclerView.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT,
            )
            layoutManager = GridLayoutManager(parent.context, columns)
            clipToPadding = false
            isVerticalScrollBarEnabled = false
            overScrollMode = View.OVER_SCROLL_NEVER
            adapter = AppGridAdapter(onClick, onLongClick, onHighlight)
            // RecyclerView von focusable, nen no nuot focus va ca khoi luoi
            // sang len thay vi mot icon. Tat di de focus roi thang vao tung o.
            isFocusable = false
            descendantFocusability = ViewGroup.FOCUS_AFTER_DESCENDANTS
            // Cuon bang tay cung phai mo khoa focus cho nhung o vua duoc keo vao man hinh.
            addOnScrollListener(object : RecyclerView.OnScrollListener() {
                override fun onScrolled(view: RecyclerView, dx: Int, dy: Int) =
                    this@TabPagerAdapter.onScrolled()
            })
        }
        return PageHolder(grid)
    }

    override fun onBindViewHolder(holder: PageHolder, position: Int) {
        val tab = tabs[position.mod(tabs.size)]
        holder.grid.tag = tab
        holder.gridAdapter.submitList(itemsFor(tab))
    }

    /** Goi khi danh sach app hoac muc ghim doi: nap lai moi trang dang song. */
    fun refreshVisiblePages(pager: RecyclerView) {
        for (i in 0 until pager.childCount) {
            val page = pager.getChildAt(i) as? RecyclerView ?: continue
            val tab = page.tag as? AppTab ?: continue
            (page.adapter as? AppGridAdapter)?.submitList(itemsFor(tab))
        }
    }

    fun setCurrentGrid(grid: RecyclerView?) {
        currentGrid = grid
    }

    class PageHolder(val grid: RecyclerView) : RecyclerView.ViewHolder(grid) {
        val gridAdapter: AppGridAdapter get() = grid.adapter as AppGridAdapter
    }
}
