package com.albertford.autoflip.viewsheetactivity

import com.albertford.autoflip.room.Page

class ViewSheetLogic {


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
}

interface ViewLogicObserver {
    /** Show the next bar */
    fun next()

    fun endReached()
}
