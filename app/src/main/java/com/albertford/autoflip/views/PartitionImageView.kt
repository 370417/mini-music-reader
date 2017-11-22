package com.albertford.autoflip.views

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.os.Parcel
import android.os.Parcelable
import android.support.v4.content.ContextCompat
import android.util.AttributeSet
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.widget.ImageView
import com.albertford.autoflip.R
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
        private val lightestOverlayPaint = makePaint(
                lightestOverlay, 0, 0, 0)
    }

    var slideOffset = 0f
        set(value) {
            barOverlayPaint.alpha = Math.round(
                    lightestOverlay * value)
            invalidate()
        }

    var onSelectBarListener: ((beginRepeat: Boolean, endRepeat: Boolean) -> Unit)? = null

    var page: Page? = null

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
        val page = page
        if (page != null && page.staves.isNotEmpty()) {
            val click = clickOrigin
            if (click is StaffSelectedClick) {
                val staff = page.staves.last()
                staff.barLines.sort()
                val index = -staff.barLines.binarySearch(BarLine(click.x)) - 1
                if (index > 0 && index < staff.barLines.size) {
                    clickOrigin = null
                    page.selectedBarIndex = index - 1
                    val firstBarLine = staff.barLines[index - 1]
                    val secondBarLine = staff.barLines[index]
                    onSelectBarListener?.invoke(firstBarLine.beginRepeat, secondBarLine.endRepeat)
                }
            }
        }
    }

    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)
        val page = page
        if (page == null) {
            canvas?.drawRect(0f, 0f, width.toFloat(), height.toFloat(),
                    lightestOverlayPaint)
            return
        }
        when {
            !page.staffSelected -> {
                canvas?.drawRect(0f, 0f, width.toFloat(), height.toFloat(),
                        lightestOverlayPaint)
            }
            page.selectedBarIndex < 0 -> {
                val staff = page.staves.last()
                canvas?.drawRect(0f, 0f, width.toFloat(), staff.start,
                        lightestOverlayPaint)
                canvas?.drawRect(0f, staff.end, width.toFloat(), height.toFloat(),
                        lightestOverlayPaint)
            }
            else -> {
                val staff = page.staves.last()
                canvas?.drawRect(0f, 0f, width.toFloat(), staff.start,
                        lightestOverlayPaint)
                canvas?.drawRect(0f, staff.end, width.toFloat(), height.toFloat(),
                        lightestOverlayPaint)
                val firstBar = staff.barLines[page.selectedBarIndex]
                val secondBar = staff.barLines[page.selectedBarIndex + 1]
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
            for (bar in staff.barLines) {
                canvas?.drawLine(bar.x, staff.start, bar.x, staff.end, barPaint)
            }
        }
    }

    override fun onTouchEvent(event: MotionEvent?): Boolean {
        val page = page ?: return false
        when (event?.action) {
            MotionEvent.ACTION_DOWN -> {
                clickOrigin = when {
                    !page.staffSelected -> PageSelectedClick(event.y)
                    page.selectedBarIndex < 0 -> onTouchStaff(page, event)
                    else -> null
                }
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_POINTER_UP -> {
                when (clickOrigin) {
                    is StaffDeselected -> page.deselectStaff()
                    is StaffSelectedClick -> {
                        page.staves.last().barLines.add(BarLine(event.x))
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
                        page.staves.last().barLines.add(BarLine(event.x))
                        clickOrigin = BarDrag()
                        val subscription = longClickSubscription
                        if (subscription != null && !subscription.isDisposed) {
                            subscription.dispose()
                        }
                    }
                    is BarDrag -> {
                        page.staves.last().barLines.last().x = event.x
                    }
                }
                invalidate()
            }
        }
        return true
    }

    override fun onSaveInstanceState(): Parcelable {
        val superState = super.onSaveInstanceState()
        return PartitionImageState(superState, this)
    }

    override fun onRestoreInstanceState(state: Parcelable?) {
        when (state) {
            is PartitionImageState -> {
                super.onRestoreInstanceState(state.superState)
//                allowTouch = state.allowTouch
                slideOffset = state.slideOffSet
                page = state.page
            }
            else -> super.onRestoreInstanceState(state)
        }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        longClickSubscription?.run {
            dispose()
        }
    }

    private fun onTouchStaff(page: Page, event: MotionEvent): ClickOrigin? {
        val staff = page.staves.last()
        return when {
            approxEqual(event.y, staff.start) -> StaffSelectedDrag(true)
            approxEqual(event.y, staff.end) -> StaffSelectedDrag(false)
            event.y < staff.start -> {
                StaffDeselected()
            }
            event.y > staff.end -> {
                StaffDeselected()
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

private class PartitionImageState : View.BaseSavedState {

    var slideOffSet: Float
    var page: Page?

    constructor(savedState: Parcelable, view: PartitionImageView) : super(savedState) {
        slideOffSet = view.slideOffset
        page = view.page
    }

    private constructor(parcel: Parcel) : super(parcel) {
        slideOffSet = parcel.readFloat()
        page = parcel.readParcelable(Page::class.java.classLoader)
    }

    override fun writeToParcel(parcel: Parcel?, flags: Int) {
        parcel?.run {
            writeFloat(slideOffSet)
            writeParcelable(page, 0)
        }
    }

    companion object CREATOR : Parcelable.Creator<PartitionImageState> {
        override fun newArray(size: Int): Array<PartitionImageState?> = arrayOfNulls(size)

        override fun createFromParcel(parcel: Parcel) = PartitionImageState(parcel)
    }

}
