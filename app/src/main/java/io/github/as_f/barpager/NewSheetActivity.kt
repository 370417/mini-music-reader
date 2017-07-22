package io.github.as_f.barpager

import android.app.Activity
import android.content.Intent
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.inputmethod.EditorInfo
import kotlinx.android.synthetic.main.activity_new_sheet.*

const val PICK_PDF_REQUEST = 1

const val NAME_KEY = "NAME_KEY"
const val URI_KEY = "URI_KEY"
const val BPM_KEY = "BPM_KEY"
const val BPB_KEY = "BPB_KEY"

const val STATE_URI = "STATE_URI"

class NewSheetActivity : AppCompatActivity() {

  var uri: String? = null

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_new_sheet)

    setSupportActionBar(toolbar)
    supportActionBar?.setDisplayHomeAsUpEnabled(true)

    if (savedInstanceState == null) {
      pickPdf()
    } else {
      uri = savedInstanceState.getString(STATE_URI)
    }

    name_input_field.setOnFocusChangeListener { _, hasFocus -> if (!hasFocus) validateName() }
    bpm_input_field.setOnFocusChangeListener { _, hasFocus -> if (!hasFocus) validateBpm() }
    bpb_input_field.setOnFocusChangeListener { _, hasFocus -> if (!hasFocus) validateBpb() }

    name_input_field.setOnKeyListener { _, _, _ ->
      if (name_input_layout.error != null) {
        validateName()
      }
      false
    }
    bpm_input_field.setOnKeyListener { _, _, _ ->
      if (bpm_input_layout.error != null) {
        validateBpm()
      }
      false
    }
    bpb_input_field.setOnKeyListener { _, _, _ ->
      if (bpb_input_layout.error != null) {
        validateBpb()
      }
      false
    }

    bpb_input_field.setOnEditorActionListener { _, actionId, _ ->
      if (actionId == EditorInfo.IME_ACTION_DONE) {
        startMeasureSheetActivity()
      }
      false
    }
  }

  override fun onSaveInstanceState(outState: Bundle?) {
    outState?.putString(STATE_URI, uri)

    super.onSaveInstanceState(outState)
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
    // make sure all error messages are shown if there are multiple
    val nameIsValid = validateName()
    val bpmIsValid = validateBpm()
    val bpbIsValid = validateBpb()
    if (nameIsValid && bpmIsValid && bpbIsValid) {
      val intent = Intent(this, MeasureSheetActivity::class.java)
      intent.putExtra(NAME_KEY, name_input_field.text.toString())
      intent.putExtra(URI_KEY, uri)
      intent.putExtra(BPM_KEY, bpm_input_field.text.toString().toFloat())
      intent.putExtra(BPB_KEY, bpb_input_field.text.toString().toInt())
      startActivity(intent)
    }
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
    }
  }

  fun validateName(): Boolean {
    val message = if (name_input_field.text.isBlank()) {
      resources.getString(R.string.error_required_field)
    } else {
      null
    }
    name_input_layout.error = message
    return message == null
  }

  fun validateBpm(): Boolean {
    val floatVal = bpm_input_field.text.toString().toFloatOrNull()
    val message = if (bpm_input_field.text.isBlank()) {
      resources.getString(R.string.error_required_field)
    } else if (floatVal == 0f) {
      resources.getString(R.string.error_non_zero)
    } else if (floatVal == null) {
      resources.getString(R.string.error_not_float)
    } else {
      null
    }
    bpm_input_layout.error = message
    return message == null
  }

  fun validateBpb(): Boolean {
    val intVal = bpb_input_field.text.toString().toIntOrNull()
    val message = if (bpb_input_field.text.isBlank()) {
      resources.getString(R.string.error_required_field)
    } else if (intVal == 0) {
      resources.getString(R.string.error_non_zero)
    } else if (intVal == null) {
      resources.getString(R.string.error_not_int)
    } else {
      null
    }
    bpb_input_layout.error = message
    return message == null
  }
}
