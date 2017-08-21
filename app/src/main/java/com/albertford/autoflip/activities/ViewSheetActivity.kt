package com.albertford.autoflip.activities

import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.util.Log
import com.albertford.autoflip.PdfSheetRenderer
import com.albertford.autoflip.R
import com.albertford.autoflip.SheetRenderer
import com.albertford.autoflip.models.Bar
import com.albertford.autoflip.models.Sheet
import io.realm.Realm
import kotlinx.android.synthetic.main.activity_view_sheet.*

class ViewSheetActivity : AppCompatActivity() {
    private lateinit var realm: Realm

    private lateinit var sheetRenderer: SheetRenderer

    private lateinit var sheet: Sheet

    private lateinit var barList: List<Bar>
    private var barIndex = 0

    private var renderHandler = Handler()
    private var renderRunnable = object : Runnable {
        override fun run() {
            if (barIndex == barList.size) {
                return
            }
            renderBar()
            barIndex++
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
        barList = sheetRenderer.createBarList(sheet)

        sheet_image.post { renderBar() }

        sheet_image.setOnClickListener { renderHandler.post(renderRunnable) }
    }

    override fun onDestroy() {
        sheetRenderer.close()
        super.onDestroy()
        realm.close()
    }

    private fun renderBar() {
        val bitmap = sheetRenderer.renderBar(barList, barIndex, sheet_image.width,
                sheet_image.height)
        sheet_image.setImageBitmap(bitmap)
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
