package com.albertford.autoflip.editsheetactivity

import android.annotation.SuppressLint
import android.content.Context
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
import com.albertford.autoflip.room.Staff
import kotlinx.android.synthetic.main.activity_edit_sheet.*
import kotlinx.android.synthetic.main.edit_bottom_sheet.*
import kotlinx.coroutines.*
import kotlin.coroutines.CoroutineContext

class EditSheetActivity : AppCompatActivity(), CoroutineScope, EditPageObserver {

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

        val context = this
        val uriString = intent.getStringExtra("URI")
        val existingSheet = intent.getParcelableExtra<Sheet>("SHEET")
        // TODO: Possible race condition where app would crash if database access happens faster than the ui is inflated?
        when {
            uriString != null -> {
                toggleEditButton?.setIcon(R.drawable.done)
                toggleEditButton?.setTitle(R.string.done)
                val uri = Uri.parse(uriString)
                launch {
                    val sheetAndPages = initSheet(uri, uriString)
                    if (sheetAndPages != null) {
                        val (sheet, pages) = sheetAndPages
                        supportActionBar?.title = sheet.name
                        val adapter = PageAdapter(
                                sheet, pages, true, uri, context, context, context, observers)
                        page_recycler.adapter = adapter
                    } else {
                        finish()
                    }
                }
            }
            existingSheet != null -> launch {
                val pages = withContext(Dispatchers.Default) {
                    database?.sheetDao()?.findFullPagesBySheet(existingSheet.id) ?: arrayOf()
                }
                supportActionBar?.title = existingSheet.name
                val uri = Uri.parse(existingSheet.uri)
                val adapter = PageAdapter(existingSheet, pages, false, uri, context, context, context, observers)
                page_recycler.adapter = adapter
            }
            else -> finish()
        }
    }

    private suspend fun initSheet(uri: Uri, uriString: String): Pair<Sheet, Array<Page>>? {
        var nameVar: String? = null
        var sizesVar: Array<Size>? = null
        withContext(Dispatchers.Default) {
            nameVar = getFileName(uri,
                    this@EditSheetActivity)
            sizesVar = calcSizes(uri,
                    this@EditSheetActivity)
        }
        val name = nameVar ?: resources.getString(R.string.untitled)
        val sizes = sizesVar ?: return null
        val sheet = Sheet(name, uriString, sizes.size)
        val sheetId = withContext(Dispatchers.Default) {
            database?.sheetDao()?.insertSheet(sheet)
        } ?: return null
        sheet.id = sheetId
        val pages = Array(sheet.pageCount) { i ->
            Page(sizes[i].width, sizes[i].height, sheetId, i)
        }
        withContext(Dispatchers.Default) {
            database?.sheetDao()?.insertPages(pages)
        }
        return Pair(sheet, pages)
    }

    private fun saveSheet() {
        val adapter = getAdapter() ?: return
        val sheet = adapter.sheet
        val pages = adapter.pages
        launch {
            withContext(Dispatchers.Default) {
                var firstStaff: Staff? = null
                for (page in pages) {
                    if (page.staves.size > 0) {
                        firstStaff = page.staves.first()
                        break
                    }
                }
                sheet.firstStaffTop = firstStaff?.top
                sheet.firstStaffBottom = firstStaff?.bottom
                sheet.firstStaffPageIndex = firstStaff?.pageIndex
                database?.sheetDao()?.upateSheetAndPages(sheet, pages)
            }
        }
        Toast.makeText(this, R.string.action_save, Toast.LENGTH_SHORT).show()
    }

    override fun onDestroy() {
        super.onDestroy()
        job.cancel()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.edit_sheet_menu, menu)
        toggleEditButton = menu?.findItem(R.id.action_toggle_edit)
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
}

interface EditSheetObserver {
    fun onEditEnabledChanged(editEnabled: Boolean)
}

private fun getFileName(uri: Uri, context: Context): String? {
    if (uri.scheme == "file") {
        return trimExtension(uri.lastPathSegment)
    }
    var name: String? = null
    context.contentResolver.query(
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

/** Remove the .pdf at the end of a string if it exists */
private fun trimExtension(fileName: String?): String? {
    fileName ?: return null
    return if (fileName.endsWith(".pdf")) {
        fileName.dropLast(4)
    } else {
        fileName
    }
}
