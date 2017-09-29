package com.albertford.autoflip

import android.support.v7.widget.RecyclerView
import android.support.v7.widget.helper.ItemTouchHelper

class ItemTouchHelperCallback(private val adapter: ItemTouchHelperAdapter) : ItemTouchHelper.Callback() {

    override fun getMovementFlags(recyclerView: RecyclerView?,
            viewHolder: RecyclerView.ViewHolder?): Int =
            makeMovementFlags(0, ItemTouchHelper.START or ItemTouchHelper.END)

    override fun isLongPressDragEnabled(): Boolean = false

    override fun isItemViewSwipeEnabled(): Boolean = adapter.isSwipeEnabled()

    override fun onSwiped(viewHolder: RecyclerView.ViewHolder?, direction: Int) {
        viewHolder ?: return
        adapter.onItemDismiss(viewHolder.adapterPosition)
    }

    override fun onMove(recyclerView: RecyclerView?, viewHolder: RecyclerView.ViewHolder?,
            target: RecyclerView.ViewHolder?): Boolean = false
}
