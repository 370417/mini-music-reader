package com.albertford.autoflip

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.support.v4.content.ContextCompat
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.ViewConfiguration
import android.widget.ImageView
import com.albertford.autoflip.models.BarLine
import com.albertford.autoflip.models.Page
import io.reactivex.Completable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.functions.Action
import io.reactivex.schedulers.Schedulers
import java.util.concurrent.TimeUnit

class PartitionImageView(context: Context?, attrs: AttributeSet) : ImageView (context, attrs) {

    companion object {
        private val lightestOverlay = 64
        private val lightestOverlayPaint = makePaint(lightestOverlay, 0, 0, 0)
    }

    var allowTouch = false

    var slideOffset = 0f
        set(value) {
            field = value
            barOverlayPaint.alpha = Math.round(lightestOverlay * value)
            invalidate()
        }

    var onSelectBarListener: (() -> Unit)? = null

    private var page = Page(0, 1f)

    private var clickOrigin: ClickOrigin? = null

    private val touchSlop = ViewConfiguration.get(context).scaledTouchSlop
    private val longClickDuration = ViewConfiguration.getLongPressTimeout().toLong()

    var longClickSubscription: Disposable? = null

    private val barOverlayPaint = makePaint(0, 0, 0, 0)
    private val accentPaint = getColorPaint(R.color.colorAccent)

    private val whiteLinePaint = {
        val paint = Paint()
        paint.setARGB(255, 255, 255, 255)
        paint.strokeWidth = 2f
        paint
    }()

    private val accentLinePaint = {
        val paint = Paint(accentPaint)
        paint.strokeWidth = 2f
        paint
    }()

    private val onLongClickListener = Action {
        val click = clickOrigin
        if (click is StaffSelectedClick) {
            val staff = page.staves.last()
            staff.bars.sort()
            val index = -staff.bars.binarySearch(BarLine(click.x)) - 1
            if (index > 0 && index < staff.bars.size) {
                clickOrigin = null
                page.selectedBarIndex = index - 1
                onSelectBarListener?.invoke()
            }
        }
    }

    override fun onFinishInflate() {
        super.onFinishInflate()
        page.scale = width.toFloat()
    }

    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)
        when {
            !page.staffSelected -> {
                canvas?.drawRect(0f, 0f, width.toFloat(), height.toFloat(), lightestOverlayPaint)
            }
            page.selectedBarIndex < 0 -> {
                val staff = page.staves.last()
                canvas?.drawRect(0f, 0f, width.toFloat(), staff.start, lightestOverlayPaint)
                canvas?.drawRect(0f, staff.end, width.toFloat(), height.toFloat(), lightestOverlayPaint)
            }
            else -> {
                val staff = page.staves.last()
                canvas?.drawRect(0f, 0f, width.toFloat(), staff.start, lightestOverlayPaint)
                canvas?.drawRect(0f, staff.end, width.toFloat(), height.toFloat(), lightestOverlayPaint)
                val firstBar = staff.bars[page.selectedBarIndex]
                val secondBar = staff.bars[page.selectedBarIndex + 1]
                canvas?.drawRect(0f, staff.start, firstBar.x, staff.end, barOverlayPaint)
                canvas?.drawRect(secondBar.x, staff.start, width.toFloat(), staff.end, barOverlayPaint)
            }
        }
        for (staffIndex in page.staves.indices) {
            val staff = page.staves[staffIndex]
            val barPaint = if (page.staffSelected && staffIndex == page.staves.size - 1) {
                accentLinePaint
            } else {
                whiteLinePaint
            }
            canvas?.drawLine(0f, staff.start, width.toFloat(), staff.start, whiteLinePaint)
            canvas?.drawLine(0f, staff.end, width.toFloat(), staff.end, whiteLinePaint)
            for (bar in staff.bars) {
                canvas?.drawLine(bar.x, staff.start, bar.x, staff.end, barPaint)
            }
        }
    }

    override fun onTouchEvent(event: MotionEvent?): Boolean {
        if (!allowTouch) {
            return true
        }
        when (event?.action) {
            MotionEvent.ACTION_DOWN -> {
                clickOrigin = when {
                    !page.staffSelected -> PageSelectedClick(event.y)
                    page.selectedBarIndex < 0 -> onTouchStaff(event)
                    else -> null
                }
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_POINTER_UP -> {
                when (clickOrigin) {
                    is StaffDeselected -> page.deselectStaff()
                    is StaffSelectedClick -> {
                        page.staves.last().bars.add(BarLine(event.x))
                    }
                }
                invalidate()
                clickOrigin = null
            }
            MotionEvent.ACTION_CANCEL -> {
                clickOrigin = null
            }
            MotionEvent.ACTION_MOVE -> {
                val origin = clickOrigin
                when (origin) {
                    is PageSelectedClick -> {
                        val handle = page.insertNewStaff(origin.y, event.y)
                        clickOrigin = StaffSelectedDrag(handle)
                    }
                    is StaffSelectedDrag -> {
                        val staff = page.staves.last()
                        if (origin.isStart) {
                            staff.start = event.y
                        } else {
                            staff.end = event.y
                        }
                        if (staff.reorder()) {
                            origin.isStart = !origin.isStart
                        }
                    }
                    is StaffSelectedClick -> {
                        page.staves.last().bars.add(BarLine(event.x))
                        clickOrigin = BarDrag()
                        val subscription = longClickSubscription
                        if (subscription != null && !subscription.isDisposed) {
                            subscription.dispose()
                        }
                    }
                    is BarDrag -> {
                        page.staves.last().bars.last().x = event.x
                    }
                }
                invalidate()
            }
        }
        return true
    }

    fun deselectBar() {
        page.selectedBarIndex = -1
    }

    private fun onTouchStaff(event: MotionEvent): ClickOrigin? {
        val staff = page.staves.last()
        return when {
            approxEqual(event.y, staff.start) -> StaffSelectedDrag(true)
            approxEqual(event.y, staff.end) -> StaffSelectedDrag(false)
            event.y < staff.start -> {
                page.deselectStaff()
                null
            }
            event.y > staff.end -> {
                page.deselectStaff()
                null
            }
            else -> {
                longClickSubscription = Completable
                        .timer(longClickDuration, TimeUnit.MILLISECONDS)
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .doOnComplete(onLongClickListener)
                        .subscribe()
                StaffSelectedClick(event.x)
            }
        }
    }

    private fun getColorPaint(colorId: Int): Paint {
        val paint = Paint()
        paint.color = ContextCompat.getColor(context, colorId)
        return paint
    }

    private fun approxEqual(a: Float, b: Float) = Math.abs(a - b) < touchSlop
}

private fun makePaint(a: Int, r: Int, g: Int, b: Int): Paint {
    val paint = Paint()
    paint.setARGB(a, r, g, b)
    return paint
}

private sealed class ClickOrigin

private class PageSelectedClick(var y: Float) : ClickOrigin()

private class StaffSelectedDrag(var isStart: Boolean) : ClickOrigin()

private class StaffDeselected : ClickOrigin()

private class StaffSelectedClick(var x: Float) : ClickOrigin()

private class BarDrag : ClickOrigin()
