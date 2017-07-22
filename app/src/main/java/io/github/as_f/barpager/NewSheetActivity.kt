package io.github.as_f.barpager

import android.app.Activity
import android.content.Intent
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.view.KeyEvent
import android.view.Menu
import android.view.MenuItem
import android.view.inputmethod.EditorInfo
import kotlinx.android.synthetic.main.activity_new_sheet.*

const val PICK_PDF_REQUEST = 1

const val NAME_KEY = "NAME_KEY"
const val URI_KEY = "URI_KEY"
const val BPM_KEY = "BPM_KEY"
const val BPB_KEY = "BPB_KEY"

class NewSheetActivity : AppCompatActivity() {

  lateinit var uri: String

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_new_sheet)

    setSupportActionBar(toolbar)
    supportActionBar?.setDisplayHomeAsUpEnabled(true)

    pickPdf()

    bpb_input_field.setOnEditorActionListener { _, actionId, _ ->
      if (actionId == EditorInfo.IME_ACTION_DONE) {
        startMeasureSheetActivity()
        true
      } else {
        false
      }
    }
  }

  override fun onCreateOptionsMenu(menu: Menu?): Boolean {
    menuInflater.inflate(R.menu.menu_form, menu)
    return super.onCreateOptionsMenu(menu)
  }

  override fun onOptionsItemSelected(item: MenuItem?): Boolean {
    if (item?.itemId == R.id.action_done) {
      startMeasureSheetActivity()
    }

    return super.onOptionsItemSelected(item)
  }

  fun startMeasureSheetActivity() {
    val intent = Intent(this, MeasureSheetActivity::class.java)
    intent.putExtra(NAME_KEY, name_input_field.text.toString())
    intent.putExtra(URI_KEY, uri)
    intent.putExtra(BPM_KEY, bpm_input_field.text.toString().toFloat())
    intent.putExtra(BPB_KEY, bpb_input_field.text.toString().toInt())
    startActivity(intent)
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

    if (requestCode == PICK_PDF_REQUEST && resultCode == Activity.RESULT_OK && data?.data != null) {
      uri = data.data.toString()
      name_input_field.hint = data.data.lastPathSegment
    }
  }
}
