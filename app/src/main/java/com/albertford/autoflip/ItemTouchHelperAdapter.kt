package com.albertford.autoflip

interface ItemTouchHelperAdapter {
    fun onItemDismiss(position: Int)
    fun isSwipeEnabled(): Boolean
}