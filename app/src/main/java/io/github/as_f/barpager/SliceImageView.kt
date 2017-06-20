package io.github.as_f.barpager

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.util.AttributeSet
import android.util.Log
import android.view.MotionEvent
import android.widget.ImageView

const val HANDLE_PADDING = 10

class SliceImageView(context: Context?, attrs: AttributeSet?) : ImageView(context, attrs) {

  val solidLinePaint = createSolidLinePaint()

  val sheet: Sheet? = null
  var selection: Selection? = null

  override fun setImageBitmap(bm: Bitmap?) {
    super.setImageBitmap(bm)
  }

  override fun onDraw(canvas: Canvas?) {
    super.onDraw(canvas)
    if (canvas != null) {
      val selection = selection
      when (selection) {
        is Line -> {
          val floatWidth = width.toFloat()
          canvas.drawLine(0f, selection.starty, floatWidth, selection.starty, solidLinePaint)
          canvas.drawLine(0f, selection.endy, floatWidth, selection.endy, solidLinePaint)
        }
        is Bar -> {}
      }
    }
  }

  override fun onTouchEvent(event: MotionEvent?): Boolean {
    if (event != null) {
      val selection = selection
      when (selection) {
        is Line -> {
          if (nearLine(event.y, selection.starty)) {

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
