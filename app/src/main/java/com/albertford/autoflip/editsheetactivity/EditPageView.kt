package com.albertford.autoflip.editsheetactivity

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.*
import android.support.v4.content.ContextCompat
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import com.albertford.autoflip.R
import com.albertford.autoflip.room.Page

private val identityMatrix = Matrix()

class EditPageView(context: Context, attrs: AttributeSet) : View(context, attrs) {

    var logic: EditPageLogic? = null
        private set

    var bitmap: Bitmap? = null

//    var listener: EditPageListener? = null

    private val pixelSlop = ViewConfiguration.get(context).scaledTouchSlop
    private var slop = 0f

    private val selectionFill = initSelectionFill()
    private val selectionStroke = initSelectionStroke()

    fun setPage(page: Page) {
        logic = EditPageLogic(page, slop, resources.getDimension(R.dimen.chevron_size))
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

        val bitmap = bitmap ?: return
        canvas.drawBitmap(bitmap, identityMatrix, null)

        canvas.save()
        canvas.scale(width.toFloat(), width.toFloat())

        val motion = logic?.motion
        val selection = logic?.selection

//        val motion = motion
//        if (page.staves.isEmpty()) {
//            if (motion is FirstBarSelection) {
//                val rect = motion.rect()
//                canvas.drawRect(rect, selectionFill)
//                canvas.drawRect(rect, selectionStroke)
//            }
//        } else {
//
//        }

        canvas.restore()
    }

    // Make sure pixel values are scaled relative to view width so that they are drawn correctly
    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        slop = pixelSlop.toFloat() / w
        selectionStroke.strokeWidth /= w
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent?): Boolean {
        val logic = logic ?: return super.onTouchEvent(event)
        event ?: return false
        val touch = PointF(event.x / width, event.y / width)
        when (event.action) {
            MotionEvent.ACTION_DOWN -> logic.onActionDown(touch)
            MotionEvent.ACTION_MOVE -> logic.onActionMove(touch)
            MotionEvent.ACTION_UP -> logic.onActionUp(touch)
            MotionEvent.ACTION_CANCEL -> logic.onActionUp(touch)
            else -> super.onTouchEvent(event)
        }
        return true
    }
}

//interface EditPageListener {
//    fun initalSelection(pageIndex: Int)
//    fun confirmSelection()
//    fun changeSelection()
//    fun cancelSelection()
//}
