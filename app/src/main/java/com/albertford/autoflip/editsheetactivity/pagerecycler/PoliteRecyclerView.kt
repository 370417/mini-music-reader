package com.albertford.autoflip.editsheetactivity.pagerecycler

import android.content.Context
import android.support.v7.widget.RecyclerView
import android.util.AttributeSet
import android.view.MotionEvent

/**
 * A RecyclerView that only intercepts touch events if multiple pointers are used.
 *
 * This means:
 *  - nested views can still receive move events
 *  - nested views do not have to worry about multiple pointers
 *  - the user can still always scroll the recyclerview by using two fingers
 */
class PoliteRecyclerView(context: Context, attrs: AttributeSet?) : RecyclerView(context, attrs) {

    override fun onInterceptTouchEvent(e: MotionEvent?): Boolean {
        e ?: return false
        return if (e.pointerCount == 1) {
            false
        } else {
            super.onInterceptTouchEvent(e)
        }
    }
}