package io.github.as_f.barpager

class Sheet {
  val pages = arrayListOf<Page>()
}

class Page {
  val lines = arrayListOf<Line>()
}

sealed class Selection

class Line(var starty: Float, var endy: Float) : Selection() {
  val bars = arrayListOf<Bar>()
}

class Bar(var startx: Float, var endx: Float) : Selection() {

}
