package com.albertford.autoflip.viewsheetactivity

import android.content.pm.ActivityInfo
import android.graphics.Bitmap
import android.net.Uri
import android.support.v7.app.AppCompatActivity
import android.os.*
import com.albertford.autoflip.*
import com.albertford.autoflip.room.*
import kotlinx.android.synthetic.main.activity_view_sheet.*
import kotlinx.coroutines.*
import kotlin.coroutines.CoroutineContext

class ViewSheetActivity : AppCompatActivity(), CoroutineScope, ViewLogicObserver {

    companion object {
        const val SHEET_KEY = "SHEET_KEY"
    }

    private val observers: MutableSet<ViewActivityObserver> = mutableSetOf()

    private lateinit var job: Job
    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Main + job

    private var pdfFile: ParcelFileDescriptor? = null
    private var logic: ViewSheetLogic? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        job = Job()
        setContentView(R.layout.activity_view_sheet)

        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE

        val sheet = intent.getParcelableExtra<Sheet>(SHEET_KEY)
        if (sheet?.firstStaffTop != null) launch {
            val pages = withContext(Dispatchers.Default) {
                database?.sheetDao()?.findFullPagesBySheet(sheet.id)
            }
            if (pages != null) {
                main_image.post {
                    initLogic(sheet, pages)
                }
            } else {
                finish()
            }
        } else {
            finish()
        }
    }

    /** Dispose of asynchronous tasks */
    override fun onDestroy() {
        super.onDestroy()
        pdfFile?.close()
        job.cancel()
    }

    override fun onSaveInstanceState(outState: Bundle?) {
        super.onSaveInstanceState(outState)
    }

    private fun initLogic(sheet: Sheet, pages: Array<Page>) {
        val pdfFile = contentResolver.openFileDescriptor(Uri.parse(sheet.uri), "r")
        if (pdfFile != null) {
            this.pdfFile = pdfFile
            val logic = ViewSheetLogic(pdfFile, pages, this, this)
            this.logic = logic
            observers.add(logic)
        }
    }

    override fun endReached() {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun showNext(bitmap: Bitmap) {
        main_image.setImageBitmap(bitmap)
    }

    override fun getImgWidth(): Int = main_image.width

    override fun getImgHeight(): Int = main_image.height

}

interface ViewActivityObserver {
    fun play()
    fun pause()
    fun restart()
}
