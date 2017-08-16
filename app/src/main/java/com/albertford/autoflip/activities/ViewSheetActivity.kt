package com.albertford.autoflip.activities

import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import com.albertford.autoflip.R
import io.realm.Realm

class ViewSheetActivity : AppCompatActivity() {
    private lateinit var realm: Realm

    private lateinit var rendererPages: Array<PdfRenderer.Page>

    private var pageIndex = 0
    private var staffIndex = 0
    private var barIndex = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        realm = Realm.getDefaultInstance()
        setContentView(R.layout.activity_view_sheet)

        val uri = intent.getStringExtra(URI_KEY)
        loadPdfRenderers(uri)
    }

    override fun onDestroy() {
        for (page in rendererPages) {
            page.close()
        }
        super.onDestroy()
        realm.close()
    }

    private fun loadPdfRenderers(uri: String) {
        val pdfDescriptor = contentResolver.openFileDescriptor(Uri.parse(uri), "r")
        val renderer = PdfRenderer(pdfDescriptor)
        rendererPages = Array(renderer.pageCount, { i -> renderer.openPage(i) })
    }
}
