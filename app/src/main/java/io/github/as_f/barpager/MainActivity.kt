package io.github.as_f.barpager

import android.app.Activity
import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.database.Cursor
import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.os.ParcelFileDescriptor
import android.provider.MediaStore
import android.util.Log
import com.squareup.picasso.Picasso
import kotlinx.android.synthetic.main.activity_main.*
import java.io.File

class MainActivity : AppCompatActivity() {

  val PICK_PHOTO_REQUEST = 1
  val PICK_PDF_REQUEST = 2

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_main)

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

  fun pickPhoto() {
    val pickPhotoIntent = Intent(Intent.ACTION_GET_CONTENT)
    pickPhotoIntent.type = "image/*"
    if (pickPhotoIntent.resolveActivity(packageManager) != null) {
      startActivityForResult(pickPhotoIntent, PICK_PHOTO_REQUEST)
    }
  }

  override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
    super.onActivityResult(requestCode, resultCode, data)

    if (requestCode == PICK_PHOTO_REQUEST && resultCode == Activity.RESULT_OK) {
      if (data?.data != null) {
        Picasso.with(this)
            .load(data.data)
            .into(zoomImage)
      }
    }

    if (requestCode == PICK_PDF_REQUEST && resultCode == Activity.RESULT_OK) {
      if (data?.data != null) {
        Log.v("uri", data.data.toString())
        val pdfDescriptor = contentResolver.openFileDescriptor(data.data, "r")
        val renderer = PdfRenderer(pdfDescriptor)

        val bitmaps = Array(renderer.pageCount) { i ->
          val page = renderer.openPage(i)
          var width = pointsToPixels(page.width)
          var height = pointsToPixels(page.height)
          if (width > 1024) {
            width = 1024
            height *= 1024 / width
          }
          if (height > 1024) {
            height = 1024
            width *= 1024 / height
          }
          val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
          page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
          page.close()
          bitmap
        }

        zoomImage.setImageBitmap(bitmaps[0])
      }
    }
  }

  private fun pointsToPixels(pixels: Int): Int {
    return resources.displayMetrics.densityDpi * pixels / 72
  }

  fun getDataColumn(uri: Uri): String? {

    var cursor: Cursor? = null
    val column = MediaStore.MediaColumns.DATA
    val projection = arrayOf(column)

    try {
      cursor = contentResolver.query(uri, projection, null, null, null)
      if (cursor != null && cursor.moveToFirst()) {
        val column_index = cursor.getColumnIndexOrThrow(column)
        return cursor.getString(column_index)
      }
    } finally {
      if (cursor != null) cursor.close()
    }
    return null
  }
}
