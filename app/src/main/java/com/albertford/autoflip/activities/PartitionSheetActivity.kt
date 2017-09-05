package com.albertford.autoflip.activities

import android.content.Intent
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.support.design.widget.BottomSheetBehavior
import android.view.Menu
import android.view.MenuItem
import android.view.View
import com.albertford.autoflip.R
import kotlinx.android.synthetic.main.activity_partition_sheet.*

class PartitionSheetActivity : AppCompatActivity() {

    private var changeTitleButton: MenuItem? = null

    private var uri: String? = null
    private var sheetIsPdf = true

    private var title: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_partition_sheet)

        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        val bottomSheetBehavior = BottomSheetBehavior.from(bottom_sheet)
        bottomSheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED

        readUri()

        start_finish_button.setOnClickListener(startButtonListener)
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_partition, menu)
        changeTitleButton = menu?.findItem(R.id.action_title)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        if (item?.itemId == R.id.action_title) {
            toggleTitle()
        }
        return super.onOptionsItemSelected(item)
    }

    private val startButtonListener = View.OnClickListener {
        val bottomSheetBehavior = BottomSheetBehavior.from(bottom_sheet)
        bottomSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
        start_finish_button.text = resources.getString(R.string.finish)
    }

    private fun readUri() {
        val pdfUriString = intent.getStringExtra("PDF")
        val imageUriString = intent.getStringExtra("IMAGE")
        if (pdfUriString != null) {
            uri = pdfUriString
            sheetIsPdf = true
        } else if (imageUriString != null) {
            uri = imageUriString
            sheetIsPdf = false
        } else {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
        }
    }

    private fun toggleTitle() {
        if (title == null) {
            title = title_field.text.toString()
            if (title == "") {
                title = resources.getString(R.string.untitled)
            }
            toolbar.title = title
            title_field.visibility = View.GONE
            changeTitleButton?.setIcon(R.drawable.ic_mode_edit_white_24dp)
            changeTitleButton?.title = resources.getString(R.string.action_edit)
        } else {
            title_field.setText(title)
            title_field.visibility = View.VISIBLE
            title = null
            changeTitleButton?.setIcon(R.drawable.ic_done_white_24dp)
            changeTitleButton?.title = resources.getString(R.string.action_save)
        }
    }
}
