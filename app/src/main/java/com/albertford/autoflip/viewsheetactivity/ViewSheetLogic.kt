package com.albertford.autoflip.viewsheetactivity

import android.graphics.Bitmap
import android.graphics.Matrix
import android.graphics.Rect
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import com.albertford.autoflip.room.BarLine
import com.albertford.autoflip.room.Page
import com.albertford.autoflip.room.Staff
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class ViewSheetLogic(
        private val pdf: ParcelFileDescriptor,
        private val pages: Array<Page>,
        private val observer: ViewLogicObserver,
        private val coroutineScope: CoroutineScope
) : ViewActivityObserver {

    class Index(private val pages: Array<Page>, val pageIndex: Int, val staffIndex: Int, val barIndex: Int) {

        fun next(): Index? {
            val page = pages[pageIndex]
            val staff = page.staves[staffIndex]
            return when {
                barIndex < staff.barLines.size - 2 -> Index(pages, pageIndex, staffIndex, barIndex + 1)
                staffIndex < page.staves.size - 1 -> Index(pages, pageIndex, staffIndex + 1, 0)
                else -> Index(pages, pageIndex + 1, 0, 0).skipEmptyPage()
            }
        }

        fun skipEmptyPage(): Index? {
            return pages.getOrNull(pageIndex)?.let { page ->
                if (page.staves.isNotEmpty()) {
                    this
                } else {
                    Index(pages, pageIndex + 1, 0 , 0).skipEmptyPage()
                }
            }
        }
    }

    val maxStaffHeight = calcMaxStaffHeight(pages)
    val maxTwoBarWidth = calcMaxTwoBarWidth(pages)
    val maxBarWidth = calcMaxBarWidth(pages)

    var playing = false
    var index: Index = Index(pages, 0, 0, 0).skipEmptyPage()!!
    var nextBitmap:Bitmap? = null

    init {
        renderTwoBars()
    }

    private fun calcTwoBarScale(): Float {
        val scaleX = observer.getImgWidth() / maxTwoBarWidth
        val scaleY = observer.getImgHeight() / maxStaffHeight
        return Math.min(scaleX, scaleY)
    }

    private fun renderStaff(bitmap: Bitmap, staff: Staff, barIndex: Int, startX: Int) {
        PdfRenderer(pdf).openPage(staff.pageIndex)?.use { page ->
            val scale = calcTwoBarScale()
            val barLine = staff.barLines[barIndex]
            val destClip = calcDestClip(staff, barIndex, startX, page.width, scale)
            val matrix = Matrix()
            matrix.postTranslate(-barLine.x * page.width, -staff.top * page.width)
            matrix.postScale(scale, scale)
            matrix.postTranslate(0f, destClip.top.toFloat())
            page.render(bitmap, destClip, matrix, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
        }
    }

    private fun calcDestClip(staff: Staff, barIndex: Int, startX: Int, pageWidth: Int, scale: Float): Rect {
        val imageWidth = observer.getImgWidth()
        val imageHeight = observer.getImgHeight()
        val staffWidth = (staff.barLines.last().x - staff.barLines[barIndex].x) * pageWidth * scale
        val staffHeight = (staff.bottom - staff.top) * pageWidth * scale
        val destClip = Rect(0, 0, staffWidth.toInt(), staffHeight.toInt())
        destClip.offset(startX, (imageHeight - destClip.bottom) / 2)
        if (destClip.right > imageWidth) {
            destClip.right = imageWidth
        }
        return destClip
    }

    private fun renderTwoBars() {
        val bitmap = Bitmap.createBitmap(observer.getImgWidth(), observer.getImgHeight(), Bitmap.Config.ARGB_8888)
        renderStaff(bitmap, pages[0].staves[0], 0, 0)
        observer.showNext(bitmap)
    }

    private fun calcMaxStaffHeight(pages: Array<Page>): Float {
        return pages.fold(0f) { maxHeight, page ->
            page.staves.fold(maxHeight) { maxHeight, staff ->
                val height = (staff.bottom - staff.top) * page.height
                if (height > maxHeight) {
                    height
                } else {
                    maxHeight
                }
            }
        }
    }

    private fun calcMaxBarWidth(pages: Array<Page>): Float {
        return pages.fold(0f) { maxWidth, page ->
            page.staves.fold(maxWidth) { maxWidth, staff ->
                staff.barIndices().fold(maxWidth) { maxWidth, i ->
                    val bar = staff.getBar(i)
                    val width = (bar.right - bar.left) * page.width
                    if (width > maxWidth) {
                        width
                    } else {
                        maxWidth
                    }
                }
            }
        }
    }

    /**
     * Class for the accumulator used in calcMaxTwoBarWidth.
     *
     * max is the maximum two bar width found so far, and
     * prev is the width of the previous single bar.
     */
    private class TwoBarWidth(val max: Float, val prev: Float)

    private fun calcMaxTwoBarWidth(pages: Array<Page>): Float {
        return pages.fold(TwoBarWidth(0f, 0f)) { width, page ->
            page.staves.fold(width) { width, staff ->
                staff.barIndices().fold(width) { width, i ->
                    val bar = staff.getBar(i)
                    val currWidth = (bar.right - bar.left) * page.width
                    if (currWidth + width.prev > width.max) {
                        TwoBarWidth(currWidth + width.prev, currWidth)
                    } else {
                        width
                    }
                }
            }
        }.max
    }

    override fun pause() {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun play() {
        playing = true
        val job = coroutineScope.launch {
            while (playing) {
                if (index.next() != null && nextBitmap == null) {
                    val bitmap = Bitmap.createBitmap(observer.getImgWidth(), observer.getImgHeight(), Bitmap.Config.ARGB_8888)
                    renderStaff(bitmap, pages[0].staves[0], 0, 0)
                    nextBitmap = bitmap
                }
                delay(16)
            }
        }
    }

    override fun restart() {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }
}

interface ViewLogicObserver {
    /** Show the next bar */
    fun showNext(bitmap: Bitmap)

    fun endReached()

    fun getImgWidth(): Int

    fun getImgHeight(): Int
}
