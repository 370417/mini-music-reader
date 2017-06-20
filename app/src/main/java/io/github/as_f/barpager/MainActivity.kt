package io.github.as_f.barpager

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Point
import android.graphics.pdf.PdfRenderer
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {

  val PICK_PDF_REQUEST = 1

  lateinit var pdfRenderer: PdfRenderer

  var buttonState = ButtonState.STAFF_PAGE

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_main)

    left_button.setOnClickListener {
      when (buttonState) {
        ButtonState.STAFF_PAGE, ButtonState.STAFF_DONE -> preview_image.saveStaff()
        ButtonState.BAR_STAFF -> preview_image.saveBar()
      }
    }
    right_button.setOnClickListener {
      when (buttonState) {
        ButtonState.BAR_STAFF -> preview_image.nextStaff()
        ButtonState.STAFF_DONE -> {}
        ButtonState.STAFF_PAGE -> {}
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
        val pdfDescriptor = contentResolver.openFileDescriptor(data.data, "r")
        val renderer = PdfRenderer(pdfDescriptor)

        val size = Point()
        windowManager.defaultDisplay.getSize(size)
        val viewWidth = size.x

        val bitmaps = Array(renderer.pageCount) { i ->
          val page = renderer.openPage(i)
          val fullWidth = pointsToPixels(page.width)
          val fullHeight = pointsToPixels(page.height)
          val fitToWidthScale = viewWidth.toFloat() / fullWidth
          val scaledWidth = Math.round(fullWidth * fitToWidthScale)
          val scaledHeight = Math.round(fullHeight * fitToWidthScale)
          val bitmap = Bitmap.createBitmap(scaledWidth, scaledHeight, Bitmap.Config.ARGB_8888)
          page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
          page.close()
          bitmap
        }

        preview_image.setImageBitmap(bitmaps[0])
      }
    }
  }

  private fun pointsToPixels(pixels: Int): Int {
    return resources.displayMetrics.densityDpi * pixels / 72
  }
}

enum class ButtonState {
  STAFF_PAGE, BAR_STAFF, STAFF_DONE
}
