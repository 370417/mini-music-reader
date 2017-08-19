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
    fun renderBar(sheet: Sheet, pageIndex: Int, staffIndex: Int, barIndex: Int, imageWidth: Int,
            imageHeight: Int): Bitmap

    fun close()
}

class PdfSheetRenderer(context: Context, uri: Uri) : SheetRenderer {

    private val renderer: PdfRenderer

    private var cachedPageRenderer: PdfRenderer.Page? = null

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
            imageWidth: Int, imageHeight: Int): Bitmap {
        val pageRenderer = getPage(pageIndex)
        val staff = sheet.pages[pageIndex].staves[staffIndex]
        val barStart = staff.barLines[barIndex].x
        val barEnd = staff.barLines[barIndex + 1].x
        val barWidth = barEnd - barStart
        val barHeight = staff.endY - staff.startY
        val scale = if (barWidth * imageHeight > barHeight * imageWidth) {
            barWidth / imageWidth
        } else {
            barHeight / imageHeight
        }
        val matrix = Matrix()
        matrix.postScale(scale, scale)
        val bitmap = Bitmap.createBitmap(imageWidth, imageHeight, Bitmap.Config.ARGB_8888)
        pageRenderer.render(bitmap, null, matrix, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
        return bitmap
    }

    override fun close() {
        cachedPageRenderer?.close()
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

}
