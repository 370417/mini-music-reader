package com.albertford.autoflip.views

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import com.albertford.autoflip.room.Page

private val identityMatrix = Matrix()

class EditPageView(context: Context?, attrs: AttributeSet) : View(context, attrs) {
    var bitmap: Bitmap? = null
        set(value) {
            field = value
            invalidate()
        }

    var page: Page? = null

    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas) // draws the background
        canvas ?: return

        val bitmap = bitmap ?: return
        canvas.drawBitmap(bitmap, identityMatrix, null)
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent?): Boolean {
        return true
    }
}

private class Point

private sealed class Selection

private class InitialBarSelection(val origin: Int, var drag: Int) : Selection()



// Initial bar selection
// inital bar corner resize
// initial bar edge resize

// once the inital bar is created, the bottom sheet buttons will pop up:
// Add bar / Add sheet (or Next Bar / Next Sheet for when the bar is not the current last bar)

// Use barlines and staff classes instead of bar classes?
