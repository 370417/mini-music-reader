package io.github.as_f.barpager

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.util.AttributeSet
import android.util.Log
import android.widget.ImageView

class SliceImageView(context: Context?, attrs: AttributeSet?) : ImageView(context, attrs) {

  var scrollLength = 0

  override fun setImageBitmap(bm: Bitmap?) {
    val values = FloatArray(9)
    imageMatrix.getValues(values)
    Log.v("Matrix", imageMatrix.toString())
    super.setImageBitmap(bm)
  }

  override fun onDraw(canvas: Canvas?) {
    super.onDraw(canvas)
  }

}