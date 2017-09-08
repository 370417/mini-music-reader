package com.albertford.autoflip

import android.content.Context
import android.graphics.Canvas
import android.util.AttributeSet
import android.view.MotionEvent
import android.widget.ImageView
import com.albertford.autoflip.models.PagePartition

class PartitionImageView(context: Context?, attrs: AttributeSet) : ImageView (context, attrs) {

    private var page = PagePartition()

    private var activePointerId = MotionEvent.INVALID_POINTER_ID
    private var lastTouchX = -1f
    private var lastTouchY = -1f
    private var firstTouchX = -1f
    private var firstTouchY = -1f

    val onLongClickListener = OnLongClickListener {
        false
    }

    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)
    }

    override fun onTouchEvent(event: MotionEvent?): Boolean {
        event ?: return super.onTouchEvent(event)
        when {
            page.selectedStaffIndex < 0 -> onTouchNoSelection(event)
            page.selectedBarIndex < 0 -> onTouchStaffSelected(event)
            else -> onTouchBarSelected(event)
        }
        return super.onTouchEvent(event)
    }

    private fun onTouchNoSelection(event: MotionEvent) {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                activePointerId = event.getPointerId(event.actionIndex)
                lastTouchX = event.x
                lastTouchY = event.y
                firstTouchX = event.x
                firstTouchY = event.y
                if (page.staves.isEmpty()) {
                    
                }
                val index = page.insertNewStaffIndex(firstTouchY)
                // check index and index - 1 for handles
            }
        }
    }

    private fun onTouchStaffSelected(event: MotionEvent) {}

    private fun onTouchBarSelected(event: MotionEvent) {}

}