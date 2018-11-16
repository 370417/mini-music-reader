package com.albertford.autoflip.views

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View

private val identityMatrix = Matrix()

class EditPageView(context: Context?, attrs: AttributeSet) : View(context, attrs) {
    var bitmap: Bitmap? = null
        set(value) {
            field = value
            invalidate()
        }

    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas) // draws the background
        canvas ?: return

        val bitmap = bitmap ?: return
        canvas.drawBitmap(bitmap, identityMatrix, null)
    }
}
