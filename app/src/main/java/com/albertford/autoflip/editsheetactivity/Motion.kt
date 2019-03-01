package com.albertford.autoflip.editsheetactivity

import android.graphics.PointF
import android.graphics.RectF
import com.albertford.autoflip.room.BarLine
import com.albertford.autoflip.room.Page
import com.albertford.autoflip.room.Staff

// touch is always a PointF and never just a Float. This is because, even when we are only resizing
// in one dimension, the other dimension is useful for knowing when to snap to other lines
sealed class Motion(var touch: PointF) {
    var moved: Boolean = false

    abstract fun onActionMove(page: Page, selection: Selection?, slop: Float)

    /**
     * Returns the new selection if it needs to be changed. The new selection can be invalid to
     * indicate the selection should be canceled.
     * Returns the current selection if it has been clicked.
     * Returns null if no changes to selection need to be made
     */
    abstract fun onActionUp(page: Page, selection: Selection?, slop: Float): MotionResult?
}

class ClickSelection(touch: PointF) : Motion(touch) {
    override fun onActionMove(page: Page, selection: Selection?, slop: Float) {
        // intentionally does nothing
    }

    override fun onActionUp(page: Page, selection: Selection?, slop: Float): MotionResult? {
        return if (!moved) {
            ClickSelectionResult
        } else {
            null
        }
    }
}

fun destroyBarIfTooSmall(staff: Staff, barIndex: Int, slop: Float, fixedX: Float): MotionResult? {
    val prevBarLine = staff.barLines.getOrNull(barIndex - 1)
    val leftBarLine = staff.barLines[barIndex]
    val rightBarLine = staff.barLines[barIndex + 1]
    val nextBarLine = staff.barLines.getOrNull(barIndex + 2)
    if (prevBarLine != null && leftBarLine.x - prevBarLine.x < slop) {
        staff.barLines.removeAt(barIndex)
        return CancelSelectionResult
    } else if (rightBarLine.x - leftBarLine.x < slop) {
        // delete the barline that was just moved, not the fixed one
        if (rightBarLine.x == fixedX) {
            staff.barLines.removeAt(barIndex)
        } else {
            staff.barLines.removeAt(barIndex + 1)
        }
        return CancelSelectionResult
    } else if (nextBarLine != null && nextBarLine.x - rightBarLine.x < slop) {
        staff.barLines.removeAt(barIndex + 1)
        return CancelSelectionResult
    }
    return null
}

class ResizeCorner(touch: PointF, private val fixedCorner: PointF) : Motion(touch) {
    override fun onActionMove(page: Page, selection: Selection?, slop: Float) {
        val selection = selection ?: return
        val staff = page.getStaff(selection)
        val left = Math.min(touch.x, fixedCorner.x)
        val top = Math.min(touch.y, fixedCorner.y)
        val right = Math.max(touch.x, fixedCorner.x)
        val bottom = Math.max(touch.y, fixedCorner.y)
        val rect = RectF(left, top, right, bottom)
        val clamped = rect
        staff.top = clamped.top
        staff.bottom = clamped.bottom
        val leftBar = staff.barLines[selection.barIndex]
        val rightBar = staff.barLines[selection.barIndex + 1]
        leftBar.x = clamped.left
        rightBar.x = clamped.right
    }

    override fun onActionUp(page: Page, selection: Selection?, slop: Float): MotionResult? {
        val selection = selection ?: return null
        val staff = page.getStaff(selection)
        return if (staff.bottom - staff.top < slop) {
            page.staves.removeAt(selection.staffIndex)
            CancelSelectionResult
        } else {
            destroyBarIfTooSmall(staff, selection.barIndex, slop, fixedCorner.x)
        }
    }
}

class ResizeHorizontal(touch: PointF, private val fixedX: Float) : Motion(touch) {
    override fun onActionMove(page: Page, selection: Selection?, slop: Float) {
        val selection = selection ?: return
        val staff = page.getStaff(selection)
        val left = Math.min(touch.x, fixedX)
        val right = Math.max(touch.x, fixedX)
        val rect = RectF(left, staff.top, right, staff.bottom)
        val clamped = rect
        val leftBar = staff.barLines[selection.barIndex]
        val rightBar = staff.barLines[selection.barIndex + 1]
        leftBar.x = clamped.left
        rightBar.x = clamped.right
    }

    override fun onActionUp(page: Page, selection: Selection?, slop: Float): MotionResult? {
        val selection = selection ?: return null
        val staff = page.getStaff(selection)
        return destroyBarIfTooSmall(staff, selection.barIndex, slop, fixedX)
    }
}

class ResizeVertical(touch: PointF, private val fixedY: Float) : Motion(touch) {
    override fun onActionMove(page: Page, selection: Selection?, slop: Float) {
        val selection = selection ?: return
        val staff = page.getStaff(selection)
        val leftBar = staff.barLines[selection.barIndex]
        val rightBar = staff.barLines[selection.barIndex + 1]
        val top = Math.min(touch.y, fixedY)
        val bottom = Math.max(touch.y, fixedY)
        val rect = RectF(leftBar.x, top, rightBar.x, bottom)
        val clamped = rect
        staff.top = clamped.top
        staff.bottom = clamped.bottom
    }

    override fun onActionUp(page: Page, selection: Selection?, slop: Float): MotionResult? {
        val selection = selection ?: return null
        val staff = page.getStaff(selection)
        return if (staff.bottom - staff.top < slop) {
            page.staves.removeAt(selection.staffIndex)
            CancelSelectionResult
        } else {
            null
        }
    }
}

// In the following two classes, touchOffset refers to the distance between the inital touch (on
// the chevron) and the boundary of the previously selected bar.

class NewBar(touch: PointF, private val touchOffset: Float) : Motion(touch) {
    override fun onActionMove(page: Page, selection: Selection?, slop: Float) {
        val selection = selection ?: return
        val staff = page.getStaff(selection)
        if (!moved) {
            val lastBarLine = staff.barLines.last()
            staff.barLines.add(BarLine(lastBarLine.x, lastBarLine.sheetId, lastBarLine.pageIndex, lastBarLine.staffIndex))
        }
        val secondLastBar = staff.barLines[staff.barLines.size - 2]
        val lastBar = staff.barLines.last()
        lastBar.x = Math.max(secondLastBar.x, touch.x - touchOffset)
    }

    override fun onActionUp(page: Page, selection: Selection?, slop: Float): MotionResult? {
        val selection = selection ?: return null
        val staff = page.getStaff(selection)
        val secondLastBarLine = staff.barLines[staff.barLines.size - 2]
        val lastBarLine = staff.barLines.last()
        return if (!moved) {
            val x = Math.min(1f, 2 * lastBarLine.x - secondLastBarLine.x)
            staff.barLines.add(BarLine(x, lastBarLine.sheetId, lastBarLine.pageIndex, lastBarLine.staffIndex))
            ChangeSelectionResult(Selection(selection.staffIndex, staff.barLines.size - 2))
        } else if (lastBarLine.x - secondLastBarLine.x < slop) {
            staff.barLines.removeAt(staff.barLines.size - 1)
            null
        } else {
            ChangeSelectionResult(Selection(selection.staffIndex, staff.barLines.size - 2))
        }
    }
}

class NewStaff(touch: PointF, private val touchOffset: Float) : Motion(touch) {
    override fun onActionMove(page: Page, selection: Selection?, slop: Float) {
        if (!moved) {
            val staff = page.staves.last()
            page.staves.add(Staff(staff.bottom, staff.bottom, staff.sheetId, staff.pageIndex))
        }
        val staff = page.staves.last()
        staff.bottom = Math.max(staff.top, touch.y - touchOffset)
    }

    override fun onActionUp(page: Page, selection: Selection?, slop: Float): MotionResult? {
        val lastStaff = page.staves.last()
        return if (!moved) {
            val top = lastStaff.bottom
            val bottom = Math.min(page.height.toFloat() / page.width, 2 * lastStaff.bottom - lastStaff.top)
            page.staves.add(Staff(top, bottom, lastStaff.sheetId, lastStaff.pageIndex))
            ChangeSelectionResult(Selection(page.staves.size - 1, 0))
        } else if (lastStaff.bottom - lastStaff.top < slop) {
            page.staves.removeAt(page.staves.size - 1)
            null
        } else {
            ChangeSelectionResult(Selection(page.staves.size - 1, 0))
        }
    }
}

class ChangeSelection(touch: PointF, private val newSelection: Selection) : Motion(touch) {
    override fun onActionMove(page: Page, selection: Selection?, slop: Float) {
        // intentionally does nothing
    }

    override fun onActionUp(page: Page, selection: Selection?, slop: Float): MotionResult? {
        return if (!moved) {
            ChangeSelectionResult(newSelection)
        } else {
            null
        }
    }
}

class CancelSelection(touch: PointF) : Motion(touch) {
    override fun onActionMove(page: Page, selection: Selection?, slop: Float) {
        // intentionally does nothing
    }

    override fun onActionUp(page: Page, selection: Selection?, slop: Float): MotionResult? {
        return CancelSelectionResult
    }
}

sealed class MotionResult

class ChangeSelectionResult(val newSelection: Selection) : MotionResult()

object ClickSelectionResult : MotionResult()

object CancelSelectionResult : MotionResult()
