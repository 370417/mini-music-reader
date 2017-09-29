package com.albertford.autoflip.activities

import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.support.design.widget.BottomSheetBehavior
import android.util.Log
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

    private var bottomSheetBehavior: BottomSheetBehavior<View>? = null

    private var changeTitleButton: MenuItem? = null

    private var uri: String? = null
    private var sheetRenderer: SheetRenderer? = null
    private var pageIndex = 0
    private var sheet = Sheet(0, "Untitled")

    private var title: String? = null

    private var sheetSubscription: Disposable? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_partition_sheet)

        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        bottomSheetBehavior = BottomSheetBehavior.from(bottom_sheet)
        bottomSheetBehavior?.state = BottomSheetBehavior.STATE_EXPANDED
        bottomSheetBehavior?.setBottomSheetCallback(bottomSheetCallback)

        insertSheet()

        readUri()
        initPageCount()

        bottom_sheet.setOnTouchListener { _ , _ -> true }
        sheet_image.onSelectBarListener = onSelectBarListener
        next_page_button.setOnClickListener(nextButtonListener)
        start_finish_button.setOnClickListener(startButtonListener)
        bottom_cancel_button.setOnClickListener(cancelButtonListener)
    }

    override fun onDestroy() {
        super.onDestroy()
        val sheetSub = sheetSubscription
        if (sheetSub != null && !sheetSub.isDisposed) {
            sheetSub.dispose()
        }
        if (sheet_image.longClickSubscription?.isDisposed == false) {
            sheet_image.longClickSubscription?.dispose()
        }
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
        next_page_button.isEnabled = false
        start_finish_button.isEnabled = false
        beats_measure_field.text = null
        beats_minute_field.text = null
        begin_repeat_checkbox.isChecked = beginRepeat
        end_repeat_checkbox.isChecked = endRepeat
        bottomSheetBehavior?.state = BottomSheetBehavior.STATE_EXPANDED
    }

    private val nextButtonListener = View.OnClickListener {
        val pageWidth = sheetRenderer?.getPageWidth(pageIndex) ?: -1
        val oldPage = sheet_image.page
        oldPage ?: return@OnClickListener
        val bars = oldPage.toBarArray(sheet.id, pageWidth)
        if (bars.isNotEmpty()) {
            Completable.fromAction {
                database?.barDao()?.insertBars(*bars)
            }.subscribeOn(Schedulers.io()).subscribe()
        }
        pageIndex++
        val lastBar = bars.lastOrNull()
        sheet_image.setImageBitmap(sheetRenderer?.renderFullPage(pageIndex, sheet_image.width))
        sheet_image.page = Page(oldPage, lastBar?.beatsPerMinute, lastBar?.beatsPerMeasure)
        val renderer = sheetRenderer
        if (renderer is PdfSheetRenderer && pageIndex + 1 == renderer.getPageCount()) {
            next_page_button.visibility = View.GONE
            start_finish_button.visibility = View.VISIBLE
        }
    }

    private val startButtonListener = View.OnClickListener {
        val bpbValid = validateBpb()
        val bpmValid = validateBpm()
        if (!bpbValid || !bpmValid) {
            return@OnClickListener
        }
        start_finish_button.text = resources.getString(R.string.finish)
        start_finish_button.setOnClickListener(finishButtonListener)
        val renderer = sheetRenderer
        if (renderer is PdfSheetRenderer) {
            if (renderer.getPageCount() != 1) {
                start_finish_button.visibility = View.GONE
                next_page_button.visibility = View.VISIBLE
            }
        } else {
            next_page_button.visibility = View.VISIBLE
        }
        bottomSheetBehavior?.state = BottomSheetBehavior.STATE_COLLAPSED
        sheet_image.allowTouch = true
        sheet_image.page = Page(0, sheet_image.width.toFloat())
    }

    private val finishButtonListener = View.OnClickListener {
        start_finish_button.isEnabled = false
        val pageWidth = sheetRenderer?.getPageWidth(pageIndex) ?: -1
        val bars = sheet_image.page?.toBarArray(sheet.id, pageWidth)
        bars ?: return@OnClickListener
        if (bars.isNotEmpty()) {
            Completable.fromAction {
                database?.barDao()?.insertBars(*bars)
            }.doOnComplete {
                finish()
            }.subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread()).subscribe()
        } else {
            finish()
        }
    }

    private val cancelButtonListener = View.OnClickListener {
        next_page_button.isEnabled = true
        start_finish_button.isEnabled = true
        bottomSheetBehavior?.state = BottomSheetBehavior.STATE_COLLAPSED
    }

    private fun readUri() {
        val pdfUriString = intent.getStringExtra("PDF")
        val imageUriString = intent.getStringExtra("IMAGE")
        if (pdfUriString != null) {
            uri = pdfUriString
            val pdfSingle = Single.fromCallable {
                PdfSheetRenderer(this, pdfUriString)
            }
            sheetSubscription = pdfSingle
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe({ renderer ->
                        sheetRenderer = renderer
                        sheet_image.post {
                            renderCurrentPage()
                            initPageCount()
                        }
                    }, { error ->
                        Log.e("AutoFlip", error.toString())
                    })
        } else if (imageUriString != null) {
            uri = imageUriString
        } else {
            finish()
        }
    }

    private fun initPageCount() {
        val renderer = sheetRenderer
        page_number_text.text = (pageIndex + 1).toString()
        if (renderer is PdfSheetRenderer) {
            of_text.visibility = View.VISIBLE
            page_count_text.text = renderer.getPageCount().toString()
            page_count_text.visibility = View.VISIBLE
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
            title_field.visibility = View.VISIBLE
            title = null
            changeTitleButton?.setIcon(R.drawable.ic_done_white_24dp)
            changeTitleButton?.title = resources.getString(R.string.action_save)
        }
    }

    private fun renderCurrentPage() {
        val bitmap = sheetRenderer?.renderFullPage(pageIndex, sheet_image.width)
        sheet_image.setImageBitmap(bitmap)
    }

    private fun insertSheet() {
        sheet.name = resources.getString(R.string.untitled)
        Single.fromCallable {
            database?.sheetDao()?.insertSheet(sheet)
        }.subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread()).subscribe { sheetId ->
            sheet.id = sheetId ?: 0
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
}
