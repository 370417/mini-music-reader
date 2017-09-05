package com.albertford.autoflip

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View

class OverlayView(context: Context?, attrs: AttributeSet?) : View(context, attrs) {

    companion object {
        val maxAlpha = 100
    }

    var opacity = 1f
        set(value) {
            field = value
            paint.alpha = Math.round(maxAlpha * value)
            invalidate()
        }

    private val paint: Paint = Paint()

    init {
        paint.style = Paint.Style.FILL
        paint.color = 0
        paint.alpha = maxAlpha
    }

    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)
        canvas?.drawRect(0f, 0f, canvas.width.toFloat(), canvas.height.toFloat(), paint)
    }

}