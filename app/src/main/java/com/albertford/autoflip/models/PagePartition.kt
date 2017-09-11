package com.albertford.autoflip.models

import android.os.Parcel
import android.os.Parcelable

class PagePartition(var selectedStaffIndex: Int, var selectedBarIndex: Int) : Parcelable {

    val staves: MutableList<StaffPartition> = ArrayList()

    constructor() : this(-1, -1)

    constructor(parcel: Parcel) : this(parcel.readInt(), parcel.readInt()) {
        parcel.readTypedList(staves, StaffPartition.CREATOR)
    }

    override fun describeContents() = 0

    override fun writeToParcel(parcel: Parcel?, int: Int) {
        parcel?.writeInt(selectedStaffIndex)
        parcel?.writeInt(selectedBarIndex)
        parcel?.writeTypedList(staves)
    }

    fun getSelectedStaff() = staves[selectedStaffIndex]

    /**
     * Insert a new staff and select it.
     * @param initY inital click y coordinate
     * @param dragY y coordinate of drag
     * @return true if dragY corresponds to the staff's start, not end
     */
    fun insertNewStaff(initY: Float, dragY: Float) : Boolean {
        val staff = StaffPartition(initY)
        val index = staves.binarySearch(staff)
        val positiveIndex = if (index < 0) {
            -index - 1
        } else {
            index
        }
        staves.add(positiveIndex, staff)
        selectedStaffIndex = positiveIndex
        selectedBarIndex = -1
        return if (initY < dragY) {
            staff.end = dragY
            false
        } else {
            staff.start = dragY
            true
        }
    }

    companion object CREATOR : Parcelable.Creator<PagePartition> {
        override fun createFromParcel(parcel: Parcel?) = if (parcel != null) {
            PagePartition(parcel)
        } else {
            PagePartition()
        }

        override fun newArray(size: Int) = arrayOfNulls<PagePartition>(size)
    }

}
