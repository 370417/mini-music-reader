package io.github.as_f.barpager

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.pdf.PdfRenderer
import android.util.AttributeSet
import android.util.Log
import android.view.MotionEvent
import android.widget.ImageView

const val HANDLE_PADDING = 50

const val DEFAULT_STAFF_START = 0.1f
const val DEFAULT_STAFF_END = 0.25f

val backgroundPaint = createBackgroundPaint()
val solidLinePaint = createSolidLinePaint()

class SliceImageView(context: Context?, attrs: AttributeSet?) : ImageView(context, attrs) {

  val sheet: Sheet = Sheet()
  var selection: Selection = Staff(0f, 0f)

  var renderer: PdfRenderer? = null
    set(value) {
      field = value
      renderPage(0)
    }

  var activePointerId = MotionEvent.INVALID_POINTER_ID
  var activeHandle = Handle.NONE
  var lastTouchX = 0f
  var lastTouchY = 0f

  override fun setImageBitmap(bm: Bitmap?) {
    super.setImageBitmap(bm)
  }

  override fun onDraw(canvas: Canvas?) {
    super.onDraw(canvas)
    if (canvas != null) {
      val selection = selection
      when (selection) {
        is Staff -> {
          val floatWidth = width.toFloat()
          val floatHeight = height.toFloat()
          canvas.drawRect(0f, 0f, floatWidth, selection.startY, backgroundPaint)
          canvas.drawRect(0f, selection.endY, floatWidth, floatHeight, backgroundPaint)
          canvas.drawLine(0f, selection.startY, floatWidth, selection.startY, solidLinePaint)
          canvas.drawLine(0f, selection.endY, floatWidth, selection.endY, solidLinePaint)
        }
        is Bar -> {
          val page = sheet.pages[sheet.pages.size-1]
          val staff = page.staves[page.staves.size-1]
        }
      }
    }
  }

  override fun onTouchEvent(event: MotionEvent?): Boolean {
    Log.v("Event", "${event?.action}")
    return when (event?.action) {
      MotionEvent.ACTION_DOWN -> {
        val x = event.x
        val y = event.y

        val selection = selection
        activeHandle = when (selection) {
          is Staff -> {
            if (nearLine(y, selection.startY)) {
              Handle.START
            } else if (nearLine(y, selection.endY)) {
              Handle.END
            } else {
              Handle.NONE
            }
          }
          is Bar -> {
            if (nearLine(x, selection.startX)) {
              Handle.START
            } else if (nearLine(x, selection.endX)) {
              Handle.END
            } else {
              Handle.NONE
            }
          }
        }

        if (activeHandle != Handle.NONE) {
          lastTouchX = event.x
          lastTouchY = event.y
          activePointerId = event.getPointerId(event.actionIndex)
          true
        } else {
          false
        }
      }

      MotionEvent.ACTION_MOVE -> {
        Log.v("ID", "$activePointerId")
        val pointerIndex = event.findPointerIndex(activePointerId)
        val x = event.getX(pointerIndex)
        val y = event.getY(pointerIndex)
        val dx = x - lastTouchX
        val dy = y - lastTouchY
        lastTouchX = x
        lastTouchY = y

        val selection = selection
        when (selection) {
          is Staff -> when (activeHandle) {
            Handle.START -> {
              selection.startY += dy
              invalidate()
            }
            Handle.END -> {
              selection.endY += dy
              invalidate()
            }
            Handle.NONE -> {}
          }
          is Bar -> when (activeHandle) {
            Handle.START -> {
              selection.startX += dx
              invalidate()
            }
            Handle.END -> {
              selection.endX += dx
              invalidate()
            }
            Handle.NONE -> {}
          }
        }
        activeHandle != Handle.NONE
      }

      MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
        activePointerId = MotionEvent.INVALID_POINTER_ID
        activeHandle = Handle.NONE
        true
      }

      MotionEvent.ACTION_POINTER_UP -> {
        val pointerId = event.getPointerId(event.actionIndex)
        if (pointerId == activePointerId) {
          activePointerId = MotionEvent.INVALID_POINTER_ID
          activeHandle = Handle.NONE
        }
        true
      }

      MotionEvent.ACTION_POINTER_DOWN -> true

      else -> super.onTouchEvent(event)
    }
  }

  fun saveStaff() {}

  fun nextStaff() {}

  fun saveBar() {}

  /**
   * @return Whether the current page (after this function executes) is the last one
   */
  fun nextPage(): Boolean {
    return sheet.pages.size + 1 == renderer?.pageCount
  }

  fun renderPage(i: Int) {
    val renderer = renderer
    if (renderer != null) {
      val page = renderer.openPage(i)
      val imageWidth = pointsToPixels(page.width)
      val imageHeight = pointsToPixels(page.height)
      val fitToWidthScale = maxWidth.toFloat() / imageWidth
      val scaledWidth = Math.round(imageWidth * fitToWidthScale)
      val scaledHeight = Math.round(imageHeight * fitToWidthScale)
      val bitmap = Bitmap.createBitmap(scaledWidth, scaledHeight, Bitmap.Config.ARGB_8888)
      page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
      page.close()
      setImageBitmap(bitmap)

      if (i == 0) {
        selection = Staff(scaledHeight * DEFAULT_STAFF_START, scaledHeight * DEFAULT_STAFF_END)
      } else {
        selection = sheet.pages[i-1].staves[0].clone()
      }
    }
  }

  private fun pointsToPixels(pixels: Int): Int {
    return resources.displayMetrics.densityDpi * pixels / 72
  }
}

fun createBackgroundPaint(): Paint {
  val paint = Paint()
  paint.setARGB(128, 0, 0, 0)
  paint.style = Paint.Style.FILL
  return paint
}

fun createSolidLinePaint(): Paint {
  return Paint()
}

/**
 * Whether a coordinate is within less than HANDLE_PADDING pixels of a 1-pixel thick line
 */
fun nearLine(coord: Float, target: Float): Boolean {
  return Math.abs(coord - target) <= HANDLE_PADDING
}

enum class Handle {
  NONE, START, END
}
