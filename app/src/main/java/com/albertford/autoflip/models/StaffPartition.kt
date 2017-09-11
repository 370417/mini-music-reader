package com.albertford.autoflip.models

import android.os.Parcel
import android.os.Parcelable

class StaffPartition(var start: Float, var end: Float) : Comparable<StaffPartition>, Parcelable {

    val bars: MutableList<BarPartition> = ArrayList()

    val center: Float
        get() = (start + end) / 2

    init { reorder() }

    constructor(height: Float) : this(height, height)

    constructor(parcel: Parcel) : this(parcel.readFloat(), parcel.readFloat()) {
        parcel.readTypedList(bars, BarPartition.CREATOR)
    }

    /**
     * Flip start & end if start > end.
     * @return true if start & end were flipped.
     */
    fun reorder(): Boolean {
        return if (start > end) {
            val temp = start
            start = end
            end = temp
            true
        } else {
            false
        }
    }

    override fun compareTo(other: StaffPartition): Int = when {
        center < other.center -> -1
        other.center > center -> 1
        else -> 0
    }

    override fun describeContents() = 0

    override fun writeToParcel(parcel: Parcel?, int: Int) {
        parcel?.writeFloat(start)
        parcel?.writeFloat(end)
        parcel?.writeTypedList(bars)
    }

    companion object CREATOR : Parcelable.Creator<StaffPartition> {
        override fun createFromParcel(parcel: Parcel?) = if (parcel != null) {
            StaffPartition(parcel)
        } else {
            StaffPartition(0f)
        }

        override fun newArray(size: Int) = arrayOfNulls<StaffPartition>(size)
    }

}