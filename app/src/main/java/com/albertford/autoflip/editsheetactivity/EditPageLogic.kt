package com.albertford.autoflip.editsheetactivity

import android.graphics.PointF
import android.graphics.RectF
import com.albertford.autoflip.room.Page

// All dimensions and locations in this class are given relative to the width of document
// This way the same normalized slop value can be used in the x and y axes

class EditPageLogic(val page: Page, private val slop: Float, private val chevronSize: Float) {
    var selection: Selection? = null
    var initialTouch: InitialTouch? = null
    var motion: Motion? = null

    fun onActionDown(touch: PointF) {
        initialTouch = if (page.staves.isEmpty()) {
            InitialTouch.FIRST_SELECTION
        } else {
            verifySelection()
            val selection = selection
            if (selection == null) {
                InitialTouch.CHANGE_OR_CANCEL_SELECTION
            } else {
                val touchLocation = calcTouchLocation(touch, calcSelectionRect(selection))
                when {
                    touchLocation != null -> touchLocation
                    calcNewBarRect(selection).contains(touch.x, touch.y) -> InitialTouch.NEW_BAR
                    calcNewStaffRect(selection).contains(touch.x, touch.y) -> InitialTouch.NEW_STAFF
                    else -> InitialTouch.CHANGE_OR_CANCEL_SELECTION
                }
            }
        }
    }

    fun onActionMove(touch: PointF) {
        when (initialTouch) {
            InitialTouch.FIRST_SELECTION -> TODO()
            InitialTouch.CURRENT_SELECTION -> TODO()
            InitialTouch.RESIZE_NW -> TODO()
            InitialTouch.RESIZE_NE -> TODO()
            InitialTouch.RESIZE_SW -> TODO()
            InitialTouch.RESIZE_SE -> TODO()
            InitialTouch.RESIZE_LEFT -> TODO()
            InitialTouch.RESIZE_RIGHT -> TODO()
            InitialTouch.RESIZE_TOP -> TODO()
            InitialTouch.RESIZE_BOTTOM -> TODO()
            InitialTouch.NEW_BAR -> TODO()
            InitialTouch.NEW_STAFF -> TODO()
            InitialTouch.CHANGE_OR_CANCEL_SELECTION -> TODO()
            null -> TODO()
        }
        initialTouch = null
        TODO()
    }

    fun onActionUp(touch: PointF) {
        motion = null
        initialTouch = null
        TODO()
    }

    /**
     * Calculates which part of a target rectangle an initial touch corresponds to. If it is outside
     * the rectangle, null is returned. Otherwise it returns an enum for the inside of the rectangle
     * or one of the four corners or four edges.
     */
    fun calcTouchLocation(touch: PointF, rect: RectF): InitialTouch? {
        val horizTouchLocation = calcTouchLocation(touch.x, rect.left, rect.right)
        val vertTouchLocation = calcTouchLocation(touch.y, rect.top, rect.bottom)
        return when (horizTouchLocation) {
            TouchLocation.OUTSIDE -> null
            TouchLocation.INSIDE -> when (vertTouchLocation) {
                TouchLocation.OUTSIDE -> null
                TouchLocation.INSIDE -> InitialTouch.CURRENT_SELECTION
                TouchLocation.LOW_HANDLE -> InitialTouch.RESIZE_TOP
                TouchLocation.HIGH_HANDLE -> InitialTouch.RESIZE_BOTTOM
            }
            TouchLocation.LOW_HANDLE -> when (vertTouchLocation) {
                TouchLocation.OUTSIDE -> null
                TouchLocation.INSIDE -> InitialTouch.RESIZE_LEFT
                TouchLocation.LOW_HANDLE -> InitialTouch.RESIZE_NW
                TouchLocation.HIGH_HANDLE -> InitialTouch.RESIZE_SW
            }
            TouchLocation.HIGH_HANDLE -> when (vertTouchLocation) {
                TouchLocation.OUTSIDE -> null
                TouchLocation.INSIDE -> InitialTouch.RESIZE_RIGHT
                TouchLocation.LOW_HANDLE -> InitialTouch.RESIZE_NE
                TouchLocation.HIGH_HANDLE -> InitialTouch.RESIZE_SE
            }
        }
    }

    /**
     * Calculates if touch is outside or inside the two handles or within slop distance of the low
     * or high handle.
     */
    fun calcTouchLocation(touch: Float, lowHandle: Float, highHandle: Float): TouchLocation {
        return when {
            touch < lowHandle -> if (lowHandle - touch <= slop) {
                TouchLocation.LOW_HANDLE
            } else {
                TouchLocation.OUTSIDE
            }
            touch > highHandle -> if (touch - highHandle <= slop) {
                TouchLocation.HIGH_HANDLE
            } else {
                TouchLocation.OUTSIDE
            }
            2 * touch < lowHandle + highHandle -> if (touch - lowHandle <= slop) {
                TouchLocation.LOW_HANDLE
            } else {
                TouchLocation.INSIDE
            }
            else -> if (highHandle - touch <= slop) {
                TouchLocation.HIGH_HANDLE
            } else {
                TouchLocation.INSIDE
            }
        }
    }

    fun barRects(): ArrayList<RectF> {
        val rects = ArrayList<RectF>()
        for (staff in page.staves) {
            for (i in 1 until staff.barLines.size) {
                val left = staff.barLines[i - 1].x
                val right = staff.barLines[i].x
                rects.add(RectF(left, staff.top, right, staff.bottom))
            }
        }
        return rects
    }

    /**
     * Make sure the selection is consistent with the page bounds. If the selection is out of the
     * page, set the selection to null.
     */
    private fun verifySelection() {
        val selection = selection ?: return
        val staff = page.staves.getOrNull(selection.staffIndex)
        if (staff == null) {
            this.selection = null
        } else if (selection.barIndex >= staff.barLines.size) {
            this.selection = null
        }
    }

    private fun calcSelectionRect(selection: Selection): RectF {
        val staff = page.staves[selection.staffIndex]
        val leftBarLine = staff.barLines[selection.barIndex]
        val rightBarLine = staff.barLines[selection.barIndex + 1]
        return RectF(leftBarLine.x, staff.top, rightBarLine.x, staff.bottom)
    }

    private fun calcNewBarRect(selection: Selection): RectF {
        val staff = page.staves[selection.staffIndex]
        val lastBarLine = staff.barLines.last()
        val top = (staff.top + staff.bottom - chevronSize) / 2
        val bottom = (staff.top + staff.bottom + chevronSize) / 2
        return RectF(lastBarLine.x, top, lastBarLine.x + chevronSize, bottom)
    }

    private fun calcNewStaffRect(selection: Selection): RectF {
        val staff = page.staves[selection.staffIndex]
        val left = (staff.barLines[0].x + staff.barLines[1].x - chevronSize) / 2
        val right = (staff.barLines[0].x + staff.barLines[1].x + chevronSize) / 2
        return RectF(left, staff.bottom, right, staff.bottom + chevronSize)
    }
}

class Selection(val staffIndex: Int, val barIndex: Int)

enum class TouchLocation {
    OUTSIDE,
    LOW_HANDLE,
    INSIDE,
    HIGH_HANDLE
}

enum class InitialTouch {
    FIRST_SELECTION,
    CURRENT_SELECTION,
    RESIZE_NW,
    RESIZE_NE,
    RESIZE_SW,
    RESIZE_SE,
    RESIZE_LEFT,
    RESIZE_RIGHT,
    RESIZE_TOP,
    RESIZE_BOTTOM,
    NEW_BAR,
    NEW_STAFF,
    CHANGE_OR_CANCEL_SELECTION
}

sealed class Motion

class ResizeCorner(fixedCorner: PointF, touch: PointF) : Motion()

class ResizeHorizontal(fixedX: Float, top: Float, bottom: Float, touch: Float) : Motion()

class ResizeVertical(fixedY: Float, left: Float, right: Float, touch: Float) : Motion()

// In the following two classes, touchOffset refers to the distance between the inital touch (on
// the chevron) and the boundary of the previously selected bar.

class NewBar(left: Float, top: Float, bottom: Float, touch: Float, touchOffset: Float) : Motion()

class NewStaff(left: Float, top: Float, right: Float, touch: Float, touchOffset: Float) : Motion()
