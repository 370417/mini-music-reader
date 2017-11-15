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

class PartitionControlView(context: Context?, attrs: AttributeSet) : CoordinatorLayout(context, attrs) {

    var pageIndex = 0
        set(value) {
            page_number_text.text = (value + 1).toString()
        }

    var pageCount = 0
        set(value) {
            if (value > 0) {
                page_count_text.text = value.toString()
                of_text.visibility = View.VISIBLE
                page_count_text.visibility = View.VISIBLE
            } else {
                of_text.visibility = View.GONE
                page_count_text.visibility = View.GONE
            }
        }

    var partitionControlled: PartitionControlled? = null

    /// TODO: maybe inflate before this
    private var bottomSheetBehavior: BottomSheetBehavior<View> = BottomSheetBehavior.from(this)

    private val bottomSheetCallback = object : BottomSheetBehavior.BottomSheetCallback() {
        override fun onSlide(bottomSheet: View, slideOffset: Float) {
            partitionControlled?.setSlideOffset(slideOffset)
        }

        override fun onStateChanged(bottomSheet: View, newState: Int) {
            if (newState == BottomSheetBehavior.STATE_COLLAPSED) {
                next_page_button.isEnabled = true
                finish_button.isEnabled = true
                begin_repeat_checkbox.visibility = View.VISIBLE
                end_repeat_checkbox.visibility = View.VISIBLE
                bottom_cancel_button.visibility = View.VISIBLE
                bottom_apply_button.visibility = View.VISIBLE
            }
        }

    }

    private val startButtonListener = View.OnClickListener {
        val beatsPerMinuteValid = validateBeatsPerMinute()
        val beatsPerMeasureValid = validateBeatsPerMeasure()
        if (!beatsPerMinuteValid || !beatsPerMeasureValid) {
            return@OnClickListener
        }
        start_button.visibility = View.GONE
        val buttonVisibilities = partitionControlled?.startPages()
        next_page_button.visibility = buttonVisibilities?.next ?: next_page_button.visibility
        finish_button.visibility = buttonVisibilities?.finish ?: finish_button.visibility
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
        bottomSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
    }

    init {
        inflate(context, R.layout.view_partition_control, this)
        bottomSheetBehavior.setBottomSheetCallback(bottomSheetCallback)
        start_button.setOnClickListener(startButtonListener)
        next_page_button.setOnClickListener(nextButtonListener)
        finish_button.setOnClickListener(finishButtonListener)
        bottom_cancel_button.setOnClickListener(cancelButtonListener)
    }

    override fun onRestoreInstanceState(state: Parcelable?) {
        when (state) {
            is State -> {
                super.onRestoreInstanceState(state.superState)
                start_button.visibility = state.startButtonVisibility
                next_page_button.visibility = state.nextButtonVisibility
                finish_button.visibility = state.finishButtonVisibility
                begin_repeat_checkbox.visibility = state.beginCheckboxVisibility
                end_repeat_checkbox.visibility = state.endCheckboxVisibilty
                bottomSheetBehavior.state = state.bottomSheetState
                pageIndex = state.pageIndex
            }
            else -> super.onRestoreInstanceState(state)
        }
    }

    override fun onSaveInstanceState(): Parcelable {
        val superState = super.onSaveInstanceState()
        return State(superState, this)
    }

    fun expand(beginRepeat: Boolean, endRepeat: Boolean) {
        bottomSheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED
        next_page_button.isEnabled = false
        finish_button.isEnabled = false
        beats_minute_field.text = null
        beats_measure_field.text = null
        begin_repeat_checkbox.isChecked = beginRepeat
        end_repeat_checkbox.isChecked = endRepeat
    }

    fun collapse() {
        bottomSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
    }

    private fun validateBeatsPerMinute(): Boolean {
        val fieldEmpty = beats_minute_field.text.isEmpty()
        if (fieldEmpty) {
            beats_minute_layout.error = resources.getString(R.string.error_required_field)
        } else {
            beats_minute_layout.error = null
        }
        return !fieldEmpty
    }

    private fun validateBeatsPerMeasure(): Boolean {
        val fieldEmpty = beats_measure_field.text.isEmpty()
        if (fieldEmpty) {
            beats_measure_layout.error = resources.getString(R.string.error_required_field)
        } else {
            beats_measure_layout.error = null
        }
        return !fieldEmpty
    }
}

private class State : View.BaseSavedState {

    val startButtonVisibility: Int
    val nextButtonVisibility: Int
    val finishButtonVisibility: Int
    val beginCheckboxVisibility: Int
    val endCheckboxVisibilty: Int
    val bottomSheetState: Int
    val pageIndex: Int

    constructor(savedState: Parcelable, view: PartitionControlView) : super(savedState) {
        startButtonVisibility = view.start_button.visibility
        nextButtonVisibility = view.next_page_button.visibility
        finishButtonVisibility = view.finish_button.visibility
        beginCheckboxVisibility = view.begin_repeat_checkbox.visibility
        endCheckboxVisibilty = view.end_repeat_checkbox.visibility
        bottomSheetState = BottomSheetBehavior.STATE_EXPANDED
        pageIndex = view.pageIndex
    }

    private constructor(parcel: Parcel) : super(parcel) {
        startButtonVisibility = parcel.readInt()
        nextButtonVisibility = parcel.readInt()
        finishButtonVisibility = parcel.readInt()
        beginCheckboxVisibility = parcel.readInt()
        endCheckboxVisibilty = parcel.readInt()
        bottomSheetState = parcel.readInt()
        pageIndex = parcel.readInt()
    }

    override fun writeToParcel(parcel: Parcel?, flags: Int) {
        parcel ?: return
        parcel.writeInt(startButtonVisibility)
        parcel.writeInt(nextButtonVisibility)
        parcel.writeInt(finishButtonVisibility)
        parcel.writeInt(beginCheckboxVisibility)
        parcel.writeInt(endCheckboxVisibilty)
        parcel.writeInt(bottomSheetState)
        parcel.writeInt(pageIndex)
    }

    override fun describeContents(): Int = 0

    companion object CREATOR : Parcelable.Creator<State> {
        override fun newArray(size: Int): Array<State?> = arrayOfNulls(size)

        override fun createFromParcel(parcel: Parcel): State = State(parcel)
    }
}

interface PartitionControlled {
    fun startPages(): ButtonVisibilities?
    fun nextPage(): ButtonVisibilities?
    fun finishPages()
    fun setSlideOffset(slideOffset: Float)
}

class ButtonVisibilities(val next: Int, val finish: Int)
