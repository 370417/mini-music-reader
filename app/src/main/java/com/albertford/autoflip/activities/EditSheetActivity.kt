package com.albertford.autoflip.activities

import android.net.Uri
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import com.albertford.autoflip.PageAdapter
import com.albertford.autoflip.R
import com.albertford.autoflip.calcSizes
import kotlinx.android.synthetic.main.activity_edit_sheet.*
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

        val context = this
        launch(Dispatchers.Main) {
            val uriString = intent.getStringExtra("URI")
            if (uriString != null) {
                val uri = Uri.parse(uriString)
                val sizes = withContext(Dispatchers.Default) {
                    calcSizes(uri, context)
                }
                if (sizes != null) {
                    val adapter = PageAdapter(sizes, uri, context, context)
                    page_recycler.adapter = adapter
                }
            } else {
                finish()
            }
        }
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
