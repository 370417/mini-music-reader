package io.github.as_f.barpager

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.pdf.PdfRenderer
import android.support.v4.content.ContextCompat
import android.util.AttributeSet
import android.view.MotionEvent
import android.widget.ImageView
import io.github.as_f.barpager.models.BarLine
import io.github.as_f.barpager.models.Page
import io.github.as_f.barpager.models.Sheet
import io.github.as_f.barpager.models.Staff

const val HANDLE_PADDING = 50
const val DASH_LENGTH = 10

val black = makePaint(128, 0, 0, 0)
val white = makePaint(255, 255, 255, 255)

class SliceImageView(context: Context?, attrs: AttributeSet?) : ImageView(context, attrs) {

  val accent = getAccentPaint()

  var sheet: Sheet = Sheet()
  var selection: Selection = StaffSelection(0f, 0f)

  var renderer: PdfRenderer? = null

  var activePointerId = MotionEvent.INVALID_POINTER_ID
  var lastTouchX = 0f
  var lastTouchY = 0f

  override fun onDraw(canvas: Canvas?) {
    super.onDraw(canvas)
    if (canvas != null) {
      val selection = selection
      when (selection) {
        is StaffSelection -> {
          maskStaffSelection(canvas, selection)
          drawSheet(canvas)
          val (size, delta) = suggestProjectedStaff(sheet, selection)
          val period = size + delta
          if (Math.abs(period) > 2 * DASH_LENGTH) {
            val paint = fadePaint(Math.abs(period))
            projectHorizontal(canvas, selection.startY, period, paint)
            projectHorizontal(canvas, selection.endY, period, paint)
          }
          drawHorizontal(canvas, selection.startY, 0f, width.toFloat(), accent)
          drawHorizontal(canvas, selection.endY, 0f, width.toFloat(), accent)
        }
        is BarSelection -> {
          maskLastStaff(canvas)
          maskBarSelection(canvas, selection)
          drawSheet(canvas)
          val period = selection.endX - selection.startX
          if (Math.abs(period) > 2 * DASH_LENGTH) {
            val staff = sheet.pages.last().staves.last()
            val paint = fadePaint(Math.abs(period))
            projectVertical(canvas, selection.endX, period, staff.startY, staff.endY, paint)
          }
          drawVertical(canvas, selection.startX, 0f, height.toFloat(), accent)
          drawVertical(canvas, selection.endX, 0f, height.toFloat(), accent)
        }
        is BarLineSelection -> {
          maskLastStaff(canvas)
          maskBarLineSelection(canvas, selection)
          drawSheet(canvas)
          val staff = sheet.pages.last().staves.last()
          val period = selection.x - staff.barLines.last().x
          if (Math.abs(period) > 2 * DASH_LENGTH) {
            val paint = fadePaint(Math.abs(period))
            projectVertical(canvas, selection.x, period, staff.startY, staff.endY, paint)
          }
          drawVertical(canvas, selection.x, 0f, height.toFloat(), accent)
        }
      }
    }
  }

  fun maskStaffSelection(canvas: Canvas, selection: StaffSelection) {
    canvas.drawRect(0f, 0f, width.toFloat(), selection.startY, black)
    canvas.drawRect(0f, selection.endY, width.toFloat(), height.toFloat(), black)
  }

  fun maskLastStaff(canvas: Canvas) {
    val lastStaff = sheet.pages.last().staves.last()
    canvas.drawRect(0f, 0f, width.toFloat(), lastStaff.startY, black)
    canvas.drawRect(0f, lastStaff.endY, width.toFloat(), height.toFloat(), black)
  }

  fun maskBarSelection(canvas: Canvas, selection: BarSelection) {
    val lastStaff = sheet.pages.last().staves.last()
    canvas.drawRect(0f, lastStaff.startY, selection.startX, lastStaff.endY, black)
    canvas.drawRect(selection.endX, lastStaff.startY, width.toFloat(), lastStaff.endY, black)
  }

  fun maskBarLineSelection(canvas: Canvas, selection: BarLineSelection) {
    val lastStaff = sheet.pages.last().staves.last()
    val startX = lastStaff.barLines.last().x
    canvas.drawRect(0f, lastStaff.startY, startX, lastStaff.endY, black)
    canvas.drawRect(selection.x, lastStaff.startY, width.toFloat(), lastStaff.endY, black)
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

  fun onActionDown(event: MotionEvent): Boolean {
    val selection = selection
    when (selection) {
      is StaffSelection -> {
        val activeHandle = pickStaffHandle(selection, event.y)
        if (activeHandle != null) {
          selection.activeHandle = activeHandle
          updatePointer(event)
          return true
        }
      }
      is BarSelection -> {
        val activeHandle = pickBarHandle(selection, event.x)
        if (activeHandle != null) {
          selection.activeHandle = activeHandle
          updatePointer(event)
          return true
        }
      }
      is BarLineSelection -> {
        if (nearLine(event.x, selection.x)) {
          updatePointer(event)
          return true
        }
      }
    }
    activePointerId = MotionEvent.INVALID_POINTER_ID
    return false
  }

  fun updatePointer(event: MotionEvent) {
    lastTouchX = event.x
    lastTouchY = event.y
    activePointerId = event.getPointerId(event.actionIndex)
  }

  fun pickStaffHandle(selection: StaffSelection, y: Float): Handle? {
    return if (nearLine(y, selection.startY)) {
      Handle.START
    } else if (nearLine(y, selection.endY)) {
      Handle.END
    } else {
      null
    }
  }

  fun pickBarHandle(selection: BarSelection, x: Float): Handle? {
    return if (nearLine(x, selection.startX)) {
      Handle.START
    } else if (nearLine(x, selection.endX)) {
      Handle.END
    } else {
      null
    }
  }

  fun onActionMove(event: MotionEvent): Boolean {
    if (activePointerId == MotionEvent.INVALID_POINTER_ID) {
      return false
    }

    val pointerIndex = event.findPointerIndex(activePointerId)
    val x = event.getX(pointerIndex)
    val y = event.getY(pointerIndex)
    val dx = x - lastTouchX
    val dy = y - lastTouchY
    lastTouchX = x
    lastTouchY = y

    val floatWidth = width.toFloat()
    val floatHeight = height.toFloat()

    val selection = selection
    when (selection) {
      is StaffSelection -> {
        when (selection.activeHandle) {
          Handle.START -> selection.startY = clamp(selection.startY + dy, 0f, floatHeight)
          Handle.END -> selection.endY = clamp(selection.endY + dy, 0f, floatHeight)
        }
        if (selection.startY > selection.endY) {
          selection.flip()
        }
      }
      is BarSelection -> {
        when (selection.activeHandle) {
          Handle.START -> selection.startX = clamp(selection.startX + dx, 0f, floatWidth)
          Handle.END -> selection.endX = clamp(selection.endX + dx, 0f, floatWidth)
        }
        if (selection.startX > selection.endX) {
          selection.flip()
        }
      }
      is BarLineSelection -> {
        val lastStaff = sheet.pages.last().staves.last()
        selection.x = clamp(selection.x + dx, lastStaff.barLines.last().x, floatWidth)
      }
    }
    invalidate()
    return true
  }

  fun onActionCancel(): Boolean {
    activePointerId = MotionEvent.INVALID_POINTER_ID
    return true
  }

  fun onActionPointerUp(event: MotionEvent): Boolean {
    val pointerId = event.getPointerId(event.actionIndex)
    if (pointerId == activePointerId) {
      val otherPointerIndex = if (event.actionIndex == 0) 1 else 0
      lastTouchX = event.getX(otherPointerIndex)
      lastTouchY = event.getY(otherPointerIndex)
      activePointerId = event.getPointerId(otherPointerIndex)
    }
    return true
  }

  fun drawSheet(canvas: Canvas) {
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

  fun saveSelection() {
    val page = sheet.pages[sheet.pages.size - 1]
    val thisSelection = selection
    when (thisSelection) {
      is StaffSelection -> {
        selection = suggestFirstBar(sheet)
        page.staves.add(Staff(thisSelection.startY, thisSelection.endY))
      }
      is BarSelection -> {
        val staff = page.staves[page.staves.size - 1]
        staff.barLines.add(0, BarLine(thisSelection.startX))
        staff.barLines.add(1, BarLine(thisSelection.endX))
        selection = suggestBarLine(page, staff)
      }
      is BarLineSelection -> {
        val staff = page.staves[page.staves.size - 1]
        staff.barLines.add(BarLine(thisSelection.x))
        selection = suggestBarLine(page, staff)
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

      if (sheet.pages.size == i) {
        sheet.pages.add(Page(scaledWidth, scaledHeight))
        selection = suggestStaff(sheet)
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

fun drawHorizontal(canvas: Canvas, y: Float, startX: Float, endX: Float, paint: Paint) {
  canvas.drawRect(startX, y - 0.5f, endX, y + 0.5f, paint)
}

fun drawVertical(canvas: Canvas, x: Float, startY: Float, endY: Float, paint: Paint) {
  canvas.drawRect(x - 0.5f, startY, x + 0.5f, endY, paint)
}

fun drawHorizontalDashed(canvas: Canvas, y: Float, startX: Float, endX: Float, paint: Paint) {
  var x = startX
  while (x < endX) {
    canvas.drawRect(x, y - 0.5f, x + DASH_LENGTH, y + 0.5f, paint)
    x += 2 * DASH_LENGTH
  }
}

fun projectHorizontal(canvas: Canvas, initY: Float, period: Float, paint: Paint) {
  var y = initY + period
  while (y > 0 && y < canvas.height) {
    drawHorizontalDashed(canvas, y, 0f, canvas.width.toFloat(), paint)
    y += period
  }
}

fun projectVertical(canvas: Canvas, initX: Float, period: Float, startY: Float, endY: Float, paint: Paint) {
  var x = initX + period
  while (x > 0 && x < canvas.width) {
    drawVerticalDashed(canvas, x, startY, endY, paint)
    x += period
  }
}

fun drawVerticalDashed(canvas: Canvas, x: Float, startY: Float, endY: Float, paint: Paint) {
  var y = startY
  while (y < endY) {
    canvas.drawRect(x - 0.5f, y, x + 0.5f, y + DASH_LENGTH, paint)
    y += 2 * DASH_LENGTH
  }
}

fun makePaint(a: Int, r: Int, g: Int, b: Int): Paint {
  val paint = Paint()
  paint.setARGB(a, r, g, b)
  paint.style = Paint.Style.FILL
  return paint
}

fun fadePaint(period: Float): Paint {
  return if (period > 4 * DASH_LENGTH) {
    white
  } else {
    val alpha = 255 * (period - 2 * DASH_LENGTH) / (2 * DASH_LENGTH)
    makePaint(alpha.toInt(), 255, 255, 255)
  }
}
