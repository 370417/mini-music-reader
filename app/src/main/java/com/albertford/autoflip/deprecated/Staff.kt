package com.albertford.autoflip.deprecated

import android.os.Parcel
import android.os.Parcelable

@Deprecated("")
class Staff(var start: Float, var end: Float) : Comparable<Staff>, Parcelable {

    val barLines: MutableList<BarLine> = ArrayList()

    private val center: Float
        get() = (start + end) / 2

    init { reorder() }

    constructor(height: Float) : this(height, height)

    constructor(parcel: Parcel) : this(parcel.readFloat(), parcel.readFloat()) {
        parcel.readTypedList(barLines, BarLine.CREATOR)
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

    override fun compareTo(other: Staff): Int = when {
        center < other.center -> -1
        other.center > center -> 1
        else -> 0
    }

    override fun describeContents() = 0

    override fun writeToParcel(parcel: Parcel?, int: Int) {
        parcel?.writeFloat(start)
        parcel?.writeFloat(end)
        parcel?.writeTypedList(barLines)
    }

    companion object CREATOR : Parcelable.Creator<Staff> {
        override fun createFromParcel(parcel: Parcel?) = if (parcel != null) {
            Staff(parcel)
        } else {
            Staff(0f)
        }

        override fun newArray(size: Int) = arrayOfNulls<Staff>(size)
    }

}