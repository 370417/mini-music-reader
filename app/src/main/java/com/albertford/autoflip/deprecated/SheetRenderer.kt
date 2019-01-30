package com.albertford.autoflip.deprecated

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Matrix
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import com.albertford.autoflip.room.Bar

interface SheetRenderer {
    /**
     * Render a full page with a set width, and as tall as necessary.
     */
    fun renderFullPage(i: Int, width: Int): Bitmap?

    /**
     * Render a page with a set width, cut off at a maximum height, top justified.
     */
    fun renderPagePreview(i: Int, width: Int, height: Int): Bitmap?

    /**
     * Render a single bar as large as possible, preserving aspect ratio.
     */
    fun renderBar(barList: List<Bar>, index: Int, scale: Float): Bitmap?

    /**
     * Render the staff containing the input bar.
     */
    fun renderStaff(bar: Bar, width: Int): Bitmap?

    fun close()

    fun getPageWidth(i: Int): Int

    /**
     * Find the largest scale that fits every bar onto the screen.
     * This scale converts coordinates from the page scale to the image scale.
     * Note that this means, in code, we are looking for the smallest of the individual largest scales.
     */
    fun findMaxBarScale(barList: List<Bar>, imageWidth: Int, imageHeight: Int): Float =
        barList.fold(Float.POSITIVE_INFINITY) { acc, bar ->
            val scale = calcScale(imageWidth, imageHeight,
                    bar.width, bar.height)
            Math.min(acc, scale)
        }

    /**
     * Find the largest scale that fits every adjacent pair of bars onto the screen.
     * Bars are arranged vertically in portrait and horizontally in landscape.
     */
    fun findMaxTwoBarScale(barList: List<Bar>, imageWidth: Int, imageHeight: Int): Float {
        var maxScale = Float.POSITIVE_INFINITY
        for (i in 0 until barList.size - 1) {
            val firstBar = barList[i]
            val secondBar = barList[i + 1]
            val combinedBarWidth: Float
            val combinedBarHeight: Float
            if (imageWidth > imageHeight) {
                combinedBarWidth = firstBar.width + secondBar.width
                combinedBarHeight = Math.max(firstBar.height, secondBar.height)
            } else {
                combinedBarWidth = Math.max(firstBar.width, secondBar.width)
                combinedBarHeight = firstBar.height + secondBar.height
            }
            val scale = calcScale(imageWidth, imageHeight,
                    combinedBarWidth, combinedBarHeight)
            if (scale < maxScale) {
                maxScale = scale
            }
        }
        return maxScale
    }
}

private fun calcScale(containerWidth: Int, containerHeight: Int, width: Float,
        height: Float): Float {
    return if (width * containerHeight > height * containerWidth) {
        containerWidth / width
    } else {
        containerHeight / height
    }
}

class PdfSheetRenderer(context: Context, uri: Uri) : SheetRenderer {

    private val renderer: PdfRenderer

    private var cachedPageRenderer: PdfRenderer.Page? = null

    constructor(context: Context, uriString: String) : this(context, Uri.parse(uriString))

    init {
        val pdfDescriptor = context.contentResolver.openFileDescriptor(uri, "r")
        renderer = PdfRenderer(pdfDescriptor)
    }

    fun getPageCount(): Int = renderer.pageCount

    override fun renderFullPage(i: Int, width: Int): Bitmap? {
        val pageRenderer = getPage(i) ?: return null
        val height = pageRenderer.height * width / pageRenderer.width
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        pageRenderer.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
        return bitmap
    }

    override fun renderPagePreview(i: Int, width: Int, height: Int): Bitmap? {
        val pageRenderer = getPage(i) ?: return null
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val scale = width.toFloat() / pageRenderer.width
        val matrix = Matrix()
        matrix.postScale(scale, scale)
        pageRenderer.render(bitmap, null, matrix, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
        return bitmap
    }

    override fun renderBar(barList: List<Bar>, index: Int, scale: Float): Bitmap? {
        val bar = barList.getOrNull(index) ?: return null
        val pageRenderer = getPage(bar.pageIndex) ?: return null
        val imageWidth = Math.round(scale * bar.width)
        val imageHeight = Math.round(scale * bar.height)
        val bitmap = Bitmap.createBitmap(imageWidth, imageHeight, Bitmap.Config.ARGB_8888)
        val matrix = Matrix()
        matrix.postTranslate(-bar.left, -bar.top)
        matrix.postScale(scale, scale)
        pageRenderer.render(bitmap, null, matrix, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
        return bitmap
    }

    override fun renderStaff(bar: Bar, width: Int): Bitmap? {
        val pageRenderer = getPage(bar.pageIndex) ?: return null
        val scale = width.toFloat() / pageRenderer.width
        val height = bar.height * scale
        val bitmap = Bitmap.createBitmap(width, height.toInt(), Bitmap.Config.ARGB_8888)
        val matrix = Matrix()
        matrix.postTranslate(0f, -bar.top)
        matrix.postScale(scale, scale)
        pageRenderer.render(bitmap, null, matrix, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
        return bitmap
    }

    override fun close() {
        cachedPageRenderer?.close()
    }

    override fun getPageWidth(i: Int): Int {
        val pageRenderer = getPage(i)
        return pageRenderer?.width ?: -1
    }

    private fun getPage(i: Int): PdfRenderer.Page? {
        val pageRenderer = cachedPageRenderer
        return if (i == pageRenderer?.index) {
            pageRenderer
        } else if (i < 0 || i >= renderer.pageCount) {
            null
        } else {
            pageRenderer?.close()
            val newPageRenderer = renderer.openPage(i)
            cachedPageRenderer = newPageRenderer
            newPageRenderer
        }
    }
}

// drag rectangles like on desktop
// popup button above the selection like on medium for adding a staff/bar
