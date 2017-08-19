package com.albertford.autoflip

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Matrix
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import com.albertford.autoflip.models.Sheet

interface SheetRenderer {
    fun getPageCount(): Int

    /**
     * Render a full page with a set width, and as tall as necessary.
     */
    fun renderFullPage(i: Int, width: Int): Bitmap

    /**
     * Render a page with a set width, cut off at a maximmum height, top justified.
     */
    fun renderPagePreview(i: Int, width: Int, height: Int): Bitmap

    /**
     * Render a single bar as large as possible, preserving aspect ratio.
     */
    fun renderBar(sheet: Sheet, pageIndex: Int, staffIndex: Int, barIndex: Int, maxImageWidth: Int,
            maxImageHeight: Int): Bitmap

    fun close()

    fun getPageDimensions(i: Int): Pair<Int, Int>

    /**
     * Find the largest scale that fits every bar onto the screen.
     * This scale converts coordinates from the page scale to the image scale.
     * Note that this means, in code, we are looking for the smallest of the individual largest scales.
     */
     fun findMaxBarScale(sheet: Sheet, imageWidth: Int, imageHeight: Int): Float {
        var maxScale = Float.POSITIVE_INFINITY
        for (pageIndex in sheet.pages.indices) {
            val (pageWidth, pageHeight) = getPageDimensions(pageIndex)
            for (staff in sheet.pages[pageIndex].staves) {
                val scaledBarHeight = (staff.endY - staff.startY) * pageHeight
                for (barIndex in 0 until staff.barLines.size - 1) {
                    val barStart = staff.barLines[barIndex].x
                    val barEnd = staff.barLines[barIndex + 1].x
                    val scaledBarWidth = (barEnd - barStart) * pageWidth
                    if (scaledBarWidth * imageHeight > scaledBarHeight * imageWidth) {
                        val scale = imageWidth / scaledBarWidth
                        if (scale < maxScale) {
                            maxScale = scale
                        }
                    } else {
                        val scale = imageHeight / scaledBarHeight
                        if (scale < maxScale) {
                            maxScale = scale
                        }
                    }
                }
            }
        }
        return maxScale
    }
}

class PdfSheetRenderer(context: Context, uri: Uri) : SheetRenderer {

    private val renderer: PdfRenderer

    private var cachedPageRenderer: PdfRenderer.Page? = null

    private var cachedImageWidth = 0
    private var cachedImageHeight = 0
    private var cachedBarScale = 1f

    constructor(context: Context, uriString: String) : this(context, Uri.parse(uriString))

    init {
        val pdfDescriptor = context.contentResolver.openFileDescriptor(uri, "r")
        renderer = PdfRenderer(pdfDescriptor)
    }

    override fun getPageCount(): Int = renderer.pageCount

    override fun renderFullPage(i: Int, width: Int): Bitmap {
        val pageRenderer = getPage(i)
        val height = pageRenderer.height * width / pageRenderer.width
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        pageRenderer.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
        return bitmap
    }

    override fun renderPagePreview(i: Int, width: Int, height: Int): Bitmap {
        val pageRenderer = getPage(i)
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val scale = width.toFloat() / pageRenderer.width
        val matrix = Matrix()
        matrix.postScale(scale, scale)
        pageRenderer.render(bitmap, null, matrix, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
        return bitmap
    }

    override fun renderBar(sheet: Sheet, pageIndex: Int, staffIndex: Int, barIndex: Int,
            maxImageWidth: Int, maxImageHeight: Int): Bitmap {
        val scale = updateBarScale(sheet, maxImageWidth, maxImageHeight)
        val pageRenderer = getPage(pageIndex)
        val staff = sheet.pages[pageIndex].staves[staffIndex]
        val barTop = staff.startY * pageRenderer.height
        val barBottom = staff.endY * pageRenderer.height
        val barStart = staff.barLines[barIndex].x * pageRenderer.width
        val barEnd = staff.barLines[barIndex + 1].x * pageRenderer.width
        val imageWidth = Math.round(scale * (barEnd - barStart))
        val imageHeight = Math.round(scale * (barBottom - barTop))
        val matrix = Matrix()
        matrix.postTranslate(-barStart, -barTop)
        matrix.postScale(scale, scale)
        val bitmap = Bitmap.createBitmap(imageWidth, imageHeight, Bitmap.Config.ARGB_8888)
        pageRenderer.render(bitmap, null, matrix, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
        return bitmap
    }

    override fun close() {
        cachedPageRenderer?.close()
    }

    override fun getPageDimensions(i: Int): Pair<Int, Int> {
        val pageRenderer = getPage(i)
        return Pair(pageRenderer.width, pageRenderer.height)
    }

    private fun getPage(i: Int): PdfRenderer.Page {
        val pageRenderer = cachedPageRenderer
        return if (i == pageRenderer?.index) {
            pageRenderer
        } else {
            pageRenderer?.close()
            val newPageRenderer = renderer.openPage(i)
            cachedPageRenderer = newPageRenderer
            newPageRenderer
        }
    }

    private fun updateBarScale(sheet: Sheet, width: Int, height: Int): Float {
        if (width != cachedImageWidth || height != cachedImageHeight) {
            cachedImageWidth = width
            cachedImageHeight = height
            cachedBarScale = findMaxBarScale(sheet, width, height)
        }
        return cachedBarScale
    }
}
