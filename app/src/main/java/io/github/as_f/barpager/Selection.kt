package io.github.as_f.barpager

import android.graphics.Canvas
import android.graphics.Paint
import android.os.Parcel
import android.os.Parcelable
import io.github.as_f.barpager.models.Page
import io.github.as_f.barpager.models.Sheet
import io.github.as_f.barpager.models.Staff

const val DEFAULT_STAFF_START = 0.1f
const val DEFAULT_STAFF_END = 0.2f

const val DEFAULT_BAR_START = 0.1f
const val DEFAULT_BAR_END = 0.3f

sealed class Selection : Parcelable {
  override fun describeContents(): Int {
    return 0
  }

  abstract fun move(page: Page, dx: Float, dy: Float, width: Float, height: Float)

  abstract fun mask(canvas: Canvas, page: Page)

  abstract fun project(canvas: Canvas, sheet: Sheet)

  abstract fun drawHandles(canvas: Canvas, page: Page, paint: Paint)
}

enum class Handle {
  START, END
}

class StaffSelection(var startY: Float, var endY: Float) : Selection() {
  var activeHandle = Handle.START

  override fun move(page: Page, dx: Float, dy: Float, width: Float, height: Float) {
    when (activeHandle) {
      Handle.START -> startY = clamp(startY + dy, 0f, height)
      Handle.END -> endY = clamp(endY + dy, 0f, height)
    }
    if (startY > endY) {
      flip()
    }
  }

  override fun mask(canvas: Canvas, page: Page) {
    maskStaff(canvas, startY, endY, page)
  }

  override fun project(canvas: Canvas, sheet: Sheet) {
    val (size, delta) = suggestProjectedStaff(sheet, this)
    val period = size + delta
    if (Math.abs(period) > MINIMUM_PROJECTION) {
      val paint = fadePaint(Math.abs(period))
      projectHorizontal(canvas, startY, period, paint)
      projectHorizontal(canvas, endY, period, paint)
    }
  }

  override fun drawHandles(canvas: Canvas, page: Page, paint: Paint) {
    drawHorizontal(canvas, startY, 0f, page.width.toFloat(), paint)
    drawHorizontal(canvas, endY, 0f, page.width.toFloat(), paint)
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
    if (endY > page.height) {
      endY = page.height.toFloat()
    }
    if (startY > page.height) {
      val lastStaff = page.staves[page.staves.size - 1]
      startY = lastStaff.endY
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

class BarSelection(var startX: Float, var endX: Float) : Selection() {
  var activeHandle = Handle.START

  override fun move(page: Page, dx: Float, dy: Float, width: Float, height: Float) {
    when (activeHandle) {
      Handle.START -> startX = clamp(startX + dx, 0f, width)
      Handle.END -> endX = clamp(endX + dx, 0f, width)
    }
    if (startX > endX) {
      flip()
    }
  }

  override fun mask(canvas: Canvas, page: Page) {
    val lastStaff = page.staves.last()
    maskStaff(canvas, lastStaff.startY, lastStaff.endY, page)
    canvas.drawRect(0f, lastStaff.startY, startX, lastStaff.endY, black)
    canvas.drawRect(endX, lastStaff.startY, page.width.toFloat(), lastStaff.endY, black)
  }

  override fun project(canvas: Canvas, sheet: Sheet) {
    val period = endX - startX
    if (Math.abs(period) > MINIMUM_PROJECTION) {
      val staff = sheet.pages.last().staves.last()
      val paint = fadePaint(Math.abs(period))
      projectVertical(canvas, endX, period, staff.startY, staff.endY, paint)
    }
  }

  override fun drawHandles(canvas: Canvas, page: Page, paint: Paint) {
    drawVertical(canvas, startX, 0f, page.height.toFloat(), paint)
    drawVertical(canvas, endX, 0f, page.height.toFloat(), paint)
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

  fun clipOverflow(page: Page): BarSelection {
    if (endX > page.width) {
      endX = page.width.toFloat()
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

class BarLineSelection(var x: Float) : Selection() {
  override fun move(page: Page, dx: Float, dy: Float, width: Float, height: Float) {
    val lastStaff = page.staves.last()
    x = clamp(x + dx, lastStaff.barLines.last().x, width)
  }

  override fun mask(canvas: Canvas, page: Page) {
    val lastStaff = page.staves.last()
    maskStaff(canvas, lastStaff.startY, lastStaff.endY, page)
    val startX = lastStaff.barLines.last().x
    canvas.drawRect(0f, lastStaff.startY, startX, lastStaff.endY, black)
    canvas.drawRect(x, lastStaff.startY, page.width.toFloat(), lastStaff.endY, black)
  }

  override fun project(canvas: Canvas, sheet: Sheet) {
    val staff = sheet.pages.last().staves.last()
    val period = x - staff.barLines.last().x
    if (Math.abs(period) > MINIMUM_PROJECTION) {
      val paint = fadePaint(Math.abs(period))
      projectVertical(canvas, x, period, staff.startY, staff.endY, paint)
    }
  }

  override fun drawHandles(canvas: Canvas, page: Page, paint: Paint) {
    drawVertical(canvas, x, 0f, page.height.toFloat(), paint)
  }

  fun clipOverflow(page: Page): BarLineSelection {
    if (x > page.width) {
      x = page.width.toFloat()
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
    StaffSelection(
        lastPage.height * DEFAULT_STAFF_START,
        lastPage.height * DEFAULT_STAFF_END)
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
    suggestBarFromPage(lastPage).clipOverflow(lastPage)
  } else {
    try {
      val refPage = sheet.pages.last { it.staves.isNotEmpty() }
      suggestBarFromPage(refPage).clipOverflow(lastPage)
    } catch (e: NoSuchElementException) {
      BarSelection(
          lastPage.width * DEFAULT_BAR_START,
          lastPage.width * DEFAULT_BAR_END)
    }
  }
}

private fun suggestBarFromPage(page: Page): BarSelection {
  val barLines = page.staves.last().barLines
  return BarSelection(barLines[0].x, barLines[1].x)
}

fun suggestBarLine(page: Page, staff: Staff): BarLineSelection {
  val lineCount = staff.barLines.size
  val lastBarLine = staff.barLines[lineCount - 1].x
  val secondLastBarLine = staff.barLines[lineCount - 2].x
  return BarLineSelection(2 * lastBarLine - secondLastBarLine).clipOverflow(page)
}

private fun maskStaff(canvas: Canvas, startY: Float, endY: Float, page: Page) {
  canvas.drawRect(0f, 0f, page.width.toFloat(), startY, black)
  canvas.drawRect(0f, endY, page.width.toFloat(), page.height.toFloat(), black)
}
