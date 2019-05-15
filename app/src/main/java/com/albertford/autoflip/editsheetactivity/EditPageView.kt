package com.albertford.autoflip.editsheetactivity

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.*
import android.support.graphics.drawable.VectorDrawableCompat
import android.support.v4.content.ContextCompat
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import com.albertford.autoflip.R
import com.albertford.autoflip.room.Page
import kotlin.math.roundToInt

private val identityMatrix = Matrix()

class EditPageView(context: Context, attrs: AttributeSet) : View(context, attrs), EditSheetObserver {

    private var logic: EditPageLogic? = null

    var bitmap: Bitmap? = null

//    var editable: Boolean = false

//    var listener: EditPageListener? = null

    private val pixelSlop = ViewConfiguration.get(context).scaledTouchSlop
    private var slop = 0f

    private val selectionFill = initSelectionFill()
    private val selectionStroke = initSelectionStroke()
    private val nonSelectionFill = initNonSelectinFill()

    private val chevronRight = VectorDrawableCompat.create(resources, R.drawable.chevron_right, null)
    private val chevronDown = VectorDrawableCompat.create(resources, R.drawable.chevron_down, null)
    private var chevronSize = 0f

    // preallocated rect used for drawing
    private val rect = RectF()

    fun setPage(page: Page, editPageObserver: EditPageObserver) {
        val logic = EditPageLogic(page, slop, chevronSize)
        logic.observers.add(editPageObserver)
        this.logic = logic
    }

    /**
     * Set width-dependent variables.
     *
     * When this view is first inflated, it may have width/height of 0.
     */
    fun bindWidth(width: Int) {
        slop = pixelSlop.toFloat() / width
        selectionStroke.strokeWidth = 2f / width
        chevronSize = resources.getDimension(R.dimen.chevron_size) / width
    }

    private fun initSelectionFill(): Paint {
        val paint = Paint()
        paint.color = ContextCompat.getColor(context, R.color.selectionFill)
        return paint
    }

    private fun initSelectionStroke(): Paint {
        val paint = Paint()
        paint.color = ContextCompat.getColor(context, R.color.selectionStroke)
        paint.style = Paint.Style.STROKE
        return paint
    }

    private fun initNonSelectinFill(): Paint {
        val paint = Paint()
        paint.color = ContextCompat.getColor(context, R.color.nonselectionFill)
        return paint
    }

    override fun onEditEnabledChanged(editEnabled: Boolean) {
        logic?.editable = editEnabled
        if (!editEnabled) {
            logic?.selection = null
        }
        invalidate()
    }

    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas) // draws the background
        canvas ?: return

        bitmap?.also { bitmap ->
            canvas.drawBitmap(bitmap, identityMatrix, null)
        }

        val logic = logic ?: return

        canvas.save()
        canvas.scale(width.toFloat(), width.toFloat())

        val selection = logic.selection
        val page = logic.page
        for (staffIndex in page.staves.indices) {
            val staff = page.staves[staffIndex]
            for (barIndex in staff.barIndices()) {
                val bar = staff.getBar(barIndex)
                if (selection != null && staffIndex == selection.staffIndex && barIndex == selection.barIndex) {
                    rect.left = bar.left
                    rect.top = staff.top
                    rect.right = bar.right
                    rect.bottom = staff.bottom
                    canvas.drawRect(rect, selectionFill)
                    canvas.drawRect(rect, selectionStroke)
                } else {
                    rect.left = bar.left + selectionStroke.strokeWidth
                    rect.top = staff.top + selectionStroke.strokeWidth
                    rect.right = bar.right - selectionStroke.strokeWidth
                    rect.bottom = staff.bottom - selectionStroke.strokeWidth
                    canvas.drawRect(rect, nonSelectionFill)
                }
            }
        }

        canvas.restore()

        // the chevrons are drawables, so they can can only work with integer bounds
        // for this reason they have to be drawn outside the canvas transformation
        if (logic.calcNewBarRect(rect)) {
            chevronRight?.let {
                val left = (rect.left * width).roundToInt()
                val top = (rect.top * width).roundToInt()
                it.setBounds(left, top, left + it.intrinsicWidth, top + it.intrinsicHeight)
                it.draw(canvas)
            }
        }

        if (logic.calcNewStaffRect(rect)) {
            chevronDown?.let {
                val left = (rect.left * width).roundToInt()
                val top = (rect.top * width).roundToInt()
                it.setBounds(left, top, left + it.intrinsicWidth, top + it.intrinsicHeight)
                it.draw(canvas)
            }
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent?): Boolean {
        val logic = logic ?: return super.onTouchEvent(event)
        if (!logic.editable) {
            return false
        }
        event ?: return false
        val touch = PointF(event.x / width, event.y / width)
        when (event.action) {
            MotionEvent.ACTION_DOWN -> logic.onActionDown(touch)
            MotionEvent.ACTION_MOVE -> logic.onActionMove(touch)
            MotionEvent.ACTION_UP -> logic.onActionUp()
            MotionEvent.ACTION_CANCEL -> logic.onActionUp() // don't respond to clicks if they were canceled
            else -> return super.onTouchEvent(event)
        }
        invalidate()
        return true
    }
}
