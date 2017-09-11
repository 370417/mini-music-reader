package com.albertford.autoflip

import android.content.Context
import android.support.v4.widget.NestedScrollView
import android.util.AttributeSet
import android.view.MotionEvent

class LockableScrollView(context: Context?, attrs: AttributeSet?) : NestedScrollView(context,
        attrs) {
    var actionDownEvent: MotionEvent? = null

    override fun onInterceptTouchEvent(ev: MotionEvent?): Boolean {
        if (ev != null && ev.pointerCount > 1) {
            super.onTouchEvent(actionDownEvent)
            super.onInterceptTouchEvent(ev)
            return true
        } else {
            if (ev?.action == MotionEvent.ACTION_DOWN) {
                actionDownEvent = ev
            }
            return false
        }
    }
}