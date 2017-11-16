package com.albertford.autoflip.activities

import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.os.Parcel
import android.os.Parcelable
import android.view.Menu
import android.view.MenuItem
import android.view.View
import com.albertford.autoflip.*
import com.albertford.autoflip.models.*
import com.albertford.autoflip.room.*
import com.albertford.autoflip.views.ButtonVisibilities
import com.albertford.autoflip.views.PartitionControlled
import io.reactivex.Completable
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.activity_partition_sheet.*

class PartitionSheetActivity : AppCompatActivity(), PartitionControlled {

    private lateinit var state: State

    private var changeTitleButton: MenuItem? = null

    private var sheetRenderer: SheetRenderer? = null

    private var sheetSubscription: Disposable? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_partition_sheet)

        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        bottom_sheet.partitionControlled = this
        bottom_sheet.setOnTouchListener { _ , _ -> true }
        sheet_image.onSelectBarListener = onSelectBarListener

        if (savedInstanceState == null) {
            state = State(this)
            insertSheet()
            sheet_image.allowTouch = false
        } else {
            state = savedInstanceState.getParcelable("STATE")
            sheet_image.page = savedInstanceState.getParcelable("PAGE")
            sheet_image.allowTouch = sheet_image.page != null
        }
        loadRenderer()
    }

    override fun onDestroy() {
        super.onDestroy()
        val sheetSub = sheetSubscription
        if (sheetSub?.isDisposed == false) {
            sheetSub.dispose()
        }
        val clickSub = sheet_image.longClickSubscription
        if (clickSub?.isDisposed == false) {
            clickSub.dispose()
        }
    }

    override fun onSaveInstanceState(outState: Bundle?) {
        super.onSaveInstanceState(outState)
        outState?.putParcelable("STATE", state)
        outState?.putParcelable("PAGE", sheet_image.page)
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_partition, menu)
        changeTitleButton = menu?.findItem(R.id.action_title)
        toggleTitle()
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        if (item?.itemId == R.id.action_title) {
            state.isTitleEditable = !state.isTitleEditable
            toggleTitle()
        }
        return super.onOptionsItemSelected(item)
    }

    override fun startPages(): ButtonVisibilities {
        bottom_sheet.collapse()
        sheet_image.allowTouch = true
        sheet_image.page = Page(0, sheet_image.width.toFloat())
        val renderer = sheetRenderer
        return when (renderer) {
            is PdfSheetRenderer -> if (renderer.getPageCount() == 1) {
                ButtonVisibilities(next = View.GONE, finish = View.VISIBLE)
            } else {
                ButtonVisibilities(next = View.VISIBLE, finish = View.GONE)
            }
            else -> ButtonVisibilities(next = View.VISIBLE, finish = View.VISIBLE)
        }
    }

    override fun nextPage(): ButtonVisibilities? {
        val renderer = sheetRenderer
        renderer ?: return null
        val pageWidth = renderer.getPageWidth(state.pageIndex)
        val oldPage = sheet_image.page
        oldPage ?: return null
        val bars = oldPage.toBarArray(state.sheet.id, pageWidth)
        if (bars.isNotEmpty()) {
            Completable.fromAction {
                writeBars(bars)
            }.subscribeOn(Schedulers.io()).subscribe()
        }
        state.pageIndex++
        val lastBar = bars.lastOrNull()
        sheet_image.setImageBitmap(renderer.renderFullPage(state.pageIndex, sheet_image.width))
        sheet_image.page = Page(oldPage, lastBar?.beatsPerMinute, lastBar?.beatsPerMeasure)
        return when (renderer) {
            is PdfSheetRenderer -> if (state.pageIndex + 1 == renderer.getPageCount()) {
                ButtonVisibilities(next = View.GONE, finish = View.VISIBLE)
            } else {
                ButtonVisibilities(next = View.VISIBLE, finish = View.GONE)
            }
            else -> ButtonVisibilities(next = View.VISIBLE, finish = View.VISIBLE)
        }
    }

    override fun finishPages() {
        val renderer = sheetRenderer
        renderer ?: return
        val pageWidth = renderer.getPageWidth(state.pageIndex)
        val bars = sheet_image.page?.toBarArray(state.sheet.id, pageWidth)
        bars ?: return
        if (bars.isNotEmpty()) {
            Completable.fromAction {
                writeBars(bars)
            }.doOnComplete {
                finish()
            }.subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread()).subscribe()
        } else {
            finish()
        }
    }

    override fun cancelBar() {
        sheet_image.allowTouch = true
    }

    override fun applyBar(beatsPerMinute: Float?, beatsPerMeasure: Int?, beginRepeat: Boolean?, endRepeat: Boolean?) {
        sheet_image.allowTouch = true
    }

    override fun setSlideOffset(slideOffset: Float) {
        sheet_image.slideOffset = slideOffset
    }

    private val onSelectBarListener = { beginRepeat: Boolean, endRepeat: Boolean ->
        bottom_sheet.expand(beginRepeat, endRepeat)
    }

    private fun writeBars(bars: Array<Bar>) {
        if (state.sheet.type == IMG_SHEET) {
            val pageUri = PageUri(state.uri, state.sheet.id, state.pageIndex)
            database?.uriDao()?.insertUris(pageUri)
        }
        database?.barDao()?.insertBars(*bars)
        database?.sheetDao()?.updateSheet(state.sheet)
    }

    private fun loadRenderer() {
        when (state.sheet.type) {
            PDF_SHEET -> {
                Single.fromCallable {
                    PdfSheetRenderer(this, state.uri)
                }.subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread())
                        .subscribe { renderer ->
                            sheetRenderer = renderer
                            sheet_image.post {
                                renderCurrentPage()
                            }
                            bottom_sheet.pageCount = renderer.getPageCount()
                        }
            }
            IMG_SHEET -> {}
            else -> {
                finish()
            }
        }
    }


    private fun toggleTitle() {
        if (state.isTitleEditable) {
            title_field.visibility = View.VISIBLE
            changeTitleButton?.setIcon(R.drawable.ic_done_white_24dp)
            changeTitleButton?.title = resources.getString(R.string.action_save)
        } else {
            state.sheet.name = title_field.text.toString()
            if (state.sheet.name == "") {
                state.sheet.name = resources.getString(R.string.untitled)
            }
            toolbar.title = state.sheet.name
            title_field.visibility = View.GONE
            changeTitleButton?.setIcon(R.drawable.ic_mode_edit_white_24dp)
            changeTitleButton?.title = resources.getString(R.string.action_edit)
        }
    }

    private fun renderCurrentPage() {
        val bitmap = sheetRenderer?.renderFullPage(state.pageIndex, sheet_image.width)
        sheet_image.setImageBitmap(bitmap)
    }

    private fun insertSheet() {
        state.sheet.name = resources.getString(R.string.untitled)
        Single.fromCallable {
            database?.sheetDao()?.insertSheet(state.sheet)
        }.map { sheetId ->
            if (state.sheet.type == PDF_SHEET) {
                val pageUri = PageUri(state.uri, sheetId, -1)
                database?.uriDao()?.insertUris(pageUri)
            }
            sheetId
        }.subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread()).subscribe { sheetId ->
            state.sheet.id = sheetId ?: 0
        }
    }

    private class State : Parcelable {

        val sheet: Sheet
        var pageIndex: Int
        var isTitleEditable: Boolean
        var uri: String

        constructor(activity: PartitionSheetActivity) {
            sheet = Sheet(0, "Untitled", activity.intent.getIntExtra("TYPE", IMG_SHEET))
            pageIndex = 0
            isTitleEditable = true
            val uri = activity.intent.getStringExtra("URI")
            if (uri != null) {
                this.uri = uri
            } else {
                this.uri = ""
                activity.finish()
            }
        }

        private constructor(parcel: Parcel) {
            sheet = Sheet(parcel.readLong(), parcel.readString(), parcel.readInt())
            pageIndex = parcel.readInt()
            isTitleEditable = parcel.readInt() != 0
            uri = parcel.readString()
            uri = parcel.readString()
        }

        override fun writeToParcel(parcel: Parcel?, int: Int) {
            parcel?.writeLong(sheet.id)
            parcel?.writeInt(pageIndex)
            parcel?.writeString(sheet.name)
            parcel?.writeInt(sheet.type)
            parcel?.writeInt(if (isTitleEditable) 1 else 0)
            parcel?.writeString(uri)
            parcel?.writeString(uri)
        }

        override fun describeContents(): Int = 0

        companion object CREATOR : Parcelable.Creator<State> {
            override fun newArray(size: Int): Array<State?> = arrayOfNulls(size)

            override fun createFromParcel(parcel: Parcel): State = State(parcel)
        }

    }
}
