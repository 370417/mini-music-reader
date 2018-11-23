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

    private val pixelSlop = ViewConfiguration.get(context).scaledTouchSlop
    private var slop = 0f

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

        canvas.save()
        canvas.scale(width.toFloat(), width.toFloat())

        val motion = motion
        if (page.staves.isEmpty()) {
            if (motion is FirstBarSelection) {
                val rect = motion.rect()
                canvas.drawRect(rect, firstSelectionFill)
                canvas.drawRect(rect, firstSelectionStroke)
            }
        } else {

        }

        canvas.restore()
    }

    // Make sure pixel values are scaled relative to view width so that they are drawn correctly
    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        slop = pixelSlop.toFloat() / w
        firstSelectionStroke.strokeWidth /= w
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent?): Boolean {
        val page = page ?: return super.onTouchEvent(event)
        event ?: return false
        val touch = PointF(event.x / width, event.y / width)
        return when (event.action) {
            MotionEvent.ACTION_DOWN -> onActionDown(page, touch)
            MotionEvent.ACTION_MOVE -> onActionMove(page, touch)
            MotionEvent.ACTION_UP -> onActionUp(page, touch)
            MotionEvent.ACTION_CANCEL -> onActionUp(page, touch)
            else -> super.onTouchEvent(event)
        }
    }

    private fun onActionDown(page: Page, touch: PointF): Boolean {
        val motion = motion
        return if (page.staves.isEmpty()) {
            when (motion) {
                is FirstBarSelection -> {
                    val newMotion = touchHandle(touch, motion.rect())
                    if (newMotion != null) {
                        this.motion = newMotion
                        true
                    } else {
                        false
                    }
                }
                null -> {
                    this.motion = FirstBarDrag(touch)
                    true
                }
                else -> false
            }
        } else {
            false
        }
    }

    private fun onActionMove(page: Page, touch: PointF): Boolean {
        val motion = motion
        return when (motion) {
            is FirstBarDrag -> {
                motion.drag = touch
                invalidate()
                true
            }
            is FirstBarVertical -> {
                motion.drag = touch.y
                invalidate()
                true
            }
            is FirstBarHorizontal -> {
                motion.drag = touch.x
                invalidate()
                true
            }
            is FirstBar -> false
            null -> false
        }
    }

    private fun onActionUp(page: Page, touch: PointF): Boolean {
        val motion = motion
        return when (motion) {
            is FirstBarDrag -> {
                this.motion = motion.release()
                true
            }
            is FirstBarSelection -> {
                this.motion = FirstBar(motion.rect())
                true
            }
            else -> false
        }
    }

    private fun touchHandle(touch: PointF, rect: RectF): Motion? {
        val vertical = touchHandlePair(touch.y, rect.top, rect.bottom)
        val horizontal = touchHandlePair(touch.x, rect.left, rect.right)
        return when {
            vertical != null && horizontal != null -> FirstBarDrag(vertical, horizontal)
            vertical != null -> FirstBarVertical(rect, vertical)
            horizontal != null -> FirstBarHorizontal(rect, horizontal)
            else -> null
        }
    }

    private fun touchHandlePair(touch: Float, low: Float, high: Float): HandlePair? {
        when {
            touch < low -> if (low - touch <= slop) {
                return HandlePair(low, high)
            }
            touch > high -> if (touch - high <= slop) {
                return HandlePair(high, low)
            }
            2 * touch < low + high -> if (touch - low <= slop) {
                return HandlePair(low, high)
            }
            high - touch <= slop -> {
                return HandlePair(high, low)
            }
        }
        return null
    }
}

private class HandlePair(val touched: Float, val notTouched: Float)

/**
 * Represents ?
 */
private sealed class Motion

private class FirstBarDrag(val origin: PointF, var drag: PointF) : Motion(), FirstBarSelection {
    constructor(origin: PointF) : this(origin, origin)

    constructor(vertical: HandlePair, horizontal: HandlePair) : this(
            PointF(horizontal.notTouched, vertical.notTouched),
            PointF(horizontal.touched, vertical.touched)
    )

    override fun rect(): RectF {
        val left = Math.min(origin.x, drag.x)
        val top = Math.min(origin.y, drag.y)
        val right = Math.max(origin.x, drag.x)
        val bottom = Math.max(origin.y, drag.y)
        return RectF(left, top, right, bottom)
    }

    fun release(): FirstBar? {
        return if (origin == drag) {
            null
        } else {
            FirstBar(rect())
        }
    }
}

private class FirstBar(private val selection: RectF) : Motion(), FirstBarSelection {
    override fun rect() = selection
}

private class FirstBarVertical(val left: Float, val right: Float, val origin: Float, var drag: Float) : Motion(), FirstBarSelection {
    constructor(rect: RectF, handlePair: HandlePair) : this(rect.left, rect.right, handlePair.notTouched, handlePair.touched)

    override fun rect(): RectF {
        val top = Math.min(origin, drag)
        val bottom = Math.max(origin, drag)
        return RectF(left, top, right, bottom)
    }
}

private class FirstBarHorizontal(val top: Float, val bottom: Float, val origin: Float, var drag: Float) : Motion(), FirstBarSelection {
    constructor(rect: RectF, handlePair: HandlePair) : this(rect.top, rect.bottom, handlePair.notTouched, handlePair.touched)

    override fun rect(): RectF {
        val left = Math.min(origin, drag)
        val right = Math.max(origin, drag)
        return RectF(left, top, right, bottom)
    }
}

interface FirstBarSelection {
    fun rect(): RectF
}
