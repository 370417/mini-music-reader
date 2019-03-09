@file:Suppress("NAME_SHADOWING")

package com.albertford.autoflip.editsheetactivity

import android.graphics.PointF
import android.graphics.RectF
import com.albertford.autoflip.room.BarLine
import com.albertford.autoflip.room.Page
import com.albertford.autoflip.room.Staff

// Maximum overlap between two adjacent staves
// This is a fraction of the page width, so the logic works the same in both portait and landscape
const val STAFF_OVERLAP = 0.05f

// touch is always a PointF and never just a Float. This is because, even when we are only resizing
// in one dimension, the other dimension is useful for knowing when to snap to other lines
/**
 * Represents the user's current touch
 *
 * Each subclass represents a different initial touch target
 */
sealed class Motion(var touch: PointF) {
    var moved: Boolean = false

    abstract fun onActionMove(page: Page, selection: Selection?, slop: Float)

    abstract fun onActionUp(page: Page, selection: Selection?, slop: Float): MotionResult?
}

class ClickSelection(touch: PointF, private val clickedSelection: Selection) : Motion(touch) {
    override fun onActionMove(page: Page, selection: Selection?, slop: Float) {
        // intentionally does nothing
    }

    override fun onActionUp(page: Page, selection: Selection?, slop: Float): MotionResult? {
        return if (!moved) {
            ClickSelectionResult(clickedSelection)
        } else {
            AttemptedScrollResult
        }
    }
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
        clampBar(rect, page, selection)
        staff.top = rect.top
        staff.bottom = rect.bottom
        val leftBar = staff.barLines[selection.barIndex]
        val rightBar = staff.barLines[selection.barIndex + 1]
        leftBar.x = rect.left
        rightBar.x = rect.right
    }

    override fun onActionUp(page: Page, selection: Selection?, slop: Float): MotionResult? {
        val selection = selection ?: return null
        val staff = page.getStaff(selection)
        return if (staff.bottom - staff.top < slop) {
            page.staves.removeAt(selection.staffIndex)
            CancelSelectionResult
        } else {
            destroyBarIfTooSmall(page, selection, slop, fixedCorner.x)
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
        clampBarHorizontal(rect, page, selection)
        val leftBarLine = staff.barLines[selection.barIndex]
        val rightBarLine = staff.barLines[selection.barIndex + 1]
        leftBarLine.x = rect.left
        rightBarLine.x = rect.right
    }

    override fun onActionUp(page: Page, selection: Selection?, slop: Float): MotionResult? {
        val selection = selection ?: return null
        return destroyBarIfTooSmall(page, selection, slop, fixedX)
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
        clampBarVertical(rect, page, selection)
        staff.top = rect.top
        staff.bottom = rect.bottom
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
            ClickSelectionResult(Selection(selection.staffIndex, staff.barLines.size - 2))
        } else if (lastBarLine.x - secondLastBarLine.x < slop) {
            staff.barLines.removeAt(staff.barLines.indices.last)
            null
        } else {
            ClickSelectionResult(Selection(selection.staffIndex, staff.barIndices().last))
        }
    }
}

class NewStaff(touch: PointF, private val touchOffset: Float) : Motion(touch) {
    override fun onActionMove(page: Page, selection: Selection?, slop: Float) {
        if (!moved) {
            val staff = page.staves.last()
            val newStaffIndex = page.staves.size
            val firstBarLine = staff.barLines[0]
            val secondBarLine = staff.barLines[1]
            val newStaff = Staff(staff.bottom, staff.bottom, staff.sheetId, staff.pageIndex)
            newStaff.barLines.add(BarLine(firstBarLine.x, page.sheetId, page.pageIndex, newStaffIndex))
            newStaff.barLines.add(BarLine(secondBarLine.x, page.sheetId, page.pageIndex, newStaffIndex))
            page.staves.add(newStaff)
        }
        val staff = page.staves.last()
        staff.bottom = Math.max(staff.top, touch.y - touchOffset)
    }

    override fun onActionUp(page: Page, selection: Selection?, slop: Float): MotionResult? {
        val lastStaff = page.staves.last()
        val firstBarLine = lastStaff.barLines[0]
        val secondBarLine = lastStaff.barLines[1]
        return if (!moved) {
            val top = lastStaff.bottom
            val bottom = Math.min(page.height.toFloat() / page.width, 2 * lastStaff.bottom - lastStaff.top)
            val newStaff = Staff(top, bottom, page.sheetId, page.pageIndex)
            val newStaffIndex = page.staves.size
            newStaff.barLines.add(BarLine(firstBarLine.x, page.sheetId, page.pageIndex, newStaffIndex))
            newStaff.barLines.add(BarLine(secondBarLine.x, page.sheetId, page.pageIndex, newStaffIndex))
            page.staves.add(newStaff)
            ClickSelectionResult(Selection(page.staves.size - 1, 0))
        } else if (lastStaff.bottom - lastStaff.top < slop) {
            page.staves.removeAt(page.staves.size - 1)
            null
        } else {
            ClickSelectionResult(Selection(page.staves.size - 1, 0))
        }
    }
}

class ChangeSelection(touch: PointF, private val newSelection: Selection) : Motion(touch) {
    override fun onActionMove(page: Page, selection: Selection?, slop: Float) {
        // intentionally does nothing
    }

    override fun onActionUp(page: Page, selection: Selection?, slop: Float): MotionResult? {
        return if (!moved) {
            ClickSelectionResult(newSelection)
        } else {
            AttemptedScrollResult
        }
    }
}

class CancelSelection(touch: PointF) : Motion(touch) {
    override fun onActionMove(page: Page, selection: Selection?, slop: Float) {
        // intentionally does nothing
    }

    override fun onActionUp(page: Page, selection: Selection?, slop: Float): MotionResult? {
        return if (!moved) {
            CancelSelectionResult
        } else {
            AttemptedScrollResult
        }
    }
}

/** Represents the possible results after the user finishes a motion */
sealed class MotionResult

class ClickSelectionResult(val newSelection: Selection) : MotionResult()

object CancelSelectionResult : MotionResult()

object AttemptedScrollResult : MotionResult()

/** Destroy bar lines if they are too close to each other after being resized */
fun destroyBarIfTooSmall(page: Page, selection: Selection, slop: Float, fixedX: Float): MotionResult? {
    val staff = page.getStaff(selection)
    val barIndex = selection.barIndex
    val prevBarLine = staff.barLines.getOrNull(barIndex - 1)
    val leftBarLine = staff.barLines[barIndex]
    val rightBarLine = staff.barLines[barIndex + 1]
    val nextBarLine = staff.barLines.getOrNull(barIndex + 2)
    val result = if (prevBarLine != null && leftBarLine.x - prevBarLine.x < slop) {
        staff.barLines.removeAt(barIndex)
        CancelSelectionResult
    } else if (rightBarLine.x - leftBarLine.x < slop) {
        // delete the barline that was just moved, not the fixed one
        if (rightBarLine.x == fixedX) {
            staff.barLines.removeAt(barIndex)
        } else {
            staff.barLines.removeAt(barIndex + 1)
        }
        CancelSelectionResult
    } else if (nextBarLine != null && nextBarLine.x - rightBarLine.x < slop) {
        staff.barLines.removeAt(barIndex + 1)
        CancelSelectionResult
    } else {
        null
    }
    if (result is CancelSelectionResult && staff.barLines.size < 2) {
        page.staves.removeAt(selection.staffIndex)
    }
    return result
}

/** Modifies a rect (representing current selection) to make sure it is not too large */
fun clampBar(rect: RectF, page: Page, selection: Selection) {
    clampBarVertical(rect, page, selection)
    clampBarHorizontal(rect, page, selection)
}

/** Modifies a rect (representing current selection) to make sure it is not too large vertically */
fun clampBarVertical(rect: RectF, page: Page, selection: Selection) {
    val prevStaff = page.staves.getOrNull(selection.staffIndex - 1)
    val nextStaff = page.staves.getOrNull(selection.staffIndex + 1)
    if (prevStaff != null && rect.top < prevStaff.bottom - STAFF_OVERLAP) {
        rect.top = prevStaff.bottom - STAFF_OVERLAP
    }
    if (nextStaff != null && rect.bottom > nextStaff.top + STAFF_OVERLAP) {
        rect.bottom = nextStaff.top + STAFF_OVERLAP
    }
}

/** Modifies a rect (representing current selection) to make sure it is not too large horizontally */
fun clampBarHorizontal(rect: RectF, page: Page, selection: Selection) {
    val staff = page.getStaff(selection)
    val prevBarLine = staff.barLines.getOrNull(selection.barIndex - 1)
    val nextBarLine = staff.barLines.getOrNull(selection.barIndex + 2)
    if (prevBarLine != null && rect.left < prevBarLine.x) {
        rect.left = prevBarLine.x
    }
    if (nextBarLine != null && rect.right > nextBarLine.x) {
        rect.right = nextBarLine.x
    }
}
