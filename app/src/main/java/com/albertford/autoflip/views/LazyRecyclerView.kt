package com.albertford.autoflip.views

import android.content.Context
import android.support.v7.widget.RecyclerView
import android.util.AttributeSet

/**
 * Custom recycler view that tells its adapter when it has stopped scrolling.
 * This is so that the adapter can defer loading images until after scolling
 */
class LazyRecyclerView(context: Context, attrs: AttributeSet) : RecyclerView(context, attrs) {
    override fun onScrollStateChanged(state: Int) {
        super.onScrollStateChanged(state)
        val adapter = adapter
        if (adapter is LazyAdapter) {
            adapter.onScrollStateChanged(state)
        }
    }
}

interface LazyAdapter {
    fun onScrollStateChanged(state: Int)
}
