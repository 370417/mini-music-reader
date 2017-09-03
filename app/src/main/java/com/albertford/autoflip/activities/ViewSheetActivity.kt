package com.albertford.autoflip.activities

import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
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

    private var scale = 1f

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

    private var countDownDunnable = object : Runnable {
        override fun run() {

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

        secondary_image.post {
            scale = sheetRenderer.findMaxTwoBarScale(barList, play_button.width, play_button.height)
            renderBar()
        }

        play_button.setOnClickListener { renderHandler.post(renderRunnable) }
    }

    override fun onDestroy() {
        sheetRenderer.close()
        super.onDestroy()
        realm.close()
    }

    private fun renderBar() {
        val primaryBitmap = sheetRenderer.renderBar(barList, barIndex, scale)
        primary_image.setImageBitmap(primaryBitmap)
        val secondaryBitmap = if (barIndex < barList.size - 1) {
            sheetRenderer.renderBar(barList, barIndex + 1, scale)
        } else {
            null
        }
        secondary_image.setImageBitmap(secondaryBitmap)
    }

    private fun loadSheet(uri: String): Sheet {
        realm.beginTransaction()
        val sheet = realm.where(Sheet::class.java)
                .equalTo("uri", uri)
                .findFirst()
        realm.commitTransaction()
        return sheet
    }
}
