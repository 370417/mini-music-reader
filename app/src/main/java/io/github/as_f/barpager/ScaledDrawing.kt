package io.github.as_f.barpager

import android.graphics.Canvas
import android.graphics.Paint

fun drawRect(canvas: Canvas, left: Float, top: Float, right: Float, bottom: Float, paint: Paint) {
  canvas.drawRect(left * canvas.width, top * canvas.height, right * canvas.width, bottom * canvas.height, paint)
}

fun drawHorizontal(canvas: Canvas, y: Float, startX: Float, endX: Float, paint: Paint) {
  canvas.drawRect(startX * canvas.width, y * canvas.height - 0.5f, endX * canvas.width, y * canvas.height + 0.5f, paint)
}

fun drawVertical(canvas: Canvas, x: Float, startY: Float, endY: Float, paint: Paint) {
  canvas.drawRect(x * canvas.width - 0.5f, startY * canvas.height, x * canvas.width + 0.5f, endY * canvas.height, paint)
}

fun drawHorizontalDashed(canvas: Canvas, y: Float, startX: Float, endX: Float, paint: Paint) {
  var scaledX = startX * canvas.width
  val scaledY = y * canvas.height
  while (scaledX < endX * canvas.width) {
    canvas.drawRect(scaledX, scaledY - 0.5f, scaledX + DASH_LENGTH, scaledY + 0.5f, paint)
    scaledX += 2 * DASH_LENGTH
  }
}

fun drawVerticalDashed(canvas: Canvas, x: Float, startY: Float, endY: Float, paint: Paint) {
  val scaledX = x * canvas.width
  var scaledY = startY * canvas.height
  while (scaledY < endY * canvas.height) {
    canvas.drawRect(scaledX - 0.5f, scaledY, scaledX + 0.5f, scaledY + DASH_LENGTH, paint)
    scaledY += 2 * DASH_LENGTH
  }
}

fun projectHorizontal(canvas: Canvas, initY: Float, period: Float) {
  val scaledPeriod = Math.abs(period * canvas.height)
  if (scaledPeriod < MINIMUM_PROJECTION) {
    return
  }
  val paint = fadePaint(scaledPeriod)
  var y = initY + period
  while (y > 0 && y < 1) {
    drawHorizontalDashed(canvas, y, 0f, 1f, paint)
    y += period
  }
}

fun projectVertical(canvas: Canvas, initX: Float, period: Float, startY: Float, endY: Float) {
  val scaledPeriod = Math.abs(period * canvas.width)
  if (scaledPeriod < MINIMUM_PROJECTION) {
    return
  }
  val paint = fadePaint(scaledPeriod)
  var x = initX + period
  while (x > 0 && x < 1) {
    drawVerticalDashed(canvas, x, startY, endY, paint)
    x += period
  }
}
