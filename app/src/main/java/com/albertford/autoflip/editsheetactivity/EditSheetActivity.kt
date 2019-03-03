package com.albertford.autoflip.editsheetactivity

import android.content.Context
import android.net.Uri
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.provider.OpenableColumns
import android.support.constraint.ConstraintLayout
import android.support.design.widget.BottomSheetBehavior
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

class EditSheetActivity : AppCompatActivity(), CoroutineScope {

    private lateinit var job: Job
    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Main + job

    private lateinit var bottomSheetBehavior: BottomSheetBehavior<ConstraintLayout>

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
                val uri = Uri.parse(uriString)
                launch {
                    val sheetAndPages = initSheet(uri, uriString)
                    if (sheetAndPages != null) {
                        val (sheet, pages) = sheetAndPages
                        supportActionBar?.title = sheet.name
                        val adapter = PageAdapter(
                                sheet, pages, true, uri, context, context)
                        page_recycler.adapter = adapter
                    } else {
                        finish()
                    }
                }
            }
            existingSheet != null -> launch {
                var pages: Array<Page> = arrayOf()
                withContext(Dispatchers.Default) {
                    pages = database?.sheetDao()?.findFullPagesBySheet(existingSheet.id) ?: arrayOf()
                }
                supportActionBar?.title = existingSheet.name
                val uri = Uri.parse(existingSheet.uri)
                val adapter = PageAdapter(existingSheet, pages, false, uri, context, context)
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
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem?) = when (item?.itemId) {
        R.id.action_save -> {
            saveSheet()
            true
        }
        R.id.action_rename -> {
            true
        }
        else -> {
            super.onOptionsItemSelected(item)
        }
    }

    // enable/disable menu actions
//    override fun onPrepareOptionsMenu(menu: Menu?): Boolean {
//        super.onPrepareOptionsMenu(menu)
//        menu?.findItem(R.id.action_undo)?.isEnabled = false
//        menu?.findItem(R.id.action_redo)?.isEnabled = false
//        return true
//    }

//    override fun initalSelection(pageIndex: Int) {
//        bottomSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
//    }
//
//    override fun confirmSelection() {
//        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
//    }
//
//    override fun changeSelection() {
//        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
//    }
//
//    override fun cancelSelection() {
//        bottomSheetBehavior.state = BottomSheetBehavior.STATE_HIDDEN
//    }

    private fun getAdapter(): PageAdapter? {
        val adapter = page_recycler.adapter
        return if (adapter is PageAdapter) {
            adapter
        } else {
            null
        }
    }
}

interface EditSheetListener {
    fun setEditEnabled(enabled: Boolean)
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
