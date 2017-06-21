package io.github.as_f.barpager

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.pdf.PdfRenderer
import android.util.AttributeSet
import android.view.MotionEvent
import android.widget.ImageView

const val HANDLE_PADDING = 10

class SliceImageView(context: Context?, attrs: AttributeSet?) : ImageView(context, attrs) {

  val backgroundPaint = createBackgroundPaint()
  val solidLinePaint = createSolidLinePaint()

  val sheet: Sheet = Sheet()
  var selection: Selection? = null

  var renderer: PdfRenderer? = null
    set(value) {
      field = value
      renderPage(0)
    }

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
          canvas.drawLine(0f, selection.startY, floatWidth, selection.startY, solidLinePaint)
          canvas.drawLine(0f, selection.endY, floatWidth, selection.endY, solidLinePaint)
        }
        is Bar -> {}
      }
    }
  }

  override fun onTouchEvent(event: MotionEvent?): Boolean {
    if (event != null) {
      val selection = selection
      when (selection) {
        is Staff -> {
          if (nearLine(event.y, selection.startY)) {

          }
        }
        is Bar -> {
        }
      }
    }
    return super.onTouchEvent(event)
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
        selection = Staff(scaledHeight * 0.1f, scaledHeight * 0.25f)
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
  paint.setARGB(64, 0, 0, 0)
  return paint
}

fun createSolidLinePaint(): Paint {
  return Paint()
}

/**
 * Whether a coordinate is within less than HANDLE_PADDING pixels of a 1-pixel thick line
 */
fun nearLine(coord: Float, target: Float): Boolean {
  return coord > target - HANDLE_PADDING && coord <= target + HANDLE_PADDING
}
