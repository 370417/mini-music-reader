package io.github.as_f.barpager

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.pdf.PdfRenderer
import android.support.v4.content.res.ResourcesCompat
import android.util.AttributeSet
import android.view.MotionEvent
import android.widget.ImageView

const val HANDLE_PADDING = 50

val backgroundPaint = createBackgroundPaint()

class SliceImageView(context: Context?, attrs: AttributeSet?) : ImageView(context, attrs) {

  val handlePaint = createHandlePaint()

  val sheet: Sheet = Sheet()
  var selection: Selection = Staff(0f, 0f)
    set(value) {
      // prevent suggested selection from going off the page
      field = value
      val page = sheet.pages[sheet.pages.size - 1]
      when (value) {
        is Staff -> {
          if (value.endY > page.height) {
            value.endY = page.height.toFloat()
          }
          if (value.startY > page.height) {
            value.startY = page.height.toFloat()
          }
        }
        is Bar -> {
          if (value.endX > page.width) {
            value.endX = page.width.toFloat()
          }
          if (value.startX > page.width) {
            value.startX = page.width.toFloat()
          }
        }
      }
    }

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
      val floatWidth = width.toFloat()
      val floatHeight = height.toFloat()
      val selection = selection
      when (selection) {
        is Staff -> {
          canvas.drawRect(0f, 0f, floatWidth, selection.startY, backgroundPaint)
          canvas.drawRect(0f, selection.endY, floatWidth, floatHeight, backgroundPaint)

          canvas.drawLine(0f, selection.startY, floatWidth, selection.startY, handlePaint)
          canvas.drawLine(0f, selection.endY, floatWidth, selection.endY, handlePaint)
        }
        is Bar -> {
          val page = sheet.pages[sheet.pages.size-1]
          val staff = page.staves[page.staves.size-1]
          canvas.drawRect(0f, 0f, floatWidth, staff.startY, backgroundPaint)
          canvas.drawRect(0f, staff.endY, floatWidth, floatHeight, backgroundPaint)
          canvas.drawRect(0f, staff.startY, selection.startX, staff.endY, backgroundPaint)
          canvas.drawRect(selection.endX, staff.startY, floatWidth, staff.endY, backgroundPaint)

          canvas.drawLine(selection.startX, 0f, selection.startX, floatHeight, handlePaint)
          canvas.drawLine(selection.endX, 0f, selection.endX, floatHeight, handlePaint)
        }
      }
    }
  }

  override fun onTouchEvent(event: MotionEvent?): Boolean {
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
              selection.startY = clamp(selection.startY + dy, 0f, height.toFloat())
              if (selection.flip()) {
                activeHandle = Handle.END
              }
              invalidate()
            }
            Handle.END -> {
              selection.endY = clamp(selection.endY + dy, 0f, height.toFloat())
              if (selection.flip()) {
                activeHandle = Handle.START
              }
              invalidate()
            }
            Handle.NONE -> {}
          }
          is Bar -> when (activeHandle) {
            Handle.START -> {
              selection.startX = clamp(selection.startX + dx, 0f, width.toFloat())
              if (selection.flip()) {
                activeHandle = Handle.END
              }
              invalidate()
            }
            Handle.END -> {
              selection.endX = clamp(selection.endX + dx, 0f, width.toFloat())
              if (selection.flip()) {
                activeHandle = Handle.START
              }
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
          val otherPointerIndex = if (event.actionIndex == 0) 1 else 0
          lastTouchX = event.getX(otherPointerIndex)
          lastTouchY = event.getY(otherPointerIndex)
          activePointerId = event.getPointerId(otherPointerIndex)
        }
        true
      }

      MotionEvent.ACTION_POINTER_DOWN -> true

      else -> super.onTouchEvent(event)
    }
  }

  fun saveSelection() {
    val page = sheet.pages[sheet.pages.size - 1]
    val thisSelection = selection
    when (thisSelection) {
      is Staff -> {
        page.staves.add(thisSelection)
        selection = suggestBar(sheet)
      }
      is Bar -> {
        val staff = page.staves[page.staves.size - 1]
        staff.bars.add(thisSelection)
        selection = suggestBar(sheet)
      }
    }
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

      sheet.pages.add(Page(scaledWidth, scaledHeight))
      selection = suggestStaff(sheet)
    }
  }

  private fun pointsToPixels(pixels: Int): Int {
    return resources.displayMetrics.densityDpi * pixels / 72
  }

  private fun createHandlePaint(): Paint {
    val paint = Paint()
    val accentColor = ResourcesCompat.getColor(resources, R.color.colorAccent, null)
    paint.color = accentColor
    return Paint()
  }
}

fun createBackgroundPaint(): Paint {
  val paint = Paint()
  paint.setARGB(128, 0, 0, 0)
  paint.style = Paint.Style.FILL
  return paint
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

enum class Handle {
  NONE, START, END
}
