package com.q25.bbos7.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.q25.bbos7.R
import com.q25.bbos7.data.AppEntry

/**
 * Luoi icon khong nhan. Ten cua muc dang duoc chon bao ra ngoai qua
 * [onHighlight] de hien o mot dong rieng phia tren luoi, dung kieu BBOS7.
 */
class AppGridAdapter(
    private val onClick: (AppEntry, View) -> Unit,
    private val onLongClick: (AppEntry, View) -> Unit,
    private val onHighlight: (AppEntry?) -> Unit,
) : ListAdapter<AppEntry, AppGridAdapter.Holder>(DIFF) {

    init {
        setHasStableIds(true)
    }

    /**
     * So icon dau tien duoc phep nhan focus. Khi panel moi he ra mot hai dong,
     * trackpad khong duoc chay xuong nhung icon dang bi mep man hinh che.
     */
    var focusableLimit: Int = Int.MAX_VALUE
        set(value) {
            if (field == value) return
            field = value
            notifyItemRangeChanged(0, itemCount, PAYLOAD_FOCUSABLE)
        }

    override fun getItemId(position: Int): Long = getItem(position).key.hashCode().toLong()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_app, parent, false)
        return Holder(view)
    }

    override fun onBindViewHolder(holder: Holder, position: Int, payloads: MutableList<Any>) {
        if (payloads.contains(PAYLOAD_FOCUSABLE)) {
            holder.applyFocusable(position)
            return
        }
        super.onBindViewHolder(holder, position, payloads)
    }

    override fun onBindViewHolder(holder: Holder, position: Int) {
        val entry = getItem(position)
        holder.icon.setImageDrawable(entry.icon)
        holder.applyFocusable(position)
        holder.itemView.setOnClickListener { onClick(entry, holder.itemView) }
        holder.itemView.setOnLongClickListener {
            onLongClick(entry, holder.itemView)
            true
        }
        // Trackpad cua Q25 dieu khien focus. Khong bao theo cham tay: dong ten
        // an hien se lam luoi nhay len xuong moi lan dat ngon xuong.
        holder.itemView.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) onHighlight(entry)
        }
    }

    override fun onViewRecycled(holder: Holder) {
        holder.itemView.setOnFocusChangeListener(null)
        super.onViewRecycled(holder)
    }

    private fun Holder.applyFocusable(position: Int) {
        itemView.isFocusable = position < focusableLimit
    }

    class Holder(view: View) : RecyclerView.ViewHolder(view) {
        val icon: ImageView = view.findViewById(R.id.icon)
    }

    private companion object {
        const val PAYLOAD_FOCUSABLE = "focusable"

        val DIFF = object : DiffUtil.ItemCallback<AppEntry>() {
            override fun areItemsTheSame(old: AppEntry, new: AppEntry) = old.key == new.key
            override fun areContentsTheSame(old: AppEntry, new: AppEntry) = old.label == new.label
        }
    }
}
