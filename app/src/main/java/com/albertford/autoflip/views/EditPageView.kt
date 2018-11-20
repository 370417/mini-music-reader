package com.albertford.autoflip.views

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.*
import android.support.v4.content.ContextCompat
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import com.albertford.autoflip.R
import com.albertford.autoflip.room.BarLine
import com.albertford.autoflip.room.Page

private val identityMatrix = Matrix()

class EditPageView(context: Context, attrs: AttributeSet) : View(context, attrs) {
    var bitmap: Bitmap? = null
        set(value) {
            field = value
            invalidate()
        }

    var page: Page? = null
    var selection: BarLine? = null

    private var motion: Motion? = null

    private val slop = ViewConfiguration.get(context).scaledTouchSlop

    private val firstSelectionFill = Paint()
    private val firstSelectionStroke = Paint()

    init {
        firstSelectionFill.color = ContextCompat.getColor(context, R.color.colorAccent)
        firstSelectionFill.alpha = 100

        firstSelectionStroke.color = ContextCompat.getColor(context, R.color.colorPrimary)
        firstSelectionStroke.style = Paint.Style.STROKE
        firstSelectionStroke.strokeWidth = 2f
    }

    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas) // draws the background
        canvas ?: return

        val bitmap = bitmap ?: return
        canvas.drawBitmap(bitmap, identityMatrix, null)

        val page = page ?: return
        val motion = motion
        if (page.staves.isEmpty()) {
            if (motion is FirstBarDrag) {
                val rect = motion.rect(this)
                canvas.drawRect(rect, firstSelectionFill)
                canvas.drawRect(rect, firstSelectionStroke)
            }
        } else {

        }
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent?): Boolean {
        val page = page ?: return super.onTouchEvent(event)
        return when (event?.action) {
            MotionEvent.ACTION_DOWN -> onActionDown(page, event)
            MotionEvent.ACTION_MOVE -> onActionMove(page, event)
            MotionEvent.ACTION_UP -> onActionUp(page, event)
            MotionEvent.ACTION_CANCEL -> onActionCancel(page, event)
            else -> super.onTouchEvent(event)
        }
    }

    private fun onActionDown(page: Page, event: MotionEvent): Boolean {
        val motion = motion
        return if (page.staves.isEmpty()) {
            if (motion == null) {
                this.motion = FirstBarTouch(Point(event, this))
                invalidate()
                true
            } else {
                false
            }
        } else {
            false
        }
    }

    private fun onActionMove(page: Page, event: MotionEvent): Boolean {
        val motion = motion
        return when (motion) {
            is FirstBarTouch -> {
                this.motion = FirstBarDrag(motion.origin, Point(event, this))
                invalidate()
                true
            }
            is FirstBarDrag -> {
                motion.drag = Point(event, this)
                invalidate()
                true
            }
            null -> false
        }
    }

    private fun onActionUp(page: Page, event: MotionEvent): Boolean {
        val motion = motion
        return when (motion) {
            null -> false
            is FirstBarDrag -> {
                true
            }
            is FirstBarTouch -> {
                this.motion = null
                true
            }
        }
    }

    private fun onActionCancel(page: Page, event: MotionEvent): Boolean {
        return true
    }
}

private class Point(val x: Float, val y: Float) {
    constructor(event: MotionEvent, view: EditPageView) : this(event.x / view.width, event.y / view.width)
}

/**
 * Represents ?
 */
private sealed class Motion

private class FirstBarTouch(val origin: Point) : Motion()

private class FirstBarDrag(val origin: Point, var drag: Point) : Motion() {
    fun rect(view: EditPageView): RectF {
        val left = Math.min(origin.x, drag.x) * view.width
        val top = Math.min(origin.y, drag.y) * view.width
        val right = Math.max(origin.x, drag.x) * view.width
        val bottom = Math.max(origin.y, drag.y) * view.width
        return RectF(left, top, right, bottom)
    }
}


// Initial bar selection
// inital bar corner resize
// initial bar edge resize

// once the inital bar is created, the bottom sheet buttons will pop up:
// Add bar / Add sheet (or Next Bar / Next Sheet for when the bar is not the current last bar)

// Use barlines and staff classes instead of bar classes?
