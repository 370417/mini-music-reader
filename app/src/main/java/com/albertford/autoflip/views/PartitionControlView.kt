package com.albertford.autoflip.views

import android.content.Context
import android.os.Parcel
import android.os.Parcelable
import android.support.design.widget.BottomSheetBehavior
import android.support.design.widget.CoordinatorLayout
import android.util.AttributeSet
import android.view.View
import com.albertford.autoflip.R
import kotlinx.android.synthetic.main.view_partition_control.view.*

class PartitionControlView(context: Context, attrs: AttributeSet) : CoordinatorLayout(context, attrs) {

    var pageIndex = 0
        set(value) {
            page_count_text.text = resources.getString(R.string.page_count, value + 1, pageCount)
        }

    var pageCount = 0
        set(value) {
            page_count_text.text = resources.getString(R.string.page_count, pageIndex + 1, value)
        }

    var partitionControlled: PartitionControlled? = null

    private lateinit var bottomSheetBehavior: BottomSheetBehavior<View>
    private var expandBottomSheetOnInit = true

    private val bottomSheetCallback = object : BottomSheetBehavior.BottomSheetCallback() {
        override fun onSlide(bottomSheet: View, slideOffset: Float) {
            partitionControlled?.setSlideOffset(slideOffset)
        }

        override fun onStateChanged(bottomSheet: View, newState: Int) {
            if (newState == BottomSheetBehavior.STATE_COLLAPSED) {
                partitionControlled?.endCollapse()
                begin_repeat_checkbox.visibility = View.VISIBLE
                end_repeat_checkbox.visibility = View.VISIBLE
                bottom_cancel_button.visibility = View.VISIBLE
                bottom_apply_button.visibility = View.VISIBLE
            }
        }

    }

    private val startButtonListener = View.OnClickListener {
        val beatsPerMinute = validateBeatsPerMinute() ?: return@OnClickListener
        val beatsPerMeasure = validateBeatsPerMeasure() ?: return@OnClickListener
        start_button.visibility = View.GONE
        val buttonVisibilities = partitionControlled?.startPages(beatsPerMinute, beatsPerMeasure)
        next_page_button.visibility = buttonVisibilities?.next ?: next_page_button.visibility
        finish_button.visibility = buttonVisibilities?.finish ?: finish_button.visibility
        collapse()
    }

    private val nextButtonListener = View.OnClickListener {
        val buttonVisibilities = partitionControlled?.nextPage()
        next_page_button.visibility = buttonVisibilities?.next ?: next_page_button.visibility
        finish_button.visibility = buttonVisibilities?.finish ?: finish_button.visibility
    }

    private val finishButtonListener = View.OnClickListener {
        finish_button.isEnabled = false
        partitionControlled?.finishPages()
    }

    private val cancelButtonListener = View.OnClickListener {
        partitionControlled?.cancelBar()
        next_page_button.isEnabled = true
        finish_button.isEnabled = true
        collapse()
    }

    private val applyButtonListener = View.OnClickListener {
        partitionControlled?.applyBar(
            beatsPerMeasure = beats_measure_field.text.toString().toIntOrNull(),
            beatsPerMinute = beats_minute_field.text.toString().toFloatOrNull(),
            beginRepeat = begin_repeat_checkbox.isChecked,
            endRepeat = end_repeat_checkbox.isChecked
        )
    }

    init {
        inflate(context, R.layout.view_partition_control, this)
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        bottomSheetBehavior = BottomSheetBehavior.from(this)
        bottomSheetBehavior.setBottomSheetCallback(bottomSheetCallback)
        if (expandBottomSheetOnInit) {
            bottomSheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED
        }
        start_button.setOnClickListener(startButtonListener)
        next_page_button.setOnClickListener(nextButtonListener)
        finish_button.setOnClickListener(finishButtonListener)
        bottom_cancel_button.setOnClickListener(cancelButtonListener)
        bottom_apply_button.setOnClickListener(applyButtonListener)
    }

    override fun onRestoreInstanceState(state: Parcelable?) {
        when (state) {
            is ParitionControlState -> {
                super.onRestoreInstanceState(state.superState)
                start_button.visibility = state.startButtonVisibility
                next_page_button.visibility = state.nextButtonVisibility
                finish_button.visibility = state.finishButtonVisibility
                begin_repeat_checkbox.visibility = state.barControlsVisibility
                end_repeat_checkbox.visibility = state.barControlsVisibility
                bottom_cancel_button.visibility = state.barControlsVisibility
                bottom_apply_button.visibility = state.barControlsVisibility
                expandBottomSheetOnInit = state.bottomSheetState == BottomSheetBehavior.STATE_EXPANDED
                pageIndex = state.pageIndex
            }
            else -> super.onRestoreInstanceState(state)
        }
    }

    override fun onSaveInstanceState(): Parcelable {
        val superState = super.onSaveInstanceState()
        return ParitionControlState(superState, this)
    }

    fun expand(beginRepeat: Boolean, endRepeat: Boolean) {
        bottomSheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED
        partitionControlled?.beginExpand()
        next_page_button.isEnabled = false
        finish_button.isEnabled = false
        beats_minute_field.run {
            text = null
            isFocusable = true
            isFocusableInTouchMode = true
            isEnabled = true
        }
        beats_measure_field.run {
            text = null
            isFocusable = true
            isFocusableInTouchMode = true
            isEnabled = true
        }
        begin_repeat_checkbox.isChecked = beginRepeat
        end_repeat_checkbox.isChecked = endRepeat
    }

    private fun collapse() {
        bottomSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
        partitionControlled?.beginCollapse()
        beats_minute_field.run {
            isFocusable = false
            isFocusableInTouchMode = false
            isEnabled = false
        }
        beats_measure_field.run {
            isFocusable = false
            isFocusableInTouchMode = false
            isEnabled = false
        }
    }

    private fun validateBeatsPerMinute(): Float? {
        val fieldEmpty = beats_minute_field.text?.isEmpty()
        return if (fieldEmpty == true) {
            beats_minute_layout.error = resources.getString(R.string.error_required_field)
            null
        } else {
            beats_minute_layout.error = null
            beats_minute_field.text.toString().toFloat()
        }
    }

    private fun validateBeatsPerMeasure(): Int? {
        val fieldEmpty = beats_measure_field.text?.isEmpty()
        return if (fieldEmpty == true) {
            beats_measure_layout.error = resources.getString(R.string.error_required_field)
            null
        } else {
            beats_measure_layout.error = null
            beats_measure_field.text.toString().toInt()
        }
    }
}

private class ParitionControlState : View.BaseSavedState {

    val startButtonVisibility: Int
    val nextButtonVisibility: Int
    val finishButtonVisibility: Int
    val barControlsVisibility: Int
    val bottomSheetState: Int
    val pageIndex: Int

    constructor(savedState: Parcelable, view: PartitionControlView) : super(savedState) {
        startButtonVisibility = view.start_button.visibility
        nextButtonVisibility = view.next_page_button.visibility
        finishButtonVisibility = view.finish_button.visibility
        barControlsVisibility = view.bottom_apply_button.visibility
        bottomSheetState = BottomSheetBehavior.STATE_EXPANDED
        pageIndex = view.pageIndex
    }

    private constructor(parcel: Parcel) : super(parcel) {
        startButtonVisibility = parcel.readInt()
        nextButtonVisibility = parcel.readInt()
        finishButtonVisibility = parcel.readInt()
        barControlsVisibility = parcel.readInt()
        bottomSheetState = parcel.readInt()
        pageIndex = parcel.readInt()
    }

    override fun writeToParcel(parcel: Parcel?, flags: Int) {
        parcel?.run {
            writeInt(startButtonVisibility)
            writeInt(nextButtonVisibility)
            writeInt(finishButtonVisibility)
            writeInt(barControlsVisibility)
            writeInt(bottomSheetState)
            writeInt(pageIndex)
        }
    }

    companion object CREATOR : Parcelable.Creator<ParitionControlState> {
        override fun newArray(size: Int): Array<ParitionControlState?> = arrayOfNulls(size)

        override fun createFromParcel(parcel: Parcel) = ParitionControlState(parcel)
    }
}

interface PartitionControlled {
    fun startPages(beatsPerMinute: Float, beatsPerMeasure: Int): ButtonVisibilities
    fun nextPage(): ButtonVisibilities?
    fun finishPages()
    fun cancelBar()
    fun applyBar(beatsPerMinute: Float?, beatsPerMeasure: Int?, beginRepeat: Boolean?, endRepeat: Boolean?)
    fun setSlideOffset(slideOffset: Float)
    fun beginExpand()
    fun beginCollapse()
    fun endCollapse()
}

class ButtonVisibilities(val next: Int, val finish: Int)
