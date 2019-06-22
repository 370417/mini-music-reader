package com.albertford.autoflip.editsheetactivity

import android.graphics.PointF
import android.graphics.RectF
import com.albertford.autoflip.room.BarLine
import com.albertford.autoflip.room.Page
import com.albertford.autoflip.room.Staff

// All dimensions and locations in this class are given relative to the width of document
// This way the same normalized slop value can be used in the x and y axes

class EditPageLogic(val page: Page, private val slop: Float, private val chevronSize: Float) {
    var selection: Selection? = null
    var motion: Motion? = null
    var editable = false

    val observers: MutableSet<EditPageObserver> = mutableSetOf()

    fun onActionDown(touch: PointF) {
        motion = if (page.staves.isEmpty()) {
            // create the first bar
            val staff = Staff(touch.y, touch.y, page.sheetId, page.pageIndex)
            staff.barLines.add(BarLine(touch.x, page.sheetId, page.pageIndex, 0))
            staff.barLines.add(BarLine(touch.x, page.sheetId, page.pageIndex, 0))
            page.staves.add(staff)
            selection = Selection(0, 0)
            ResizeCorner(touch, touch)
        } else {
            verifySelection()
            val selection = selection
            if (selection == null) {
                createNonSelectionMotion(touch)
            } else {
                val touchLocation = calcTouchLocation(touch, selection)
                when {
                    touchLocation != null -> touchLocation
                    calcNewBarRect()?.contains(touch.x, touch.y) == true -> createNewBarMotion(touch, selection)
                    calcNewStaffRect()?.contains(touch.x, touch.y) == true -> createNewStaffMotion(touch, selection)
                    else -> createNonSelectionMotion(touch)
                }
            }
        }
    }

    fun onActionMove(touch: PointF) {
        if (!editable) {
            motion?.moved = true
            return
        }
        verifySelection()
        motion?.touch = touch
        motion?.onActionMove(page, selection, slop)
        motion?.moved = true // make sure to set moved to true after calling onActionMove
    }

    fun onActionUp() {
        when (val result = motion?.onActionUp(page, selection, slop)) {
            is ClickSelectionResult -> {
                if (editable) {
                    selection = result.newSelection
                } else {
                    for (observer in observers) {
                        observer.onClickBar(
                                page.pageIndex,
                                result.newSelection.staffIndex,
                                result.newSelection.barIndex
                        )
                    }
                }
            }
            CancelSelectionResult -> {
                selection = null
                for (observer in observers) {
                    observer.onCancelSelection()
                }
            }
            AttemptedScrollResult -> for (observer in observers) {
                observer.onScrollAttempt()
            }
        }
        verifySelection()
        motion = null
    }

    /**
     * Calculates which part of a target rectangle an initial touch corresponds to. If it is outside
     * the rectangle, null is returned. Otherwise it returns an enum for the inside of the rectangle
     * or one of the four corners or four edges.
     */
    private fun calcTouchLocation(touch: PointF, selection: Selection): Motion? {
        val rect = calcSelectionRect(selection)
        val horizTouchLocation = calcTouchLocation(touch.x, rect.left, rect.right)
        val vertTouchLocation = calcTouchLocation(touch.y, rect.top, rect.bottom)
        return when (horizTouchLocation) {
            TouchLocation.OUTSIDE -> null
            TouchLocation.INSIDE -> when (vertTouchLocation) {
                TouchLocation.OUTSIDE -> null
                TouchLocation.INSIDE -> ClickSelection(touch, selection)
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
        } else if (selection.barIndex > staff.barLines.size - 2) {
            this.selection = null
        }
    }

    /**
     * Calculate the bounds of the current selection.
     *
     * This assumes the selection is valid
     */
    private fun calcSelectionRect(selection: Selection): RectF {
        val staff = page.getStaff(selection)
        val leftBarLine = staff.barLines[selection.barIndex]
        val rightBarLine = staff.barLines[selection.barIndex + 1]
        return RectF(leftBarLine.x, staff.top, rightBarLine.x, staff.bottom)
    }

    // version of calcnewbarrect that does not allocate a new rect.
    // instead, it returns true if the rect exists and false if it should be treated as null
    /**
     * Calculates the dimensions of the button for creating a new bar.
     *
     * This modifies a rect instead of creating one to avoid allocation.
     * Returns true if the button should exist, false otherwise
     */
    fun calcNewBarRect(rect: RectF): Boolean {
        when (motion) {
            is ResizeCorner, is ResizeHorizontal, is ResizeVertical -> return false
        }
        val selection = selection ?: return false
        val staff = page.getStaff(selection)
        return if (motion is NewBar || selection.barIndex == staff.barIndices().last) {
            val lastBarLine = staff.barLines.last()
            rect.left = lastBarLine.x
            rect.top = (staff.top + staff.bottom - chevronSize) / 2
            rect.right = lastBarLine.x + chevronSize
            rect.bottom = (staff.top + staff.bottom + chevronSize) / 2
            true
        } else {
            false
        }
    }

    /** Wrapper around calcNewBarRect(rect) that allocates its own rect for convenience */
    private fun calcNewBarRect(): RectF? {
        val rect = RectF()
        return if (calcNewBarRect(rect)) {
            rect
        } else {
            null
        }
    }

    /**
     * Calculates the dimensions of the button for creating a new staff.
     *
     * This modifies a rect instead of creating one to avoid allocation.
     * Returns true if the button should exist, false otherwise
     */
    fun calcNewStaffRect(rect: RectF): Boolean {
        when (motion) {
            is ResizeCorner, is ResizeHorizontal, is ResizeVertical -> return false
        }
        val selection = selection ?: return false
        return if (motion is NewStaff || selection.staffIndex == page.staves.indices.last) {
            val staff = page.staves.last()
            rect.left = (staff.barLines[0].x + staff.barLines[1].x - chevronSize) / 2
            rect.top = staff.bottom
            rect.right = (staff.barLines[0].x + staff.barLines[1].x + chevronSize) / 2
            rect.bottom = staff.bottom + chevronSize
            true
        } else {
            false
        }
    }

    /** Wrapper around calcNewStaff(rect) that allocates its own rect for convenience */
    private fun calcNewStaffRect(): RectF? {
        val rect = RectF()
        return if (calcNewStaffRect(rect)) {
            rect
        } else {
            null
        }
    }

    private fun createNewBarMotion(touch: PointF, selection: Selection): Motion {
        val staff = page.getStaff(selection)
        val rightBar = staff.barLines[selection.barIndex + 1]
        return NewBar(touch, touch.x - rightBar.x)
    }

    private fun createNewStaffMotion(touch: PointF, selection: Selection): Motion {
        val staff = page.getStaff(selection)
        return NewStaff(touch, touch.y - staff.bottom)
    }

    /**
     * Create a motion from a touch location that ignores the current selection.
     *
     * If a previously-defined bar was touched, returns ChangeSelection
     * Otherwise, returns CancelSelection
     */
    private fun createNonSelectionMotion(touch: PointF): Motion {
        for (staffIndex in page.staves.indices) {
            val staff = page.staves[staffIndex]
            for (barIndex in staff.barIndices()) {
                val bar = staff.getBar(barIndex)
                val rect = RectF(bar.left, staff.top, bar.right, staff.bottom)
                if (rect.contains(touch.x, touch.y)) {
                    return ChangeSelection(touch, Selection(staffIndex, barIndex))
                }
            }
        }
        return CancelSelection(touch)
    }
}

/** Represents the index of the current selection */
data class Selection(val staffIndex: Int, val barIndex: Int)

enum class TouchLocation {
    OUTSIDE,
    LOW_HANDLE,
    INSIDE,
    HIGH_HANDLE
}

interface EditPageObserver {
    fun onClickBar(pageIndex: Int, staffIndex: Int, barIndex: Int)
    fun onCancelSelection()
    fun onScrollAttempt()
}
