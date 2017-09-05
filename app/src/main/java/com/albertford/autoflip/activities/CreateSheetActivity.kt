package com.albertford.autoflip.activities

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.support.annotation.StringRes
import android.support.v7.app.AlertDialog
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.inputmethod.EditorInfo
import com.albertford.autoflip.PdfSheetRenderer
import com.albertford.autoflip.R
import com.albertford.autoflip.SheetRenderer
import kotlinx.android.synthetic.main.activity_create_sheet.*

const val NAME_KEY = "NAME_KEY"
const val URI_KEY = "URI_KEY"
const val BPM_KEY = "BPM_KEY"
const val BPB_KEY = "BPB_KEY"

private const val STATE_URI = "STATE_URI"

class CreateSheetActivity : AppCompatActivity() {

//    private lateinit var realm: Realm

    private var uri: String? = null

    private val chooseFile = View.OnClickListener {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT)
        intent.type = "application/pdf"
        intent.putExtra(Intent.EXTRA_LOCAL_ONLY, true)
        if (intent.resolveActivity(packageManager) != null) {
            startActivityForResult(intent, PICK_PDF_REQUEST)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
//        realm = Realm.getDefaultInstance()
        setContentView(R.layout.activity_create_sheet)

        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        setFocusListeners()

        if (savedInstanceState != null) {
            uri = savedInstanceState.getString(STATE_URI)
            sheet_image.post { renderPreview(Uri.parse(uri)) }
        }

        choose_sheet_button.setOnClickListener(chooseFile)

        bpb_input_field.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                startMeasureSheetActivity()
            }
            false
        }
    }

    override fun onSaveInstanceState(outState: Bundle?) {
        super.onSaveInstanceState(outState)
        outState?.putString(STATE_URI, uri)
    }

//    override fun onDestroy() {
//        super.onDestroy()
//        realm.close()
//    }

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

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == PICK_PDF_REQUEST && resultCode == Activity.RESULT_OK && data?.data != null) {
            uri = data.data.toString()
            if (validateUri()) {
                renderPreview(data.data)
            } else {
                uri = null
            }
        }
    }

    private fun renderPreview(uri: Uri) {
        val sheetRenderer: SheetRenderer = PdfSheetRenderer(this, uri)
        val bitmap = sheetRenderer.renderPagePreview(0, sheet_image.width, sheet_image.height)
        sheetRenderer.close()
        sheet_image.setImageBitmap(bitmap)
    }

    private fun setFocusListeners() {
        name_input_field.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                name_input_layout.error = null
            } else {
                validateName()
            }
        }
        bpm_input_field.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                bpm_input_layout.error = null
            } else {
                validateBpm()
            }
        }
        bpb_input_field.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                bpb_input_layout.error = null
            } else {
                validateBpb()
            }
        }
    }

    private fun startMeasureSheetActivity() {
        // make sure all error messages are shown if there are multiple
        val nameIsValid = validateName()
        val bpmIsValid = validateBpm()
        val bpbIsValid = validateBpb()
        val uriIsValid = validateUri()
        if (nameIsValid && bpmIsValid && bpbIsValid && uriIsValid) {
            val intent = Intent(this, MeasureSheetActivity::class.java)
            intent.putExtra(NAME_KEY, name_input_field.text.toString())
            intent.putExtra(URI_KEY, uri)
            intent.putExtra(BPM_KEY, bpm_input_field.text.toString().toFloat())
            intent.putExtra(BPB_KEY, bpb_input_field.text.toString().toInt())
            startActivity(intent)
        }
    }

    /**
     * @return whether the uri is valid
     */
    private fun validateUri(): Boolean {
        val uri = uri
        return if (uri == null) {
            showAlert(R.string.error_no_uri)
            false
        } else if (!isUriUnique(uri)) {
            showAlert(R.string.error_non_unique_uri)
            false
        } else {
            true
        }
    }

    private fun showAlert(@StringRes msg: Int) {
        AlertDialog.Builder(this)
                .setMessage(msg)
                .create()
                .show()
    }

    private fun validateName(): Boolean {
        val message = if (name_input_field.text.isBlank()) {
            resources.getString(R.string.error_required_field)
        } else {
            null
        }
        name_input_layout.error = message
        return message == null
    }

    private fun validateBpm(): Boolean {
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

    private fun validateBpb(): Boolean {
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

    private fun isUriUnique(uri: String): Boolean {
//        realm.beginTransaction()
//        val sameUri = realm.where(SheetPartition::class.java)
//                .equalTo("uri", uri)
//                .findFirst()
//        realm.commitTransaction()
//        return sameUri == null
        return true
    }
}
