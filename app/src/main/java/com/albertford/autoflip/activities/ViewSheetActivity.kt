package com.albertford.autoflip.activities

import android.animation.ObjectAnimator
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.os.Parcel
import android.os.Parcelable
import android.view.View
import android.view.animation.LinearInterpolator
import com.albertford.autoflip.*
import com.albertford.autoflip.room.Bar
import com.albertford.autoflip.room.PDF_SHEET
import com.albertford.autoflip.room.PageUri
import com.albertford.autoflip.room.Sheet
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.activity_view_sheet.*

class ViewSheetActivity : AppCompatActivity() {

    private lateinit var state: State

    private var sheet: Sheet? = null
    private var bars: List<Bar>? = null
    private var pageUris: List<PageUri>? = null
    private var sheetRenderer: SheetRenderer? = null
    private var scale = 1f

//    private lateinit var sheetRenderer: SheetRenderer
//
//    private lateinit var sheetPartition: SheetPartition
//
//    private lateinit var barList: List<Bar>
//    private var barIndex = 0
//
//    private var scale = 1f
//
//    private var renderHandler = Handler()
//    private var renderRunnable = object : Runnable {
//        override fun run() {
//            if (barIndex == barList.size) {
//                return
//            }
//            renderBar()
//            barIndex++
//            val delay = Math.round(60000.0 * sheetPartition.bpb / sheetPartition.bpm)
//            renderHandler.postDelayed(this, delay)
//        }
//    }
//
//    private var countDownDunnable = object : Runnable {
//        override fun run() {
//
//        }
//    }
//
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_view_sheet)

        state = if (savedInstanceState != null) {
            savedInstanceState.getParcelable("STATE")
        } else {
            State(this)
        }
        loadSheet()
//
//        secondary_image.post {
//            scale = sheetRenderer.findMaxTwoBarScale(barList, play_button.width, play_button.height)
//            renderBar()
//        }
//
        progress_bar.rotation = 270f
        play_button.setOnClickListener(playButtonListener)
    }

    override fun onSaveInstanceState(outState: Bundle?) {
        super.onSaveInstanceState(outState)
        outState?.putParcelable("STATE", state)
    }
//
//    private fun renderBar() {
//        val primaryBitmap = sheetRenderer.renderBar(barList, barIndex, scale)
//        primary_image.setImageBitmap(primaryBitmap)
//        val secondaryBitmap = if (barIndex < barList.size - 1) {
//            sheetRenderer.renderBar(barList, barIndex + 1, scale)
//        } else {
//            null
//        }
//        secondary_image.setImageBitmap(secondaryBitmap)
//    }
//

    private val playButtonListener = View.OnClickListener {
        val animation = ObjectAnimator.ofInt(progress_bar, "progress", 0, 100)
        animation.duration = 500
        animation.interpolator = LinearInterpolator()
        animation.start()
    }

    private fun loadSheet() {
        database?.sheetDao()?.selectSheetById(state.sheetId)
                ?.subscribeOn(Schedulers.io())
                ?.observeOn(AndroidSchedulers.mainThread())
                ?.subscribe { result ->
                    sheet = result.sheet
                    bars = result.bars
                    pageUris = result.pageUris
                    loadRenderer()
                }
    }

    private fun loadRenderer() {
        val sheet = sheet
        val pageUris = pageUris
        val bars = bars
        if (sheet == null || pageUris == null || bars == null) {
            return
        }
        if (sheet.type == PDF_SHEET) {
            val pageUri = pageUris[0].uri
            pageUri ?: return
            Single.fromCallable {
                PdfSheetRenderer(this, pageUri)
            }.subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread()).subscribe { renderer ->
                secondary_image.post {
                    sheetRenderer = renderer
                    scale = renderer.findMaxBarScale(bars, play_button.width, play_button.height)
                    primary_image.setImageBitmap(renderer.renderBar(bars, 0, scale))
                }
            }
        }
    }

    private class State : Parcelable {

        val sheetId: Long
        var barIndex: Int

        constructor(activity: ViewSheetActivity) {
            sheetId = activity.intent.getLongExtra("SHEET_ID", -1)
            barIndex = 0
        }

        private constructor(parcel: Parcel) {
            sheetId = parcel.readLong()
            barIndex = parcel.readInt()
        }

        override fun writeToParcel(parcel: Parcel?, int: Int) {
            parcel?.writeLong(sheetId)
            parcel?.writeInt(barIndex)
        }

        override fun describeContents(): Int = 0

        companion object CREATOR : Parcelable.Creator<State> {
            override fun newArray(size: Int): Array<State?> = arrayOfNulls(size)

            override fun createFromParcel(parcel: Parcel): State = State(parcel)
        }
    }
}
