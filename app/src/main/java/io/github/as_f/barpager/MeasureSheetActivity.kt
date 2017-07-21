package io.github.as_f.barpager

import android.app.Activity
import android.content.Intent
import android.graphics.Point
import android.graphics.pdf.PdfRenderer
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import io.realm.Realm
import kotlinx.android.synthetic.main.activity_measure_sheet.*

class MeasureSheetActivity : AppCompatActivity() {

  val PICK_PDF_REQUEST = 1

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

    preview_image.maxWidth = calcWidth()

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

    pickPdf()
  }

  fun pickPdf() {
    val pickPdfIntent = Intent(Intent.ACTION_GET_CONTENT)
    pickPdfIntent.type = "application/pdf"
    pickPdfIntent.putExtra(Intent.EXTRA_LOCAL_ONLY, true)
    if (pickPdfIntent.resolveActivity(packageManager) != null) {
      startActivityForResult(pickPdfIntent, PICK_PDF_REQUEST)
    }
  }

  override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
    super.onActivityResult(requestCode, resultCode, data)

    if (requestCode == PICK_PDF_REQUEST && resultCode == Activity.RESULT_OK) {
      if (data?.data != null) {
        preview_image.sheet.uri = data.data.toString()
        val pdfDescriptor = contentResolver.openFileDescriptor(data.data, "r")
        val renderer = PdfRenderer(pdfDescriptor)
        preview_image.renderer = renderer
        onLastPage = renderer.pageCount == 1
        if (onLastPage) {
          rightButtonText = RightButtonText.FINISH
        }
      }
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
