package com.albertford.autoflip

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.support.v4.content.ContextCompat
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.ViewConfiguration
import android.widget.ImageView
import com.albertford.autoflip.models.BarPartition
import com.albertford.autoflip.models.PagePartition
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

    var slideOffset = 0f

    private var page = PagePartition()

    private var clickOrigin: ClickOrigin? = null

    private val touchSlop = ViewConfiguration.get(context).scaledTouchSlop
    private val longClickDuration = ViewConfiguration.getLongPressTimeout().toLong()

    private var longClickSubscription: Disposable? = null

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
            val staff = page.getSelectedStaff()
            staff.bars.sort()
            val index = -staff.bars.binarySearch(BarPartition(click.x)) - 1
            if (index > 1 && index < staff.bars.size) {
                clickOrigin = null

            }
        }
    }

    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)
        when {
            page.selectedStaffIndex < 0 -> {
                canvas?.drawRect(0f, 0f, width.toFloat(), height.toFloat(), lightestOverlayPaint)
            }
            page.selectedBarIndex < 0 -> {
                val selectedStaff = page.getSelectedStaff()
                canvas?.drawRect(0f, 0f, width.toFloat(), selectedStaff.start, lightestOverlayPaint)
                canvas?.drawRect(0f, selectedStaff.end, width.toFloat(), height.toFloat(), lightestOverlayPaint)
            }
            else -> {}
        }
        for (staffIndex in page.staves.indices) {
            val staff = page.staves[staffIndex]
            if (staffIndex == page.selectedStaffIndex) {
                for (bar in staff.bars) {
                    canvas?.drawLine(bar.x, staff.start, bar.x, staff.end, accentLinePaint)
                }
            } else {
                canvas?.drawLine(0f, staff.start, width.toFloat(), staff.start, whiteLinePaint)
                canvas?.drawLine(0f, staff.end, width.toFloat(), staff.end, whiteLinePaint)
                for (bar in staff.bars) {
                    canvas?.drawLine(bar.x, staff.start, bar.x, staff.end, whiteLinePaint)
                }
            }
        }
    }

    override fun onTouchEvent(event: MotionEvent?): Boolean {
        when (event?.action) {
            MotionEvent.ACTION_DOWN -> {
                clickOrigin = when {
                    page.selectedStaffIndex < 0 -> PageSelectedClick(event.y)
                    page.selectedBarIndex < 0 -> onTouchStaff(event)
                    else -> null
                }
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_POINTER_UP -> {
                when (clickOrigin) {
                    is StaffDeselected -> deselectStaff()
                    is StaffSelectedClick -> {
                        page.getSelectedStaff().bars.add(BarPartition(event.x))
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
                        val staff = page.getSelectedStaff()
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
                        page.getSelectedStaff().bars.add(BarPartition(event.x))
                        clickOrigin = BarDrag()
                        longClickSubscription = Completable
                                .timer(longClickDuration, TimeUnit.MILLISECONDS)
                                .subscribeOn(Schedulers.io())
                                .observeOn(AndroidSchedulers.mainThread())
                                .subscribe(onLongClickListener)
                    }
                    is BarDrag -> {
                        page.getSelectedStaff().bars.last().x = event.x
                    }
                }
                invalidate()
            }
        }
        return true
    }

    private fun onTouchStaff(event: MotionEvent): ClickOrigin? {
        val staff = page.getSelectedStaff()
        return when {
            approxEqual(event.y, staff.start) -> StaffSelectedDrag(true)
            approxEqual(event.y, staff.end) -> StaffSelectedDrag(false)
            event.y < staff.start -> StaffDeselected()
            event.y > staff.end -> StaffDeselected()
            else -> StaffSelectedClick(event.x)
        }
    }

    private fun deselectStaff(): ClickOrigin? {
        page.selectedBarIndex = -1
        page.selectedStaffIndex = -1
        return null
    }

    private fun getColorPaint(colorId: Int): Paint {
        val paint = Paint()
        paint.color = ContextCompat.getColor(context, colorId)
        return paint
    }

    private fun approxEqual(a: Float, b: Float) = Math.abs(a - b) < touchSlop
}

private sealed class ClickOrigin

private class PageSelectedClick(var y: Float) : ClickOrigin()

private class StaffSelectedDrag(var isStart: Boolean) : ClickOrigin()

private class StaffDeselected : ClickOrigin()

private class StaffSelectedClick(var x: Float) : ClickOrigin()

private class BarDrag : ClickOrigin()

private class BarSelectedClick : ClickOrigin()
