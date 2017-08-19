package com.albertford.autoflip.activities

import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.util.Log
import com.albertford.autoflip.PdfSheetRenderer
import com.albertford.autoflip.R
import com.albertford.autoflip.SheetRenderer
import com.albertford.autoflip.models.Sheet
import io.realm.Realm
import kotlinx.android.synthetic.main.activity_view_sheet.*

class ViewSheetActivity : AppCompatActivity() {
    private lateinit var realm: Realm

    private lateinit var sheetRenderer: SheetRenderer

    private lateinit var sheet: Sheet

    private var pageIndex = 0
    private var staffIndex = 0
    private var barIndex = 0

    private var renderHandler = Handler()
    private var renderRunnable = object : Runnable {
        override fun run() {
            if (pageIndex == sheet.pages.size) {
                return
            }
            renderBar()
            incrementIndeces()
            val delay = Math.round(60000.0 * sheet.bpb / sheet.bpm)
            renderHandler.postDelayed(this, delay)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        realm = Realm.getDefaultInstance()
        setContentView(R.layout.activity_view_sheet)

        val uri = intent.getStringExtra(URI_KEY)
        sheetRenderer = PdfSheetRenderer(this, uri)
        sheet = loadSheet(uri)

        jumpToNonEmptyPage()

        sheet_image.post { renderBar() }

        sheet_image.setOnClickListener { renderHandler.post(renderRunnable) }
    }

    override fun onDestroy() {
        sheetRenderer.close()
        super.onDestroy()
        realm.close()
    }

    private fun renderBar() {
        val bitmap = sheetRenderer.renderBar(sheet, pageIndex, staffIndex, barIndex,
                sheet_image.width, sheet_image.height)
        sheet_image.setImageBitmap(bitmap)
    }

    private fun incrementIndeces() {
        val page = sheet.pages[pageIndex]
        val staff = page.staves[staffIndex]
        if (barIndex < staff.barLines.size - 2) {
            barIndex++
        } else if (staffIndex < page.staves.size - 1) {
            staffIndex++
            barIndex = 0
        } else {
            pageIndex++
            staffIndex = 0
            barIndex = 0
            jumpToNonEmptyPage()
        }
    }

    /**
     * Increment the page index until the page is not empty.
     */
    private fun jumpToNonEmptyPage() {
        while (pageIndex < sheet.pages.size && sheet.pages[pageIndex].staves.isEmpty()) {
            pageIndex++
        }
    }

    private fun loadSheet(uri: String): Sheet {
        realm.beginTransaction()
        val sheet = realm.where(Sheet::class.java)
                .equalTo("uri", uri)
                .findFirst()
        realm.commitTransaction()
        Log.v("PAGES", "${sheet.pages}")
        return sheet
    }
}
