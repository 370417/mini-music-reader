package io.github.as_f.barpager

import android.graphics.Point
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import io.realm.Realm
import kotlinx.android.synthetic.main.activity_measure_sheet.*

class MeasureSheetActivity : AppCompatActivity() {

  var leftButtonText = LeftButtonText.SAVE_STAFF
    set(value) {
      field = value
      left_button.text = resources.getText(value.id)
    }

  var rightButtonText = RightButtonText.NEXT_PAGE
    set(value) {
      field = value
      right_button.text = resources.getText(value.id)
    }

  var onLastPage = false

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_measure_sheet)

    val navBarHeight = getNavBarHeight()
    val statusBarHeight = getStatusBarHeight()

    button_panel.setPadding(0, 0, 0, navBarHeight)
    image_container.setPadding(0, statusBarHeight, 0, navBarHeight)

    preview_image.maxWidth = calcWidth()

    val name = intent.getStringExtra(NAME_KEY)
    val uri = intent.getStringExtra(URI_KEY)
    val bpm = intent.getFloatExtra(BPM_KEY, 0f)
    val bpb = intent.getIntExtra(BPB_KEY, 0)
    preview_image.sheet = Sheet(name, uri, bpm, bpb)
    loadPdf(uri)

    left_button.setOnClickListener {
      when (leftButtonText) {
        LeftButtonText.SAVE_STAFF -> {
          preview_image.saveSelection()
          leftButtonText = LeftButtonText.SAVE_BAR
          rightButtonText = RightButtonText.NEXT_STAFF
          right_button.isEnabled = false
        }
        LeftButtonText.SAVE_BAR -> {
          preview_image.saveSelection()
          right_button.isEnabled = true
        }
      }
    }
    right_button.setOnClickListener {
      when (rightButtonText) {
        RightButtonText.NEXT_STAFF -> {
          preview_image.nextStaff()
          leftButtonText = LeftButtonText.SAVE_STAFF
          rightButtonText = if (onLastPage) RightButtonText.FINISH else RightButtonText.NEXT_PAGE
        }
        RightButtonText.FINISH -> {
          val realm = Realm.getDefaultInstance()
          realm.beginTransaction()
          realm.copyToRealm(preview_image.sheet)
          realm.commitTransaction()
          realm.close()
        }
        RightButtonText.NEXT_PAGE -> {
          onLastPage = preview_image.nextPage()
          if (onLastPage) {
            rightButtonText = RightButtonText.FINISH
          }
        }
      }
    }
  }

  private fun getNavBarHeight(): Int {
    val resourceId = resources.getIdentifier("navigation_bar_height", "dimen", "android")
    return if (resourceId > 0) {
      resources.getDimensionPixelSize(resourceId)
    } else {
      0
    }
  }

  private fun getStatusBarHeight(): Int {
    val resourceId = resources.getIdentifier("status_bar_height", "dimen", "android")
    return if (resourceId > 0) {
      resources.getDimensionPixelSize(resourceId)
    } else {
      0
    }
  }

  private fun loadPdf(uri: String) {
    val pdfDescriptor = contentResolver.openFileDescriptor(Uri.parse(uri), "r")
    val renderer = PdfRenderer(pdfDescriptor)
    preview_image.renderer = renderer
    onLastPage = renderer.pageCount == 1
    if (onLastPage) {
      rightButtonText = RightButtonText.FINISH
    }
  }

  private fun calcWidth(): Int {
    val size = Point()
    windowManager.defaultDisplay.getSize(size)
    return size.x
  }
}

enum class LeftButtonText(val id: Int) {
  SAVE_BAR(R.string.save_bar),
  SAVE_STAFF(R.string.save_staff)
}

enum class RightButtonText(val id: Int) {
  NEXT_STAFF(R.string.next_staff),
  NEXT_PAGE(R.string.next_page),
  FINISH(R.string.finish)
}
