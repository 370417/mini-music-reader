package com.albertford.autoflip.activities

import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.os.Parcel
import android.os.Parcelable
import android.support.design.widget.BottomSheetBehavior
import android.view.Menu
import android.view.MenuItem
import android.view.View
import com.albertford.autoflip.*
import com.albertford.autoflip.models.*
import com.albertford.autoflip.room.*
import io.reactivex.Completable
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.activity_partition_sheet.*

class PartitionSheetActivity : AppCompatActivity() {

    private lateinit var state: State

    private var bottomSheetBehavior: BottomSheetBehavior<View>? = null

    private var changeTitleButton: MenuItem? = null

    private var sheetRenderer: SheetRenderer? = null

    private var sheetSubscription: Disposable? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_partition_sheet)

        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        bottomSheetBehavior = BottomSheetBehavior.from(bottom_sheet)
        bottomSheetBehavior?.setBottomSheetCallback(bottomSheetCallback)

        bottom_sheet.setOnTouchListener { _ , _ -> true }
        sheet_image.onSelectBarListener = onSelectBarListener
        next_page_button.setOnClickListener(nextButtonListener)
        start_button.setOnClickListener(startButtonListener)
        finish_button.setOnClickListener(finishButtonListener)
        bottom_cancel_button.setOnClickListener(cancelButtonListener)

        if (savedInstanceState == null) {
            state = State(this)
            insertSheet()
        } else {
            state = savedInstanceState.getParcelable("STATE")
            sheet_image.page = savedInstanceState.getParcelable("PAGE")
        }
        restoreUIState()
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

    private val bottomSheetCallback = object : BottomSheetBehavior.BottomSheetCallback() {
        /** Darken the sheet image when the bottom sheet is expanded */
        override fun onSlide(bottomSheet: View, slideOffset: Float) {
            sheet_image.slideOffset = slideOffset
        }

        override fun onStateChanged(bottomSheet: View, newState: Int) {
            if (newState == BottomSheetBehavior.STATE_COLLAPSED) {
                sheet_image.page?.selectedBarIndex = -1
                if (bottom_apply_button.visibility == View.GONE) {
                    begin_repeat_checkbox.visibility = View.VISIBLE
                    end_repeat_checkbox.visibility = View.VISIBLE
                    bottom_apply_button.visibility = View.VISIBLE
                    bottom_cancel_button.visibility = View.VISIBLE
                }
            }
        }
    }

    private val onSelectBarListener = { beginRepeat: Boolean, endRepeat: Boolean ->
        beats_measure_field.text = null
        beats_minute_field.text = null
        begin_repeat_checkbox.isChecked = beginRepeat
        end_repeat_checkbox.isChecked = endRepeat
        setBottomSheetState(BottomSheetBehavior.STATE_EXPANDED)
    }

    private val nextButtonListener = View.OnClickListener {
        val renderer = sheetRenderer
        renderer ?: return@OnClickListener
        val pageWidth = renderer.getPageWidth(state.pageIndex)
        val oldPage = sheet_image.page
        oldPage ?: return@OnClickListener
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
        if (renderer is PdfSheetRenderer && state.pageIndex + 1 == renderer.getPageCount()) {
            setNextButtonVisibility(false)
            setFinishButtonVisibility(true)
        }
    }

    private val startButtonListener = View.OnClickListener {
        val bpbValid = validateBpb()
        val bpmValid = validateBpm()
        if (!bpbValid || !bpmValid) {
            return@OnClickListener
        }
        setStartButtonVisibility(false)
        val renderer = sheetRenderer
        if (renderer is PdfSheetRenderer) {
            if (renderer.getPageCount() == 1) {
                setFinishButtonVisibility(true)
            } else {
                setNextButtonVisibility(true)
            }
        } else {
            setNextButtonVisibility(true)
            setFinishButtonVisibility(true)
        }
        setBottomSheetState(BottomSheetBehavior.STATE_COLLAPSED)
        sheet_image.allowTouch = true
        sheet_image.page = Page(0, sheet_image.width.toFloat())
    }

    private val finishButtonListener = View.OnClickListener {
        val renderer = sheetRenderer
        renderer ?: return@OnClickListener
        finish_button.isEnabled = false
        val pageWidth = renderer.getPageWidth(state.pageIndex)
        val bars = sheet_image.page?.toBarArray(state.sheet.id, pageWidth)
        bars ?: return@OnClickListener
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

    private val cancelButtonListener = View.OnClickListener {
        setBottomSheetState(BottomSheetBehavior.STATE_COLLAPSED)
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
                            initPageCount(renderer)
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

    /**
     * @return true if bpm field is valid (not empty)
     */
    private fun validateBpm(): Boolean {
        return if (beats_minute_field.text.isEmpty()) {
            beats_minute_layout.error = resources.getString(R.string.error_required_field)
            false
        } else {
            beats_minute_layout.error = null
            true
        }
    }

    /**
     * @return true if bpb field is valid (not empty)
     */
    private fun validateBpb(): Boolean {
        return if (beats_measure_field.text.isEmpty()) {
            beats_measure_layout.error = resources.getString(R.string.error_required_field)
            false
        } else {
            beats_measure_layout.error = null
            true
        }
    }

    private fun setBottomSheetState(state: Int) {
        bottomSheetBehavior?.state = state
        this.state.bottomSheetState = state
        if (state == BottomSheetBehavior.STATE_COLLAPSED) {
            next_page_button.isEnabled = true
            finish_button.isEnabled = true
        } else {
            next_page_button.isEnabled = false
            finish_button.isEnabled = false
        }
    }

    private fun setStartButtonVisibility(visible: Boolean) {
        val visibility = if (visible) View.VISIBLE else View.GONE
        start_button.visibility = visibility
        state.startButtonVisibility = visibility
    }

    private fun setNextButtonVisibility(visible: Boolean) {
        val visibility = if (visible) View.VISIBLE else View.GONE
        next_page_button.visibility = visibility
        state.nextButtonVisibility = visibility
    }

    private fun setFinishButtonVisibility(visible: Boolean) {
        val visibility = if (visible) View.VISIBLE else View.GONE
        finish_button.visibility = visibility
        state.finishButtonVisibility = visibility
    }
    private fun initPageCount(renderer: PdfSheetRenderer) {
        of_text.visibility = View.VISIBLE
        page_count_text.text = renderer.getPageCount().toString()
        page_count_text.visibility = View.VISIBLE
    }

    private fun restoreUIState() {
        setBottomSheetState(state.bottomSheetState)
        start_button.visibility = state.startButtonVisibility
        next_page_button.visibility = state.nextButtonVisibility
        finish_button.visibility = state.finishButtonVisibility
        if (state.startButtonVisibility == View.GONE) {
            sheet_image.allowTouch = true
            begin_repeat_checkbox.visibility = View.VISIBLE
            end_repeat_checkbox.visibility = View.VISIBLE
            bottom_apply_button.visibility = View.VISIBLE
            bottom_cancel_button.visibility = View.VISIBLE
        }
        page_number_text.text = (state.pageIndex + 1).toString()
    }

    private class State : Parcelable {

        val sheet: Sheet
        var pageIndex: Int
        var isTitleEditable: Boolean
        var uri: String
        var bottomSheetState: Int
        var startButtonVisibility: Int
        var nextButtonVisibility: Int
        var finishButtonVisibility: Int

        constructor(activity: PartitionSheetActivity) {
            sheet = Sheet(0, "Untitled", activity.intent.getIntExtra("TYPE", IMG_SHEET))
            pageIndex = 0
            isTitleEditable = true
            bottomSheetState = BottomSheetBehavior.STATE_EXPANDED
            startButtonVisibility = View.VISIBLE
            nextButtonVisibility = View.GONE
            finishButtonVisibility = View.GONE
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
            bottomSheetState = parcel.readInt()
            startButtonVisibility = parcel.readInt()
            nextButtonVisibility = parcel.readInt()
            finishButtonVisibility = parcel.readInt()
            uri = parcel.readString()
        }

        override fun writeToParcel(parcel: Parcel?, int: Int) {
            parcel?.writeLong(sheet.id)
            parcel?.writeInt(pageIndex)
            parcel?.writeString(sheet.name)
            parcel?.writeInt(sheet.type)
            parcel?.writeInt(if (isTitleEditable) 1 else 0)
            parcel?.writeString(uri)
            parcel?.writeInt(bottomSheetState)
            parcel?.writeInt(startButtonVisibility)
            parcel?.writeInt(nextButtonVisibility)
            parcel?.writeInt(finishButtonVisibility)
            parcel?.writeString(uri)
        }

        override fun describeContents(): Int = 0

        companion object CREATOR : Parcelable.Creator<State> {
            override fun newArray(size: Int): Array<State?> = arrayOfNulls(size)

            override fun createFromParcel(parcel: Parcel): State = State(parcel)
        }

    }
}
