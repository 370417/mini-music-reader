package io.github.as_f.barpager

const val DEFAULT_STAFF_START = 0.1f
const val DEFAULT_STAFF_END = 0.25f

const val DEFAULT_BAR_START = 0.1f
const val DEFAULT_BAR_END = 0.3f

class Sheet {
  val pages = arrayListOf<Page>()
}

class Page(val width: Int, val height: Int) {
  val staves = arrayListOf<Staff>()
}

sealed class Selection {
  abstract fun flip(): Boolean
}

class Staff(var startY: Float, var endY: Float) : Selection() {
  val bars = arrayListOf<Bar>()

  override fun flip(): Boolean {
    if (startY > endY) {
      val oldStartY = startY
      startY = endY
      endY = oldStartY
      return true
    }
    return false
  }
}

fun suggestStaff(sheet: Sheet): Staff {
  var index = sheet.pages.size - 1
  val lastPage = sheet.pages[index]
  if (lastPage.staves.size > 1) {
    val lastStaff = lastPage.staves[lastPage.staves.size - 1]
    val penultimateStaff = lastPage.staves[lastPage.staves.size - 2]
    val startY = lastStaff.endY + lastStaff.startY - penultimateStaff.endY
    val size = lastStaff.endY - lastStaff.startY
    return Staff(startY, startY + size)
  } else if (lastPage.staves.size == 1) {
    val staff = lastPage.staves[0]
    return Staff(staff.endY, 2 * staff.endY - staff.startY)
  }
  while (sheet.pages[index].staves.size == 0) {
    index -= 1
    if (index == -1) {
      return Staff(lastPage.height * DEFAULT_STAFF_START, lastPage.height * DEFAULT_STAFF_END)
    }
  }
  val staff = sheet.pages[index].staves[0]
  return Staff(staff.startY, staff.endY)
}

class Bar(var startX: Float, var endX: Float) : Selection() {
  override fun flip(): Boolean {
    if (startX > endX) {
      val oldStartX = startX
      startX = endX
      endX = oldStartX
      return true
    }
    return false
  }
}

fun suggestBar(sheet: Sheet): Bar {
  var index = sheet.pages.size - 1
  val lastPage = sheet.pages[index]
  val lastStaff = lastPage.staves[lastPage.staves.size - 1]
  if (lastStaff.bars.size > 1) {
    val lastBar = lastStaff.bars[lastStaff.bars.size - 1]
    val penultimateBar = lastStaff.bars[lastStaff.bars.size - 2]
    val startX = lastBar.endX + lastBar.startX - penultimateBar.endX
    val size = lastBar.endX - lastBar.startX
    return Bar(startX, startX + size)
  } else if (lastStaff.bars.size == 1) {
    val lastBar = lastStaff.bars[lastStaff.bars.size - 1]
    return Bar(lastBar.endX, 2 * lastBar.endX - lastBar.startX)
  } else {
    val penultimateStaff = if (lastPage.staves.size > 1) {
      lastPage.staves[lastPage.staves.size - 2]
    } else {
      if (index == 0) {
        return Bar(lastPage.width * DEFAULT_BAR_START, lastPage.width * DEFAULT_BAR_END)
      } else {
        index -= 1
      }
      while (sheet.pages[index].staves.size == 0) {
        index -= 1
        if (index == -1) {
          return Bar(lastPage.width * DEFAULT_BAR_START, lastPage.width * DEFAULT_BAR_END)
        }
      }
      sheet.pages[index].staves[0]
    }
    val bar = penultimateStaff.bars[0]
    return Bar(bar.startX, bar.endX)
  }
}
