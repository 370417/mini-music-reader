package com.albertford.autoflip.editsheetactivity

import android.annotation.SuppressLint
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.provider.OpenableColumns
import android.support.constraint.ConstraintLayout
import android.support.design.widget.BottomSheetBehavior
import android.support.design.widget.TextInputEditText
import android.support.design.widget.TextInputLayout
import android.support.v7.app.AlertDialog
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import com.albertford.autoflip.*
import com.albertford.autoflip.editsheetactivity.pagerecycler.*
import com.albertford.autoflip.room.Page
import com.albertford.autoflip.room.Sheet
import kotlinx.android.synthetic.main.activity_edit_sheet.*
import kotlinx.android.synthetic.main.edit_bottom_sheet.*
import kotlinx.coroutines.*
import kotlin.coroutines.CoroutineContext

class EditSheetActivity : AppCompatActivity(), CoroutineScope, EditPageObserver {

    companion object {
        /**
         * EditSheetActivity expects either a uri or a sheet to be passed in as an intent extra.
         * If a uri is passed in, it creates a new sheet. If a sheet is passed in, it loads an
         * existing sheet.
         */
        const val URI_KEY = "URI_KEY"
        const val SHEET_KEY = "SHEET_KEY"
    }

    private lateinit var job: Job
    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Main + job

    private val observers: MutableSet<EditSheetObserver> = mutableSetOf()

    private lateinit var bottomSheetBehavior: BottomSheetBehavior<ConstraintLayout>

    private var toggleEditButton: MenuItem? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        job = Job()
        setContentView(R.layout.activity_edit_sheet)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        bottomSheetBehavior = BottomSheetBehavior.from(bottom_sheet)

        page_recycler.adapter = PlaceholderPageAdapter()
        val behavior = BottomSheetBehavior.from(bottom_sheet)
        behavior.state = BottomSheetBehavior.STATE_HIDDEN

        val uriString = intent.getStringExtra(URI_KEY)
        val existingSheet = intent.getParcelableExtra<Sheet>(SHEET_KEY)
        // TODO: Possible race condition where app would crash if database access happens faster than the ui is inflated?
        when {
            uriString != null -> launch {
                val sheetAndPages = initSheet(Uri.parse(uriString))
                if (sheetAndPages != null) {
                    val (sheet, pages) = sheetAndPages
                    finishOnCreate(sheet, pages, true)
                } else {
                    finish()
                }
            }
            existingSheet != null -> launch {
                val pages = withContext(Dispatchers.Default) {
                    database?.sheetDao()?.findFullPagesBySheet(existingSheet.id)
                }
                if (pages != null) {
                    finishOnCreate(existingSheet, pages, false)
                } else {
                    finish()
                }
            }
            else -> finish()
        }
    }

    private fun finishOnCreate(sheet: Sheet, pages: Array<Page>, editable: Boolean) {
        supportActionBar?.title = sheet.name
        page_recycler.adapter = PageAdapter(sheet, pages, false, this, this, this, observers)
        if (editable) {
            toggleEditEnabled()
        }
    }

    private suspend fun initSheet(uri: Uri): Pair<Sheet, Array<Page>>? {
        return withContext(Dispatchers.Default) {
            val name = getFileName(uri) ?: resources.getString(R.string.untitled)
            calcSizes(uri)?.let { pageSizes ->
                val sheet = Sheet(name, uri.toString(), pageSizes.size)
                sheet.id = database?.sheetDao()?.insertSheet(sheet) ?: sheet.id
                val pages = Array(pageSizes.size) { i ->
                    Page(pageSizes[i].width, pageSizes[i].height, sheet.id, i)
                }
                database?.sheetDao()?.insertPages(pages)
                Pair(sheet, pages)
            }
        }
    }

    private fun saveSheet() {
        val adapter = getAdapter() ?: return
        val sheet = adapter.sheet
        val pages = adapter.pages
        sheet.updateFirstStaff(pages)
        launch(Dispatchers.Default) {
            database?.sheetDao()?.upateSheetAndPages(sheet, pages)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        job.cancel()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.edit_sheet_menu, menu)
        toggleEditButton = menu?.findItem(R.id.action_toggle_edit)
        if (intent.getStringExtra("URI") != null) {
            toggleEditButton?.setIcon(R.drawable.done)
            toggleEditButton?.setTitle(R.string.done)
        }
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        when (item?.itemId) {
            R.id.action_toggle_edit -> toggleEditEnabled()
            R.id.action_save -> saveSheet()
            R.id.action_rename -> rename()
            else -> {
                return super.onOptionsItemSelected(item)
            }
        }
        return true
    }

    override fun onCancelSelection() {
        // TODO
    }

    override fun onChangeSelection(pageIndex: Int, staffIndex: Int, barIndex: Int) {
        // TODO
    }

    override fun onScrollAttempt() {
        Toast.makeText(this, R.string.scroll_helper, Toast.LENGTH_SHORT).show()
    }

    /** Get the recyclerview's adapter if it is not a placeholderadapter */
    private fun getAdapter(): PageAdapter? {
        val adapter = page_recycler.adapter
        return if (adapter is PageAdapter) {
            adapter
        } else {
            null
        }
    }

    private fun toggleEditEnabled() {
        val adapter = getAdapter() ?: return
        if (adapter.editable) {
            for (observer in observers) {
                observer.onEditEnabledChanged(false)
            }
            saveSheet()
            toggleEditButton?.setIcon(R.drawable.edit)
            toggleEditButton?.setTitle(R.string.edit)
        } else {
            for (observer in observers) {
                observer.onEditEnabledChanged(true)
            }
            toggleEditButton?.setIcon(R.drawable.done)
            toggleEditButton?.setTitle(R.string.done)
        }
    }

    // It is safe to pass null is the parent view here because AlertDialog neither provides nor
    // expects a parent view.
    @SuppressLint("InflateParams")
    private fun rename() {
        val builder  = AlertDialog.Builder(this)
        builder.setTitle(R.string.rename)
        val view = LayoutInflater.from(this).inflate(R.layout.dialog_edit_text, null)
        // AlertDialog.Builder has a method to set the view by a resource id directly
        // We inflate the view manually instead so that we have a reference to it that we can use in
        // the onclick listener
        builder.setView(view)
        builder.setPositiveButton(android.R.string.ok, null)
        builder.setNegativeButton(android.R.string.cancel) { dialog, _ ->
            dialog.cancel()
        }
        val alertDialog = builder.show()
        alertDialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
            val editText: TextInputEditText? = view.findViewById(R.id.edit_text)
            val textInputLayout: TextInputLayout? = view.findViewById(R.id.text_input_layout)
            if (editText == null || textInputLayout == null) {
                alertDialog.cancel()
            } else if (editText.text.isNullOrBlank()) {
                textInputLayout.error = "Name cannot be blank"
            } else {
                alertDialog.dismiss()
                rename(editText.text.toString())
            }
        }
    }

    private fun rename(name: String) {
        supportActionBar?.title = name
        val adapter = getAdapter() ?: return
        val sheet = adapter.sheet
        launch(Dispatchers.Default) {
            sheet.name = name
            database?.sheetDao()?.updateSheet(sheet)
        }
    }

    private fun getFileName(uri: Uri): String? {
        if (uri.scheme == "file") {
            return trimExtension(uri.lastPathSegment)
        }
        var name: String? = null
        contentResolver.query(
                uri,
                arrayOf(OpenableColumns.DISPLAY_NAME),
                null,
                null,
                null
        )?.use { cursor ->
            if (cursor.moveToFirst()) {
                name = cursor.getString(0)
            }
        }
        return trimExtension(name)
    }

    private class PageSize(val width: Int, val height: Int)

    /**
     * Calculate the size of each page.
     * We do this in advance so that we can show properly sized placeholder rectangles while the images
     * load.
     */
    private fun calcSizes(uri: Uri): Array<PageSize>? {
        return contentResolver.openFileDescriptor(uri, "r")?.use { descriptor ->
            val renderer = PdfRenderer(descriptor)
            Array(renderer.pageCount) { i ->
                renderer.openPage(i).use { page ->
                    PageSize(page.width, page.height)
                }
            }
        }
    }
}

interface EditSheetObserver {
    fun onEditEnabledChanged(editEnabled: Boolean)
}

/** Remove the .pdf at the end of a string if it exists */
private fun trimExtension(fileName: String?): String? {
    fileName ?: return null
    return if (fileName.endsWith(".pdf")) {
        fileName.dropLast(4)
    } else {
        fileName
    }
}
