package com.albertford.autoflip.views

import android.content.Context
import android.support.v4.widget.NestedScrollView
import android.util.AttributeSet
import android.view.MotionEvent

/**
 * A scroll view that requires two or more fingers to scroll.
 */

class LockableScrollView(context: Context, attrs: AttributeSet?) : NestedScrollView(context,
        attrs) {
    private var actionDownEvent: MotionEvent? = null

    override fun onInterceptTouchEvent(ev: MotionEvent?): Boolean {
        return if (ev != null && ev.pointerCount > 1) {
            if (ev.pointerCount == 2) {
                super.onTouchEvent(actionDownEvent)
            }
            super.onInterceptTouchEvent(ev)
            true
        } else {
            if (ev?.action == MotionEvent.ACTION_DOWN) {
                actionDownEvent = ev
            }
            false
        }
    }
}