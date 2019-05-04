package com.albertford.autoflip.mainactivity

import android.app.Activity
import android.content.Intent
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.provider.OpenableColumns
import android.support.v7.widget.helper.ItemTouchHelper
import android.util.Log
import android.widget.Toast
import com.albertford.autoflip.*
import com.albertford.autoflip.editsheetactivity.EditSheetActivity
import com.albertford.autoflip.room.Page
import com.albertford.autoflip.room.Sheet
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.coroutines.*
import kotlin.coroutines.CoroutineContext

/**
 * Main activity that lists all the sheets in the database and has a button to create new ones.
 */

const val PDF_REQUEST = 1

class MainActivity : AppCompatActivity(), CoroutineScope {

    // TODO: Why is this lateinit?
    private lateinit var job: Job
    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Main + job

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(R.style.AppTheme)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        job = Job()

        setSupportActionBar(toolbar)

        // we initialize an adapter with no data to show the user the placeholder right away
        val adapter = SheetAdapter(this, this)
        recycler_view.adapter = adapter
        val callback = ItemTouchHelperCallback(adapter)
        val touchHelper = ItemTouchHelper(callback)
        touchHelper.attachToRecyclerView(recycler_view)

        floating_action_button.setOnClickListener {
            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT)
            intent.type = "application/pdf"
            if (intent.resolveActivity(packageManager) != null) {
                startActivityForResult(intent, PDF_REQUEST)
            }
        }

        floating_action_button.setOnLongClickListener {
            val toast = Toast.makeText(this, R.string.new_sheet_label, Toast.LENGTH_SHORT)
            toast.show()
            false
        }
    }

    /** Dispose of asynchronous tasks */
    override fun onDestroy() {
        super.onDestroy()
        job.cancel()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode != Activity.RESULT_OK) {
            return
        }
        val uri = data?.data ?: return
        contentResolver.takePersistableUriPermission(uri,
                data.flags and Intent.FLAG_GRANT_READ_URI_PERMISSION)
        val intent = Intent(this, EditSheetActivity::class.java)
        launch {
            val sheet = createSheet(uri)
//            Log.d("SHEET", "${sheet?.name}")
            intent.putExtra(EditSheetActivity.INTENT_KEY, sheet)
            startActivity(intent)
        }
    }

    override fun onResume() {
        super.onResume()
        launch {
            var sheets: Array<Sheet> = arrayOf()
            withContext(Dispatchers.Default) {
                sheets = database?.sheetDao()?.findAllSheets() ?: arrayOf()
            }
            val adapter = recycler_view.adapter
            if (adapter is SheetAdapter) {
                adapter.sheets.clear()
                adapter.sheets.addAll(sheets)
                adapter.notifyDataSetChanged()
            }
        }
    }

    // move initializing sheets from editsheetactivity to here to separate concerns
    private suspend fun createSheet(uri: Uri): Sheet? {
        return withContext(Dispatchers.Default) {
            val name = getFileName(uri) ?: resources.getString(R.string.untitled)
            calcSizes(uri)?.let { pageSizes ->
                val sheet = Sheet(name, uri.toString(), pageSizes.size)
                sheet.id = database?.sheetDao()?.insertSheet(sheet) ?: sheet.id
                val pages = Array(pageSizes.size) { i ->
                    Page(pageSizes[i].width, pageSizes[i].height, sheet.id, i)
                }
                database?.sheetDao()?.insertPages(pages)
                sheet
            }
        }
//
//        val name = withContext(Dispatchers.Default) {
//            getFileName(uri)
//        } ?: resources.getString(R.string.untitled)
//        val pageSizes = withContext(Dispatchers.Default) {
//            calcSizes(uri)
//        } ?: return null
//        val sheet = Sheet(name, uri.toString(), pageSizes.size)
//        sheet.id = withContext(Dispatchers.Default) {
//            database?.sheetDao()?.insertSheet(sheet)
//        } ?: return null
//        val pages = Array(pageSizes.size) { i ->
//            Page(pageSizes[i].width, pageSizes[i].height, sheet.id, i)
//        }
//        withContext(Dispatchers.Default) {
//            database?.sheetDao()?.insertPages(pages)
//        }
//        return sheet
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

    /** Remove the .pdf at the end of a string if it exists */
    private fun trimExtension(fileName: String?): String? {
        fileName ?: return null
        return if (fileName.endsWith(".pdf")) {
            fileName.dropLast(4)
        } else {
            fileName
        }
    }
}
