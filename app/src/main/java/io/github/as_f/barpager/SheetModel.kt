package io.github.as_f.barpager

import android.os.Parcel
import android.os.Parcelable
import io.github.as_f.barpager.models.Page
import io.github.as_f.barpager.models.Sheet
import io.github.as_f.barpager.models.Staff

const val DEFAULT_STAFF_START = 0.1f
const val DEFAULT_STAFF_END = 0.2f

const val DEFAULT_BAR_START = 0.1f
const val DEFAULT_BAR_END = 0.3f

//open class Sheet(var name: String, @PrimaryKey var uri: String, var bpm: Float, var bpb: Int) : RealmObject() {
//  constructor() : this("", "", 0f, 0)
//
//  var pages = RealmList<Page>()
//}

//open class Page(var width: Int, var height: Int) : RealmObject() {
//  constructor() : this(0, 0)
//
//  var staves = RealmList<Staff>()
//}

//open class Staff(var startY: Float, var endY: Float) : RealmObject() {
//  constructor() : this(0f, 0f)
//
//  var barLines = RealmList<BarLine>()
//}

//open class BarLine(var x: Float) : RealmObject() {
//  constructor() : this(0f)
//}

sealed class Selection : Parcelable {
  override fun describeContents(): Int {
    return 0
  }
}

enum class Handle {
  START, END
}

class StaffSelection(var startY: Float, var endY: Float) : Selection() {
  var activeHandle = Handle.START

  fun flip() {
    val temp = startY
    startY = endY
    endY = temp
    activeHandle = when (activeHandle) {
      Handle.START -> Handle.END
      Handle.END -> Handle.START
    }
  }

  fun preventOverflow(page: Page): StaffSelection {
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

fun suggestStaff(sheet: Sheet): StaffSelection {
  val page = sheet.pages.last()
  return when (page.staves.size) {
    0 -> suggestFirstStaff(sheet)
    1 -> suggestSecondStaff(sheet)
    else -> suggestOtherStaff(page)
  }
}

fun suggestFirstStaff(sheet: Sheet): StaffSelection {
  val lastPage = sheet.pages[sheet.pages.size - 1]
  return try {
    val refPage = sheet.pages.last { it.staves.isNotEmpty() }
    val firstStaff = refPage.staves[0]
    StaffSelection(firstStaff.startY, firstStaff.endY).preventOverflow(lastPage)
  } catch (e: NoSuchElementException) {
    StaffSelection(lastPage.height * DEFAULT_STAFF_START, lastPage.height * DEFAULT_STAFF_END)
  }
}

fun suggestSecondStaff(sheet: Sheet): StaffSelection {
  val lastPage = sheet.pages[sheet.pages.size - 1]
  val lastStaff = lastPage.staves.last()
  val size = lastStaff.endY - lastStaff.startY
  val delta = try {
    val refPage = sheet.pages.last { it.staves.size > 1 }
    refPage.staves[1].startY - refPage.staves[0].endY
  } catch (e: NoSuchElementException) {
    0f
  }
  return StaffSelection(lastStaff.endY + delta, lastStaff.endY + delta + size).preventOverflow(lastPage)
}

fun suggestOtherStaff(page: Page): StaffSelection {
  val staffCount = page.staves.size
  val lastStaff = page.staves[staffCount - 1]
  val secondLastStaff = page.staves[staffCount - 2]
  val size = lastStaff.endY - lastStaff.startY
  val delta = lastStaff.startY - secondLastStaff.endY
  return StaffSelection(lastStaff.endY + delta, lastStaff.endY + delta + size).preventOverflow(page)
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

class BarSelection(var startX: Float, var endX: Float) : Selection() {
  var activeHandle = Handle.START

  fun flip() {
    val temp = startX
    startX = endX
    endX = temp
    activeHandle = when (activeHandle) {
      Handle.START -> Handle.END
      Handle.END -> Handle.START
    }
  }

  fun preventOverflow(page: Page): BarSelection {
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
  fun preventOverflow(page: Page): BarLineSelection {
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

fun suggestFirstBar(sheet: Sheet): BarSelection {
  val lastPage = sheet.pages.last()
  return if (lastPage.staves.isNotEmpty()) {
    suggestBarFromPage(lastPage).preventOverflow(lastPage)
  } else {
    try {
      val refPage = sheet.pages.last { it.staves.isNotEmpty() }
      suggestBarFromPage(refPage).preventOverflow(lastPage)
    } catch (e: NoSuchElementException) {
      BarSelection(lastPage.width * DEFAULT_BAR_START, lastPage.width * DEFAULT_BAR_END)
    }
  }
}

fun suggestBarFromPage(page: Page): BarSelection {
  val barLines = page.staves.last().barLines
  return BarSelection(barLines[0].x, barLines[1].x)
}

fun suggestBarLine(page: Page, staff: Staff): BarLineSelection {
  val lineCount = staff.barLines.size
  val lastBarLine = staff.barLines[lineCount - 1].x
  val secondLastBarLine = staff.barLines[lineCount - 2].x
  return BarLineSelection(2 * lastBarLine - secondLastBarLine).preventOverflow(page)
}
