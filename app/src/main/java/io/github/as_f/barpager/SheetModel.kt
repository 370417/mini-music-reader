package io.github.as_f.barpager

class Sheet {
  val pages = arrayListOf<Page>()
}

class Page {
  val staves = arrayListOf<Staff>()
}

sealed class Selection {
  abstract fun clone(): Selection
}

class Staff(var startY: Float, var endY: Float) : Selection() {
  val bars = arrayListOf<Bar>()

  override fun clone(): Staff {
    return Staff(startY, endY)
  }
}

class Bar(var startX: Float, var endX: Float) : Selection() {
  override fun clone(): Bar {
    return Bar(startX, endX)
  }
}
