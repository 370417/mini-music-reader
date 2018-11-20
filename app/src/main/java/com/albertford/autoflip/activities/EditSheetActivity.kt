package com.albertford.autoflip.activities

import android.content.Context
import android.net.Uri
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.provider.OpenableColumns
import android.support.design.widget.BottomSheetBehavior
import android.view.Menu
import android.view.MenuItem
import com.albertford.autoflip.*
import com.albertford.autoflip.room.Page
import com.albertford.autoflip.room.Sheet
import kotlinx.android.synthetic.main.activity_edit_sheet.*
import kotlinx.android.synthetic.main.edit_bottom_sheet.*
import kotlinx.coroutines.*
import kotlin.coroutines.CoroutineContext

class EditSheetActivity : AppCompatActivity(), CoroutineScope {
    lateinit var job: Job
    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Main + job

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        job = Job()
        setContentView(R.layout.activity_edit_sheet)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        page_recycler.adapter = PlaceholderPageAdapter()
        val behavior = BottomSheetBehavior.from(bottom_sheet)
        behavior.state = BottomSheetBehavior.STATE_HIDDEN

        val context = this
        val uriString = intent.getStringExtra("URI")
        if (uriString != null) {
            val uri = Uri.parse(uriString)
            launch {
                val sheetAndPages = initSheet(uri, uriString)
                if (sheetAndPages != null) {
                    val (sheet, pages) = sheetAndPages
                    supportActionBar?.title = sheet.name
                    val adapter = PageAdapter(sheet, pages, uri, context, context)
                    page_recycler.adapter = adapter
                } else {
                    finish()
                }
            }
        } else {
            finish()
        }
    }

    private suspend fun initSheet(uri: Uri, uriString: String): Pair<Sheet, Array<Page>>? {
        var nameVar: String? = null
        var sizesVar: Array<Size>? = null
        withContext(Dispatchers.Default) {
            nameVar = getFileName(uri, this@EditSheetActivity)
            sizesVar = calcSizes(uri, this@EditSheetActivity)
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

    override fun onDestroy() {
        super.onDestroy()
        job.cancel()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.edit_sheet_menu, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem?) = when (item?.itemId) {
        R.id.action_rename -> {
            true
        }
        R.id.action_undo -> {
            true
        }
        R.id.action_redo -> {
            true
        }
        else -> {
            super.onOptionsItemSelected(item)
        }
    }

    // enable/disable menu actions
    override fun onPrepareOptionsMenu(menu: Menu?): Boolean {
        super.onPrepareOptionsMenu(menu)
        menu?.findItem(R.id.action_undo)?.isEnabled = false
        menu?.findItem(R.id.action_redo)?.isEnabled = false
        return true
    }
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
