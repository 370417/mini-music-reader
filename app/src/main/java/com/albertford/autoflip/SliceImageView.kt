package com.albertford.autoflip

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.pdf.PdfRenderer
import android.support.v4.content.ContextCompat
import android.util.AttributeSet
import android.view.MotionEvent
import android.widget.ImageView
import com.albertford.autoflip.models.*

private const val HANDLE_PADDING = 50

class SliceImageView(context: Context?, attrs: AttributeSet?) : ImageView(context, attrs) {

    private val accent = getAccentPaint()
    private val mask = getMaskPaint()

    private var activePointerId = MotionEvent.INVALID_POINTER_ID
    private var lastTouchX = 0f
    private var lastTouchY = 0f

    var sheet: Sheet = Sheet()
    var selection: Selection = StaffSelection(0f, 0f)

    var renderer: PdfRenderer? = null

    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)
        if (canvas != null) {
            selection.mask(canvas, sheet.pages.last(), mask)
            drawSheet(canvas)
            selection.project(canvas, sheet)
            selection.drawHandles(canvas, accent)
        }
    }

    override fun onTouchEvent(event: MotionEvent?): Boolean {
        return when (event?.action) {
            MotionEvent.ACTION_DOWN -> onActionDown(event)
            MotionEvent.ACTION_MOVE -> onActionMove(event)
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> onActionCancel()
            MotionEvent.ACTION_POINTER_UP -> onActionPointerUp(event)
            else -> super.onTouchEvent(event)
        }
    }

    fun saveSelection() {
        selection = selection.save(sheet)
        invalidate()
    }

    fun nextStaff() {
        selection = suggestStaff(sheet)
        invalidate()
    }

    /**
     * @return Whether the current page (after this function executes) is the last one
     */
    fun nextPage(): Boolean {
        renderPage(sheet.pages.size)
        return sheet.pages.size + 1 == renderer?.pageCount
    }

    fun renderPage(i: Int) {
        val renderer = renderer
        if (renderer != null) {
            val pageRenderer = renderer.openPage(i)
            val imageWidth = pointsToPixels(pageRenderer.width)
            val imageHeight = pointsToPixels(pageRenderer.height)
            val fitToWidthScale = maxWidth.toFloat() / imageWidth
            val scaledWidth = Math.round(imageWidth * fitToWidthScale)
            val scaledHeight = Math.round(imageHeight * fitToWidthScale)
            val bitmap = Bitmap.createBitmap(scaledWidth, scaledHeight, Bitmap.Config.ARGB_8888)
            pageRenderer.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
            pageRenderer.close()
            setImageBitmap(bitmap)

            if (sheet.pages.size == i) {
                sheet.pages.add(Page())
                selection = suggestStaff(sheet)
            }
        }
    }

    private fun onActionDown(event: MotionEvent): Boolean {
        return if (selection.handleTouched(event.x, event.y, width, height)) {
            lastTouchX = event.x
            lastTouchY = event.y
            activePointerId = event.getPointerId(event.actionIndex)
            true
        } else {
            activePointerId = MotionEvent.INVALID_POINTER_ID
            false
        }
    }

    private fun onActionMove(event: MotionEvent): Boolean {
        if (activePointerId == MotionEvent.INVALID_POINTER_ID) {
            return false
        }
        val pointerIndex = event.findPointerIndex(activePointerId)
        val x = event.getX(pointerIndex)
        val y = event.getY(pointerIndex)
        val dx = (x - lastTouchX) / width
        val dy = (y - lastTouchY) / height
        lastTouchX = x
        lastTouchY = y
        selection.move(sheet.pages.last(), dx, dy)
        invalidate()
        return true
    }

    private fun onActionCancel(): Boolean {
        activePointerId = MotionEvent.INVALID_POINTER_ID
        return true
    }

    private fun onActionPointerUp(event: MotionEvent): Boolean {
        val pointerId = event.getPointerId(event.actionIndex)
        if (pointerId == activePointerId) {
            val otherPointerIndex = if (event.actionIndex == 0) 1 else 0
            lastTouchX = event.getX(otherPointerIndex)
            lastTouchY = event.getY(otherPointerIndex)
            activePointerId = event.getPointerId(otherPointerIndex)
        }
        return true
    }

    private fun drawSheet(canvas: Canvas) {
        val page = sheet.pages[sheet.pages.size - 1]
        for (staff in page.staves) {
            val startY = staff.startY
            val endY = staff.endY
            drawHorizontal(canvas, startY, 0f, width.toFloat(), white)
            drawHorizontal(canvas, endY, 0f, width.toFloat(), white)
            for (barLine in staff.barLines) {
                drawVertical(canvas, barLine.x, startY, endY, white)
            }
        }
    }

    private fun pointsToPixels(pixels: Int): Int {
        return resources.displayMetrics.densityDpi * pixels / 72
    }

    private fun getAccentPaint(): Paint {
        val paint = Paint()
        paint.color = ContextCompat.getColor(context, R.color.colorAccent)
        return paint
    }

    private fun getMaskPaint(): Paint {
        val paint = Paint()
        paint.color = ContextCompat.getColor(context, R.color.colorMask)
        return paint
    }

}

/**
 * Whether a coordinate is within less than HANDLE_PADDING pixels of a 1-pixel thick line
 */
fun nearLine(coord: Float, target: Float): Boolean {
    return Math.abs(coord - target) <= HANDLE_PADDING
}

fun clamp(num: Float, min: Float, max: Float): Float {
    return if (num < min) {
        min
    } else if (num > max) {
        max
    } else {
        num
    }
}
