package com.albertford.autoflip

import android.graphics.Canvas
import android.graphics.Paint
import android.os.Parcel
import android.os.Parcelable
import com.albertford.autoflip.models.Page
import com.albertford.autoflip.models.SheetPartition
import com.albertford.autoflip.models.Staff

private const val DEFAULT_STAFF_START = 0.1f
private const val DEFAULT_STAFF_END = 0.2f

private const val DEFAULT_BAR_START = 0.1f
private const val DEFAULT_BAR_END = 0.3f

interface Selection : Parcelable {
    override fun describeContents(): Int = 0

    fun move(page: Page, dx: Float, dy: Float)

    fun handleTouched(x: Float, y: Float, width: Int, height: Int): Boolean

    fun mask(canvas: Canvas, page: Page, paint: Paint)

    fun project(canvas: Canvas, sheetPartition: SheetPartition)

    fun drawHandles(canvas: Canvas, paint: Paint)

    fun save(sheetPartition: SheetPartition): Selection
}

private enum class Handle {
    START, END
}

class StaffSelection(var startY: Float, var endY: Float) : Selection {
    private var activeHandle = Handle.START

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

    override fun project(canvas: Canvas, sheetPartition: SheetPartition) {
        val (size, delta) = suggestProjectedStaff(sheetPartition, this)
        val period = size + delta
        projectHorizontal(canvas, startY, period)
        projectHorizontal(canvas, endY, period)
    }

    override fun drawHandles(canvas: Canvas, paint: Paint) {
        drawHorizontal(canvas, startY, 0f, 1f, paint)
        drawHorizontal(canvas, endY, 0f, 1f, paint)
    }

    override fun save(sheetPartition: SheetPartition): Selection {
        val newSelection = suggestFirstBar(sheetPartition)
        sheetPartition.pages.last().staves.add(Staff(startY, endY))
        return newSelection
    }

    private fun flip() {
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
        override fun createFromParcel(parcel: Parcel) = StaffSelection(parcel.readFloat(), parcel.readFloat())

        override fun newArray(size: Int): Array<StaffSelection?> = arrayOfNulls(size)
    }
}

class BarSelection(var startX: Float, var endX: Float) : Selection {
    private var activeHandle = Handle.START

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

    override fun project(canvas: Canvas, sheetPartition: SheetPartition) {
        val period = endX - startX
        val staff = sheetPartition.pages.last().staves.last()
        projectVertical(canvas, endX, period, staff.startY, staff.endY)
    }

    override fun drawHandles(canvas: Canvas, paint: Paint) {
        drawVertical(canvas, startX, 0f, 1f, paint)
        drawVertical(canvas, endX, 0f, 1f, paint)
    }

    override fun save(sheetPartition: SheetPartition): Selection {
        val lastStaff = sheetPartition.pages.last().staves.last()
        lastStaff.barLines.add(startX)
        lastStaff.barLines.add(endX)
        return suggestBarLine(lastStaff)
    }

    private fun flip() {
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
        override fun createFromParcel(parcel: Parcel) = BarSelection(parcel.readFloat(), parcel.readFloat())

        override fun newArray(size: Int): Array<BarSelection?> = arrayOfNulls(size)
    }
}

class BarLineSelection(var x: Float) : Selection {
    override fun move(page: Page, dx: Float, dy: Float) {
        val lastStaff = page.staves.last()
        x = clamp(x + dx, lastStaff.barLines.last(), 1f)
    }

    override fun handleTouched(x: Float, y: Float, width: Int, height: Int) = nearLine(x, this.x * width)

    override fun mask(canvas: Canvas, page: Page, paint: Paint) {
        val lastStaff = page.staves.last()
        maskStaff(canvas, lastStaff.startY, lastStaff.endY, paint)
        val startX = lastStaff.barLines.last()
        drawRect(canvas, 0f, lastStaff.startY, startX, lastStaff.endY, paint)
        drawRect(canvas, x, lastStaff.startY, 1f, lastStaff.endY, paint)
    }

    override fun project(canvas: Canvas, sheetPartition: SheetPartition) {
        val staff = sheetPartition.pages.last().staves.last()
        val period = x - staff.barLines.last()
        projectVertical(canvas, x, period, staff.startY, staff.endY)
    }

    override fun drawHandles(canvas: Canvas, paint: Paint) {
        drawVertical(canvas, x, 0f, 1f, paint)
    }

    override fun save(sheetPartition: SheetPartition): Selection {
        val lastStaff = sheetPartition.pages.last().staves.last()
        lastStaff.barLines.add(x)
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
        override fun createFromParcel(parcel: Parcel) = BarLineSelection(parcel.readFloat())

        override fun newArray(size: Int): Array<BarLineSelection?> = arrayOfNulls(size)
    }
}

fun suggestStaff(sheetPartition: SheetPartition): StaffSelection {
    val page = sheetPartition.pages.last()
    return when (page.staves.size) {
        0 -> suggestFirstStaff(sheetPartition)
        1 -> suggestSecondStaff(sheetPartition)
        else -> suggestOtherStaff(page)
    }
}

private fun suggestFirstStaff(sheetPartition: SheetPartition): StaffSelection {
    val lastPage = sheetPartition.pages[sheetPartition.pages.size - 1]
    return try {
        val refPage = sheetPartition.pages.last { it.staves.isNotEmpty() }
        val firstStaff = refPage.staves[0]
        StaffSelection(firstStaff.startY, firstStaff.endY).clipOverflow(lastPage)
    } catch (e: NoSuchElementException) {
        StaffSelection(DEFAULT_STAFF_START, DEFAULT_STAFF_END)
    }
}

private fun suggestSecondStaff(sheetPartition: SheetPartition): StaffSelection {
    val lastPage = sheetPartition.pages[sheetPartition.pages.size - 1]
    val lastStaff = lastPage.staves.last()
    val size = lastStaff.endY - lastStaff.startY
    val delta = try {
        val refPage = sheetPartition.pages.last { it.staves.size > 1 }
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

fun suggestProjectedStaff(sheetPartition: SheetPartition, selection: StaffSelection): Pair<Float, Float> {
    val page = sheetPartition.pages.last()
    val size = selection.endY - selection.startY
    val delta = if (page.staves.isEmpty()) {
        try {
            val refPage = sheetPartition.pages.last { it.staves.size > 1 }
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

fun suggestFirstBar(sheetPartition: SheetPartition): BarSelection {
    val lastPage = sheetPartition.pages.last()
    return if (lastPage.staves.isNotEmpty()) {
        suggestBarFromPage(lastPage).clipOverflow()
    } else {
        try {
            val refPage = sheetPartition.pages.last { it.staves.isNotEmpty() }
            suggestBarFromPage(refPage).clipOverflow()
        } catch (e: NoSuchElementException) {
            BarSelection(DEFAULT_BAR_START, DEFAULT_BAR_END)
        }
    }
}

private fun suggestBarFromPage(page: Page): BarSelection {
    val barLines = page.staves.last().barLines
    return BarSelection(barLines[0], barLines[1])
}

fun suggestBarLine(staff: Staff): BarLineSelection {
    val lineCount = staff.barLines.size
    val lastBarLine = staff.barLines[lineCount - 1]
    val secondLastBarLine = staff.barLines[lineCount - 2]
    return BarLineSelection(2 * lastBarLine - secondLastBarLine).clipOverflow()
}

fun maskStaff(canvas: Canvas, startY: Float, endY: Float, paint: Paint) {
    drawRect(canvas, 0f, 0f, 1f, startY, paint)
    drawRect(canvas, 0f, endY, 1f, 1f, paint)
}
