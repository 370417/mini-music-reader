package com.albertford.autoflip.activities

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Matrix
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.support.v7.app.AlertDialog
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import com.albertford.autoflip.R
import com.albertford.autoflip.models.Sheet
import io.realm.Realm
import kotlinx.android.synthetic.main.activity_create_sheet.*

const val PICK_PDF_REQUEST = 1

const val NAME_KEY = "NAME_KEY"
const val URI_KEY = "URI_KEY"
const val BPM_KEY = "BPM_KEY"
const val BPB_KEY = "BPB_KEY"

const val STATE_URI = "STATE_URI"

class CreateSheetActivity : AppCompatActivity() {

    lateinit var realm: Realm

    var uri: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        realm = Realm.getDefaultInstance()
        setContentView(R.layout.activity_create_sheet)

        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        setFocusListeners()

        if (savedInstanceState != null) {
            uri = savedInstanceState.getString(STATE_URI)
            sheet_image.post { renderPreview(Uri.parse(uri)) }
        }

        choose_sheet_button.setOnClickListener { chooseFile() }
    }

    override fun onSaveInstanceState(outState: Bundle?) {
        super.onSaveInstanceState(outState)
        outState?.putString(STATE_URI, uri)
    }

    override fun onDestroy() {
        super.onDestroy()
        realm.close()
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

    fun chooseFile() {
        val intent = Intent(Intent.ACTION_GET_CONTENT)
        intent.type = "application/pdf"
        intent.putExtra(Intent.EXTRA_LOCAL_ONLY, true)
        if (intent.resolveActivity(packageManager) != null) {
            startActivityForResult(intent, PICK_PDF_REQUEST)
        }
    }

    fun renderPreview(uri: Uri) {
        val width = sheet_image.width
        val height = sheet_image.height
        val pdfDescriptor = contentResolver.openFileDescriptor(uri, "r")
        val renderer = PdfRenderer(pdfDescriptor)
        val pageRenderer = renderer.openPage(0)
        val scale = width.toFloat() / pageRenderer.width
        val matrix = Matrix()
        matrix.postScale(scale, scale)
        Log.v("sdf", "$width, $height")
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        pageRenderer.render(bitmap, null, matrix, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
        pageRenderer.close()
        sheet_image.setImageBitmap(bitmap)
    }

    fun setFocusListeners() {
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

    fun startMeasureSheetActivity() {
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

    fun validateUri(): Boolean {
        val uri = uri
        return if (uri == null) {
            AlertDialog.Builder(this)
                    .setMessage(R.string.error_no_uri)
                    .create()
                    .show()
            false
        } else if (!isUriUnique(uri)) {
            AlertDialog.Builder(this)
                    .setMessage(R.string.error_non_unique_uri)
                    .create()
                    .show()
            false
        } else {
            true
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

    fun isUriUnique(uri: String): Boolean {
        realm.beginTransaction()
        val sameUri = realm.where(Sheet::class.java)
                .equalTo("uri", uri)
                .findFirst()
        realm.commitTransaction()
        return sameUri == null
    }
}
