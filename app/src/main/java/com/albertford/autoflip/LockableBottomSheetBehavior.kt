package com.albertford.autoflip

import android.content.Context
import android.support.design.widget.BottomSheetBehavior
import android.support.design.widget.CoordinatorLayout
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View

/**
 * Behavior for a bottom sheet that cannot be dragged by touch.
 */

class LockableBottomSheetBehavior<V : View>(context: Context?, attrs: AttributeSet?) : BottomSheetBehavior<V>(context, attrs) {
    override fun onInterceptTouchEvent(parent: CoordinatorLayout?, child: V,
            event: MotionEvent?) = false

    override fun onTouchEvent(parent: CoordinatorLayout?, child: V, event: MotionEvent?) = false
}