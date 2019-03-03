package com.albertford.autoflip.editsheetactivity

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.*
import android.support.v4.content.ContextCompat
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.widget.Toast
import com.albertford.autoflip.R
import com.albertford.autoflip.room.Page

private val identityMatrix = Matrix()

class EditPageView(context: Context, attrs: AttributeSet) : View(context, attrs) {

    private var logic: EditPageLogic? = null

    var bitmap: Bitmap? = null

//    var listener: EditPageListener? = null

    private val pixelSlop = ViewConfiguration.get(context).scaledTouchSlop
    private var slop = 0f

    private val selectionFill = initSelectionFill()
    private val selectionStroke = initSelectionStroke()

    private val chevronRight = BitmapFactory.decodeResource(resources, R.drawable.chevron_right)
    private var chevronDown = BitmapFactory.decodeResource(resources, R.drawable.chevron_down)

    // preallocated rect used for drawing
    private val rect = RectF()

    fun setPage(page: Page) {
        logic = EditPageLogic(page, slop, 0.05f)//resources.getDimension(R.dimen.chevron_size) / width)
    }

    fun bindWidth(width: Int) {
        slop = pixelSlop.toFloat() / width
        selectionStroke.strokeWidth /= width
    }

    private fun initSelectionFill(): Paint {
        val paint = Paint()
        paint.color = ContextCompat.getColor(context, R.color.colorAccent)
        paint.alpha = 100
        return paint
    }

    private fun initSelectionStroke(): Paint {
        val paint = Paint()
        paint.color = ContextCompat.getColor(context, R.color.colorPrimary)
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 2f
        return paint
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
                val leftBarLine = staff.barLines[barIndex]
                val rightBarLine = staff.barLines[barIndex + 1]
                if (selection != null && staffIndex == selection.staffIndex && barIndex == selection.barIndex) {
                    rect.left = leftBarLine.x
                    rect.top = staff.top
                    rect.right = rightBarLine.x
                    rect.bottom = staff.bottom
                    canvas.drawRect(rect, selectionFill)
                    canvas.drawRect(rect, selectionStroke)
                } else {
                    rect.left = leftBarLine.x + selectionStroke.strokeWidth
                    rect.top = staff.top + selectionStroke.strokeWidth
                    rect.right = rightBarLine.x - selectionStroke.strokeWidth
                    rect.bottom = staff.bottom - selectionStroke.strokeWidth
                    canvas.drawRect(rect, selectionFill)
                }
            }
        }
        if (chevronRight != null && logic.calcNewBarRect(rect)) {
            canvas.drawBitmap(chevronRight, null, rect, null)
        }
        if (logic.calcNewBarRect(rect)) {
            canvas.drawRect(rect, selectionFill)
        }
        if (chevronDown != null && logic.calcNewStaffRect(rect)) {
            canvas.drawBitmap(chevronDown, null, rect, null)
        }
        if (logic.calcNewStaffRect(rect)) {
            canvas.drawRect(rect, selectionFill)
        }

        canvas.restore()
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent?): Boolean {
        val logic = logic ?: return super.onTouchEvent(event)
        event ?: return false
        val touch = PointF(event.x / width, event.y / width)
        when (event.action) {
            MotionEvent.ACTION_DOWN -> logic.onActionDown(touch)
            MotionEvent.ACTION_MOVE -> logic.onActionMove(touch)
            MotionEvent.ACTION_UP -> {
                val result = logic.onActionUp()
                when (result) {
                    is ClickSelectionResult -> {}
                    is AttemptedScrollResult -> scrollHelpToast()
                }
            }
            MotionEvent.ACTION_CANCEL -> logic.onActionUp() // don't respond to clicks if they were canceled
            else -> return super.onTouchEvent(event)
        }
        invalidate()
        return true
    }

    private fun scrollHelpToast() {
        Toast.makeText(context, R.string.scroll_helper, Toast.LENGTH_SHORT).show()
    }
}

//interface EditPageListener {
//    fun initalSelection(pageIndex: Int)
//    fun confirmSelection()
//    fun changeSelection()
//    fun cancelSelection()
//}
