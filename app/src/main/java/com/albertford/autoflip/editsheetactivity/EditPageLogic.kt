package com.albertford.autoflip.editsheetactivity

import android.graphics.PointF
import android.graphics.RectF
import com.albertford.autoflip.room.Page

// All dimensions and locations in this class are given relative to the width of document
// This way the same normalized slop value can be used in the x and y axes

class EditPageLogic(val page: Page, private val slop: Float, private val chevronSize: Float) {
    var selection: Selection? = null
    var motion: Motion? = null

    fun onActionDown(touch: PointF) {
        motion = if (page.staves.isEmpty()) {
            ResizeCorner(touch, touch)
        } else {
            verifySelection()
            val selection = selection
            if (selection == null) {
                createOtherMotion(touch)
            } else {
                val touchLocation = calcTouchLocation(touch, calcSelectionRect(selection))
                when {
                    touchLocation != null -> touchLocation
                    calcNewBarRect(selection)?.contains(touch.x, touch.y) == true -> createNewBarMotion(touch, selection)
                    calcNewStaffRect(selection)?.contains(touch.x, touch.y) == true -> createNewStaffMotion(touch, selection)
                    else -> createOtherMotion(touch)
                }
            }
        }
    }

    fun onActionMove(touch: PointF) {
        verifySelection()
        motion?.touch = touch
        motion?.onActionMove(page, selection)
        motion?.moved = true // make sure to set moved to true after calling onActionMove
    }

    fun onActionUp(touch: PointF) {
        onActionMove(touch) // keep this or not?
        val result = motion?.onActionUp(page, selection, slop)
        when (result) {
            is ChangeSelectionResult -> selection = result.newSelection
            ClickSelectionResult -> TODO()
            CancelSelectionResult -> selection = null
            null -> {}
        }
        verifySelection()
        motion = null
    }

    /**
     * Calculates which part of a target rectangle an initial touch corresponds to. If it is outside
     * the rectangle, null is returned. Otherwise it returns an enum for the inside of the rectangle
     * or one of the four corners or four edges.
     */
    fun calcTouchLocation(touch: PointF, rect: RectF): Motion? {
        val horizTouchLocation = calcTouchLocation(touch.x, rect.left, rect.right)
        val vertTouchLocation = calcTouchLocation(touch.y, rect.top, rect.bottom)
        return when (horizTouchLocation) {
            TouchLocation.OUTSIDE -> null
            TouchLocation.INSIDE -> when (vertTouchLocation) {
                TouchLocation.OUTSIDE -> null
                TouchLocation.INSIDE -> ClickSelection(touch)
                TouchLocation.LOW_HANDLE -> ResizeVertical(touch, rect.bottom)
                TouchLocation.HIGH_HANDLE -> ResizeVertical(touch, rect.top)
            }
            TouchLocation.LOW_HANDLE -> when (vertTouchLocation) {
                TouchLocation.OUTSIDE -> null
                TouchLocation.INSIDE -> ResizeHorizontal(touch, rect.right)
                TouchLocation.LOW_HANDLE -> ResizeCorner(touch, PointF(rect.right, rect.bottom))
                TouchLocation.HIGH_HANDLE -> ResizeCorner(touch, PointF(rect.right, rect.top))
            }
            TouchLocation.HIGH_HANDLE -> when (vertTouchLocation) {
                TouchLocation.OUTSIDE -> null
                TouchLocation.INSIDE -> ResizeHorizontal(touch, rect.left)
                TouchLocation.LOW_HANDLE -> ResizeCorner(touch, PointF(rect.left, rect.bottom))
                TouchLocation.HIGH_HANDLE -> ResizeCorner(touch, PointF(rect.left, rect.top))
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

    private fun calcNewBarRect(selection: Selection): RectF? {
        val staff = page.staves[selection.staffIndex]
        if (selection.barIndex != staff.barLines.size - 2) {
            return null
        }
        val lastBarLine = staff.barLines.last()
        val top = (staff.top + staff.bottom - chevronSize) / 2
        val bottom = (staff.top + staff.bottom + chevronSize) / 2
        return RectF(lastBarLine.x, top, lastBarLine.x + chevronSize, bottom)
    }

    private fun calcNewStaffRect(selection: Selection): RectF? {
        if (selection.staffIndex != page.staves.size - 1) {
            return null
        }
        val staff = page.staves.last()
        val left = (staff.barLines[0].x + staff.barLines[1].x - chevronSize) / 2
        val right = (staff.barLines[0].x + staff.barLines[1].x + chevronSize) / 2
        return RectF(left, staff.bottom, right, staff.bottom + chevronSize)
    }

    private fun createNewBarMotion(touch: PointF, selection: Selection): Motion {
        val staff = page.staves[selection.staffIndex]
        val rightBar = staff.barLines[selection.barIndex + 1]
        return NewBar(touch, touch.x - rightBar.x)
    }

    private fun createNewStaffMotion(touch: PointF, selection: Selection): Motion {
        val staff = page.staves[selection.staffIndex]
        return NewStaff(touch, touch.y - staff.bottom)
    }

    private fun createOtherMotion(touch: PointF): Motion {
        for (staffIndex in page.staves.indices) {
            val staff = page.staves[staffIndex]
            for (barIndex in 0..staff.barLines.size - 2) {
                val left = staff.barLines[barIndex].x
                val right = staff.barLines[barIndex + 1].x
                val rect = RectF(left, staff.top, right, staff.bottom)
                if (rect.contains(touch.x, touch.y)) {
                    return ChangeSelection(touch, Selection(staffIndex, barIndex))
                }
            }
        }
        return CancelSelection(touch)
    }
}

class Selection(val staffIndex: Int, val barIndex: Int)

enum class TouchLocation {
    OUTSIDE,
    LOW_HANDLE,
    INSIDE,
    HIGH_HANDLE
}
