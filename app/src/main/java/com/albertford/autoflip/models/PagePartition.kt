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

    fun insertNewStaffIndex(height: Float): Int {
        val staff = StaffPartition(height)
        var index = staves.binarySearch(staff)
        if (index < 0) {
            index = -index - 1
        }
        return index
    }

    fun insertNewStaff(height: Float, index: Int) {
        staves.add(index, StaffPartition(height))
        selectedStaffIndex = index
        selectedBarIndex = -1
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
