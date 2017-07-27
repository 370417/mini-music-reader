package com.albertford.autoflip

import android.graphics.Canvas
import android.graphics.Paint
import android.os.Parcel
import android.os.Parcelable
import com.albertford.autoflip.models.BarLine
import com.albertford.autoflip.models.Page
import com.albertford.autoflip.models.Sheet
import com.albertford.autoflip.models.Staff

const val DEFAULT_STAFF_START = 0.1f
const val DEFAULT_STAFF_END = 0.2f

const val DEFAULT_BAR_START = 0.1f
const val DEFAULT_BAR_END = 0.3f

interface Selection : Parcelable {
    override fun describeContents(): Int {
        return 0
    }

    fun move(page: Page, dx: Float, dy: Float)

    fun handleTouched(x: Float, y: Float, width: Int, height: Int): Boolean

    fun mask(canvas: Canvas, page: Page, paint: Paint)

    fun project(canvas: Canvas, sheet: Sheet)

    fun drawHandles(canvas: Canvas, paint: Paint)

    fun save(sheet: Sheet): Selection
}

enum class Handle {
    START, END
}

class StaffSelection(var startY: Float, var endY: Float) : Selection {
    var activeHandle = Handle.START

    override fun move(page: Page, dx: Float, dy: Float) {
        when (activeHandle) {
            Handle.START -> startY = clamp(startY + dy, 0f, 1f)
            Handle.END -> endY = clamp(endY + dy, 0f, 1f)
        }
        if (startY > endY) {
            flip()
        }
    }

    override fun handleTouched(x: Float, y: Float, width: Int, height: Int): Boolean {
        return if (nearLine(y, startY * height)) {
            activeHandle = Handle.START
            true
        } else if (nearLine(y, endY * height)) {
            activeHandle = Handle.END
            true
        } else {
            false
        }
    }

    override fun mask(canvas: Canvas, page: Page, paint: Paint) {
        maskStaff(canvas, startY, endY, paint)
    }

    override fun project(canvas: Canvas, sheet: Sheet) {
        val (size, delta) = suggestProjectedStaff(sheet, this)
        val period = size + delta
        projectHorizontal(canvas, startY, period)
        projectHorizontal(canvas, endY, period)
    }

    override fun drawHandles(canvas: Canvas, paint: Paint) {
        drawHorizontal(canvas, startY, 0f, 1f, paint)
        drawHorizontal(canvas, endY, 0f, 1f, paint)
    }

    override fun save(sheet: Sheet): Selection {
        val newSelection = suggestFirstBar(sheet)
        sheet.pages.last().staves.add(Staff(startY, endY))
        return newSelection
    }

    fun flip() {
        val temp = startY
        startY = endY
        endY = temp
        activeHandle = when (activeHandle) {
            Handle.START -> Handle.END
            Handle.END -> Handle.START
        }
    }

    fun clipOverflow(page: Page): StaffSelection {
        if (endY > 1f) {
            endY = 1f
        }
        if (startY > 1f) {
            startY = page.staves.last().endY
        }
        return this
    }

    override fun writeToParcel(output: Parcel?, flags: Int) {
        output?.writeFloat(startY)
        output?.writeFloat(endY)
    }

    companion object CREATOR : Parcelable.Creator<StaffSelection> {
        override fun createFromParcel(parcel: Parcel): StaffSelection {
            return StaffSelection(parcel.readFloat(), parcel.readFloat())
        }

        override fun newArray(size: Int): Array<StaffSelection?> {
            return arrayOfNulls(size)
        }
    }
}

class BarSelection(var startX: Float, var endX: Float) : Selection {
    var activeHandle = Handle.START

    override fun move(page: Page, dx: Float, dy: Float) {
        when (activeHandle) {
            Handle.START -> startX = clamp(startX + dx, 0f, 1f)
            Handle.END -> endX = clamp(endX + dx, 0f, 1f)
        }
        if (startX > endX) {
            flip()
        }
    }

    override fun handleTouched(x: Float, y: Float, width: Int, height: Int): Boolean {
        return if (nearLine(x, startX * width)) {
            activeHandle = Handle.START
            true
        } else if (nearLine(x, endX * width)) {
            activeHandle = Handle.END
            true
        } else {
            false
        }
    }

    override fun mask(canvas: Canvas, page: Page, paint: Paint) {
        val lastStaff = page.staves.last()
        maskStaff(canvas, lastStaff.startY, lastStaff.endY, paint)
        drawRect(canvas, 0f, lastStaff.startY, startX, lastStaff.endY, paint)
        drawRect(canvas, endX, lastStaff.startY, 1f, lastStaff.endY, paint)
    }

    override fun project(canvas: Canvas, sheet: Sheet) {
        val period = endX - startX
        val staff = sheet.pages.last().staves.last()
        projectVertical(canvas, endX, period, staff.startY, staff.endY)
    }

    override fun drawHandles(canvas: Canvas, paint: Paint) {
        drawVertical(canvas, startX, 0f, 1f, paint)
        drawVertical(canvas, endX, 0f, 1f, paint)
    }

    override fun save(sheet: Sheet): Selection {
        val lastStaff = sheet.pages.last().staves.last()
        lastStaff.barLines.add(BarLine(startX))
        lastStaff.barLines.add(BarLine(endX))
        return suggestBarLine(lastStaff)
    }

    fun flip() {
        val temp = startX
        startX = endX
        endX = temp
        activeHandle = when (activeHandle) {
            Handle.START -> Handle.END
            Handle.END -> Handle.START
        }
    }

    fun clipOverflow(): BarSelection {
        if (endX > 1f) {
            endX = 1f
        }
        return this
    }

    override fun writeToParcel(output: Parcel?, flags: Int) {
        output?.writeFloat(startX)
        output?.writeFloat(endX)
    }

    companion object CREATOR : Parcelable.Creator<BarSelection> {
        override fun createFromParcel(parcel: Parcel): BarSelection {
            return BarSelection(parcel.readFloat(), parcel.readFloat())
        }

        override fun newArray(size: Int): Array<BarSelection?> {
            return arrayOfNulls(size)
        }
    }
}

class BarLineSelection(var x: Float) : Selection {
    override fun move(page: Page, dx: Float, dy: Float) {
        val lastStaff = page.staves.last()
        x = clamp(x + dx, lastStaff.barLines.last().x, 1f)
    }

    override fun handleTouched(x: Float, y: Float, width: Int, height: Int): Boolean {
        return nearLine(x, this.x * width)
    }

    override fun mask(canvas: Canvas, page: Page, paint: Paint) {
        val lastStaff = page.staves.last()
        maskStaff(canvas, lastStaff.startY, lastStaff.endY, paint)
        val startX = lastStaff.barLines.last().x
        drawRect(canvas, 0f, lastStaff.startY, startX, lastStaff.endY, paint)
        drawRect(canvas, x, lastStaff.startY, 1f, lastStaff.endY, paint)
    }

    override fun project(canvas: Canvas, sheet: Sheet) {
        val staff = sheet.pages.last().staves.last()
        val period = x - staff.barLines.last().x
        projectVertical(canvas, x, period, staff.startY, staff.endY)
    }

    override fun drawHandles(canvas: Canvas, paint: Paint) {
        drawVertical(canvas, x, 0f, 1f, paint)
    }

    override fun save(sheet: Sheet): Selection {
        val lastStaff = sheet.pages.last().staves.last()
        lastStaff.barLines.add(BarLine(x))
        return suggestBarLine(lastStaff)
    }

    fun clipOverflow(): BarLineSelection {
        if (x > 1f) {
            x = 1f
        }
        return this
    }

    override fun writeToParcel(output: Parcel?, flags: Int) {
        output?.writeFloat(x)
    }

    companion object CREATOR : Parcelable.Creator<BarLineSelection> {
        override fun createFromParcel(parcel: Parcel): BarLineSelection {
            return BarLineSelection(parcel.readFloat())
        }

        override fun newArray(size: Int): Array<BarLineSelection?> {
            return arrayOfNulls(size)
        }
    }
}

fun suggestStaff(sheet: Sheet): StaffSelection {
    val page = sheet.pages.last()
    return when (page.staves.size) {
        0 -> suggestFirstStaff(sheet)
        1 -> suggestSecondStaff(sheet)
        else -> suggestOtherStaff(page)
    }
}

private fun suggestFirstStaff(sheet: Sheet): StaffSelection {
    val lastPage = sheet.pages[sheet.pages.size - 1]
    return try {
        val refPage = sheet.pages.last { it.staves.isNotEmpty() }
        val firstStaff = refPage.staves[0]
        StaffSelection(firstStaff.startY, firstStaff.endY).clipOverflow(lastPage)
    } catch (e: NoSuchElementException) {
        StaffSelection(DEFAULT_STAFF_START, DEFAULT_STAFF_END)
    }
}

private fun suggestSecondStaff(sheet: Sheet): StaffSelection {
    val lastPage = sheet.pages[sheet.pages.size - 1]
    val lastStaff = lastPage.staves.last()
    val size = lastStaff.endY - lastStaff.startY
    val delta = try {
        val refPage = sheet.pages.last { it.staves.size > 1 }
        refPage.staves[1].startY - refPage.staves[0].endY
    } catch (e: NoSuchElementException) {
        0f
    }
    return StaffSelection(lastStaff.endY + delta,
            lastStaff.endY + delta + size).clipOverflow(lastPage)
}

private fun suggestOtherStaff(page: Page): StaffSelection {
    val staffCount = page.staves.size
    val lastStaff = page.staves[staffCount - 1]
    val secondLastStaff = page.staves[staffCount - 2]
    val size = lastStaff.endY - lastStaff.startY
    val delta = lastStaff.startY - secondLastStaff.endY
    return StaffSelection(lastStaff.endY + delta,
            lastStaff.endY + delta + size).clipOverflow(page)
}

fun suggestProjectedStaff(sheet: Sheet, selection: StaffSelection): Pair<Float, Float> {
    val page = sheet.pages.last()
    val size = selection.endY - selection.startY
    val delta = if (page.staves.isEmpty()) {
        try {
            val refPage = sheet.pages.last { it.staves.size > 1 }
            refPage.staves[1].startY - refPage.staves[0].endY
        } catch (e: NoSuchElementException) {
            0f
        }
    } else {
        val lastStaff = page.staves.last()
        selection.startY - lastStaff.endY
    }
    return Pair(size, delta)
}

fun suggestFirstBar(sheet: Sheet): BarSelection {
    val lastPage = sheet.pages.last()
    return if (lastPage.staves.isNotEmpty()) {
        suggestBarFromPage(lastPage).clipOverflow()
    } else {
        try {
            val refPage = sheet.pages.last { it.staves.isNotEmpty() }
            suggestBarFromPage(refPage).clipOverflow()
        } catch (e: NoSuchElementException) {
            BarSelection(DEFAULT_BAR_START, DEFAULT_BAR_END)
        }
    }
}

private fun suggestBarFromPage(page: Page): BarSelection {
    val barLines = page.staves.last().barLines
    return BarSelection(barLines[0].x, barLines[1].x)
}

fun suggestBarLine(staff: Staff): BarLineSelection {
    val lineCount = staff.barLines.size
    val lastBarLine = staff.barLines[lineCount - 1].x
    val secondLastBarLine = staff.barLines[lineCount - 2].x
    return BarLineSelection(2 * lastBarLine - secondLastBarLine).clipOverflow()
}

fun maskStaff(canvas: Canvas, startY: Float, endY: Float, paint: Paint) {
    drawRect(canvas, 0f, 0f, 1f, startY, paint)
    drawRect(canvas, 0f, endY, 1f, 1f, paint)
}
