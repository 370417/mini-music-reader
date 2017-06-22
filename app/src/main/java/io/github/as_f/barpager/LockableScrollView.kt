package io.github.as_f.barpager

import android.content.Context
import android.support.v4.widget.NestedScrollView
import android.util.AttributeSet
import android.view.MotionEvent

class LockableScrollView(context: Context?, attrs: AttributeSet?) : NestedScrollView(context, attrs) {
  override fun onInterceptTouchEvent(ev: MotionEvent?): Boolean {
    return false
  }
}