//package com.albertford.autoflip.deprecated
//
//import android.content.Context
//import android.net.Uri
//import android.support.v7.app.AppCompatActivity
//import android.os.Bundle
//import android.os.Parcel
//import android.os.Parcelable
//import android.provider.OpenableColumns
//import android.view.Menu
//import android.view.MenuItem
//import android.view.View
//import android.view.inputmethod.EditorInfo
//import android.widget.TextView
//import com.albertford.autoflip.R
//import com.albertford.autoflip.database
//import com.albertford.autoflip.room.*
//import io.reactivex.Completable
//import io.reactivex.Single
//import io.reactivex.android.schedulers.AndroidSchedulers
//import io.reactivex.disposables.CompositeDisposable
//import io.reactivex.schedulers.Schedulers
//import kotlinx.android.synthetic.main.activity_partition_sheet.*
//
//const val DEFAULT_BEATS_PER_MEASURE = 4
//const val DEFAULT_BEATS_PER_MINUTE = 100f
//
//class PartitionSheetActivity : AppCompatActivity(), PartitionControlled {
//
//    private val compositeDisposable = CompositeDisposable()
//
//    private lateinit var state: State
//
//    private var changeTitleButton: MenuItem? = null
//
//    private var sheetRenderer: SheetRenderer? = null
//
//    private var startScrollY = 0
//    private var endScrollY = 0
//
//    private val onTitleDone = TextView.OnEditorActionListener { _, actionId: Int, _ ->
//        if (actionId == EditorInfo.IME_ACTION_DONE) {
//            if (state.isTitleEditable) {
//                hideTitleField()
//            }
//        }
//        false
//    }
//
//    override fun onCreate(savedInstanceState: Bundle?) {
//        super.onCreate(savedInstanceState)
//        setContentView(R.layout.activity_partition_sheet)
//
//        setSupportActionBar(toolbar)
//        supportActionBar?.setDisplayHomeAsUpEnabled(true)
//        title_field.setOnEditorActionListener(onTitleDone)
//
//        bottom_sheet.partitionControlled = this
//        bottom_sheet.setOnTouchListener { _ , _ -> true }
//        sheet_image.onSelectBarListener = onSelectBarListener
//
//        val savedState = savedInstanceState?.getParcelable<State>("STATE")
//        val savedPage = savedInstanceState?.getParcelable<Page>("PAGE")
//        if (savedState != null && savedPage != null) {
//            state = savedState
//            sheet_image.page = savedPage
//        } else {
//            val uri = intent.getStringExtra("URI")
//            if (uri == null) {
//                finish()
//                return
//            }
//            val fileName = getFileName(Uri.parse(uri), this)
//            if (fileName == null) {
//                finish()
//                return
//            }
//            state = State(uri, fileName)
//            insertSheet()
//        }
//        toolbar.title = state.sheet.name
//        title_field.setText(state.sheet.name)
//
//        loadRenderer()
//    }
//
//    /** Dispose of asynchronous tasks */
//    override fun onDestroy() {
//        super.onDestroy()
//        compositeDisposable.dispose()
//    }
//
//    override fun onSaveInstanceState(outState: Bundle?) {
//        super.onSaveInstanceState(outState)
//        outState?.putParcelable("STATE", state)
//        outState?.putParcelable("PAGE", sheet_image.page)
//    }
//
//    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
//        menuInflater.inflate(R.menu.menu_partition, menu)
//        changeTitleButton = menu?.findItem(R.id.action_title)
//        title_field.setText(state.sheet.name)
//        toggleTitle()
//        return true
//    }
//
//    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
//        if (item?.itemId == R.id.action_title) {
//            toggleTitle()
//        }
//        return super.onOptionsItemSelected(item)
//    }
//
//    override fun startPages(beatsPerMinute: Float, beatsPerMeasure: Int): ButtonVisibilities {
//        title_field.onEditorAction(EditorInfo.IME_ACTION_DONE)
//        currentFocus.clearFocus()
//        sheet_image.page = Page(0, sheet_image.width.toFloat())
//        val renderer = sheetRenderer
//        return when (renderer) {
//            is PdfSheetRenderer -> if (renderer.getPageCount() == 1) {
//                ButtonVisibilities(next = View.GONE,
//                        finish = View.VISIBLE)
//            } else {
//                ButtonVisibilities(next = View.VISIBLE,
//                        finish = View.GONE)
//            }
//            else -> ButtonVisibilities(next = View.VISIBLE,
//                    finish = View.VISIBLE)
//        }
//    }
//
//    override fun nextPage(): ButtonVisibilities? {
//        val renderer = sheetRenderer
//        renderer ?: return null
//        val pageWidth = renderer.getPageWidth(state.pageIndex)
//        val oldPage = sheet_image.page
//        oldPage ?: return null
//        val bars = oldPage.toBarArray(state.sheet.id, pageWidth)
//        if (bars.isNotEmpty()) {
//            Completable.fromAction {
//                writeBars(bars)
//            }.subscribeOn(Schedulers.io()).subscribe()
//        }
//        state.pageIndex++
//        val lastBar = bars.lastOrNull()
//        sheet_image.setImageBitmap(renderer.renderFullPage(state.pageIndex, sheet_image.width))
//        sheet_image.page = Page(oldPage, lastBar?.beatsPerMinute,
//                lastBar?.beatsPerMeasure)
//        return when (renderer) {
//            is PdfSheetRenderer -> if (state.pageIndex + 1 == renderer.getPageCount()) {
//                ButtonVisibilities(next = View.GONE,
//                        finish = View.VISIBLE)
//            } else {
//                ButtonVisibilities(next = View.VISIBLE,
//                        finish = View.GONE)
//            }
//            else -> ButtonVisibilities(next = View.VISIBLE,
//                    finish = View.VISIBLE)
//        }
//    }
//
//    override fun finishPages() {
//        val renderer = sheetRenderer
//        renderer ?: return
//        val pageWidth = renderer.getPageWidth(state.pageIndex)
//        val bars = sheet_image.page?.toBarArray(state.sheet.id, pageWidth) ?: return
//        if (bars.isNotEmpty()) {
//            Completable.fromAction {
//                writeBars(bars)
//            }.doOnComplete {
//                finish()
//            }.subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread()).subscribe()
//        } else {
//            finish()
//        }
//    }
//
//    override fun cancelBar() {
//        sheet_image.page?.selectedBarIndex = -1
//    }
//
//    override fun applyBar(beatsPerMinute: Float?, beatsPerMeasure: Int?, beginRepeat: Boolean?, endRepeat: Boolean?) {
//        val page = sheet_image.page ?: return
//        val barLines = page.staves.lastOrNull()?.barLines ?: return
//        val firstBarLine = barLines[page.selectedBarIndex]
//        val secondBarLine = barLines[page.selectedBarIndex + 1]
//        if (beatsPerMeasure != null) {
//            firstBarLine.bpb = beatsPerMeasure
//        }
//        if (beatsPerMinute != null) {
//            firstBarLine.bpm = beatsPerMinute
//        }
//        if (beginRepeat != null) {
//            firstBarLine.beginRepeat = beginRepeat
//        }
//        if (endRepeat != null) {
//            secondBarLine.endRepeat = endRepeat
//        }
//        page.selectedBarIndex = -1
//    }
//
//    override fun setSlideOffset(slideOffset: Float) {
//        sheet_image.slideOffset = slideOffset
//        scroll_view.partialScroll(startScrollY, endScrollY, slideOffset)
//    }
//
//    override fun beginCollapse() {
//        endScrollY = scroll_view.scrollY
//    }
//
//    override fun endCollapse() {
//        image_frame.setPadding(0, 0, 0, 0)
//    }
//
//    override fun beginExpand() {
//        val mask = bottom_sheet.height
//        image_frame.setPadding(0, 0, 0, mask)
//        val page = sheet_image.page ?: return
//        val position = page.selectedStaffPosition().toInt()
//        val target = (scroll_view.height - mask) / 2
//        startScrollY = scroll_view.scrollY
//        endScrollY = position - target
//    }
//
//    private val onSelectBarListener = { beginRepeat: Boolean, endRepeat: Boolean ->
//        bottom_sheet.expand(beginRepeat, endRepeat)
//    }
//
//    private fun writeBars(bars: Array<Bar>) {
//        database?.barDao()?.insertBars(*bars)
//        database?.sheetDao()?.updateSheet(state.sheet)
//    }
//
//    private fun loadRenderer() {
//        val disposable = Single.fromCallable {
//            PdfSheetRenderer(this, state.sheet.uri)
//        }
//                .subscribeOn(Schedulers.io())
//                .observeOn(AndroidSchedulers.mainThread())
//                .subscribe { renderer ->
//                    sheetRenderer = renderer
//                    sheet_image.post {
//                        renderCurrentPage()
//                    }
//                    bottom_sheet.pageCount = renderer.getPageCount()
//                }
//        compositeDisposable.add(disposable)
//    }
//
//    private fun toggleTitle() {
//        if (state.isTitleEditable) {
//            hideTitleField()
//        } else {
//            showTitleField()
//        }
//        state.isTitleEditable = !state.isTitleEditable
//    }
//
//    private fun showTitleField() {
//        title_field.visibility = View.VISIBLE
//        changeTitleButton?.setIcon(R.drawable.done)
//        changeTitleButton?.title = resources.getString(R.string.action_save)
//    }
//
//    private fun hideTitleField() {
//        state.sheet.name = title_field.text.toString()
//        if (state.sheet.name == "") {
//            state.sheet.name = resources.getString(R.string.untitled)
//        }
//        toolbar.title = state.sheet.name
//        title_field.visibility = View.GONE
//        changeTitleButton?.setIcon(R.drawable.edit)
//        changeTitleButton?.title = resources.getString(R.string.action_edit)
//    }
//
//    private fun renderCurrentPage() {
//        val bitmap = sheetRenderer?.renderFullPage(state.pageIndex, sheet_image.width)
//        sheet_image.setImageBitmap(bitmap)
//    }
//
//    private fun insertSheet() {
//        val disposable = Single.fromCallable {
//            database?.sheetDao()?.insertSheet(state.sheet)
//        }.subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread()).subscribe { sheetId ->
//            state.sheet.id = sheetId ?: 0
//        }
//        compositeDisposable.add(disposable)
//    }
//
//    private class State : Parcelable {
//
//        val sheet: Sheet
//        var pageIndex: Int
//        var isTitleEditable: Boolean
//        var lastBeatsPerMinute: Float
//        var lastBeatsPerMeasure: Int
//
//        constructor(uri: String, fileName: String) {
//            sheet = Sheet(fileName, uri, 0)
//            pageIndex = 0
//            isTitleEditable = true
//            lastBeatsPerMeasure = DEFAULT_BEATS_PER_MEASURE
//            lastBeatsPerMinute = DEFAULT_BEATS_PER_MINUTE
//        }
//
//        private constructor(parcel: Parcel) {
//            sheet = Sheet(parcel.readString(), parcel.readString()!!, parcel.readInt())
//            pageIndex = parcel.readInt()
//            isTitleEditable = parcel.readInt() != 0
//            lastBeatsPerMinute = parcel.readFloat()
//            lastBeatsPerMeasure = parcel.readInt()
//        }
//
//        override fun writeToParcel(parcel: Parcel?, int: Int) {
//            parcel?.run {
//                writeLong(sheet.id)
//                writeString(sheet.name)
//                writeString(sheet.uri)
//                writeInt(pageIndex)
//                writeInt(if (isTitleEditable) 1 else 0)
//                writeFloat(lastBeatsPerMinute)
//                writeInt(lastBeatsPerMeasure)
//            }
//        }
//
//        override fun describeContents(): Int = 0
//
//        companion object CREATOR : Parcelable.Creator<State> {
//            override fun newArray(size: Int): Array<State?> = arrayOfNulls(size)
//
//            override fun createFromParcel(parcel: Parcel): State = State(
//                    parcel)
//        }
//
//    }
//}
//
//private fun getFileName(uri: Uri, context: Context): String? {
//    if (uri.scheme == "file") {
//        return trimExtension(uri.lastPathSegment)
//    }
//    var name: String? = null
//    context.contentResolver.query(
//            uri,
//            arrayOf(OpenableColumns.DISPLAY_NAME),
//            null,
//            null,
//            null
//    )?.use { cursor ->
//        if (cursor.moveToFirst()) {
//            name = cursor.getString(0)
//        }
//    }
//    return trimExtension(name)
//}
//
///** Remove the .pdf at the end of a string if it exists */
//private fun trimExtension(fileName: String?): String? {
//    fileName ?: return null
//    return if (fileName.endsWith(".pdf")) {
//        fileName.dropLast(4)
//    } else {
//        fileName
//    }
//}
