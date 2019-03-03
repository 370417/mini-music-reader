package com.albertford.autoflip.mainactivity

import android.app.Activity
import android.content.Intent
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.support.v7.widget.helper.ItemTouchHelper
import android.widget.Toast
import com.albertford.autoflip.*
import com.albertford.autoflip.editsheetactivity.EditSheetActivity
import com.albertford.autoflip.room.Sheet
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.coroutines.*
import kotlin.coroutines.CoroutineContext

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
        contentResolver.takePersistableUriPermission(uri, data.flags and Intent.FLAG_GRANT_READ_URI_PERMISSION)
        val intent = Intent(this, EditSheetActivity::class.java)
        intent.putExtra("URI", uri.toString())
        startActivity(intent)
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
}
