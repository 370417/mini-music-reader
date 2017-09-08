package com.albertford.autoflip.models

import android.os.Parcel
import android.os.Parcelable

class BarPartition(var x: Float, var leftBeginRepeat: Boolean, var rightEndRepeat: Boolean) : Comparable<BarPartition>, Parcelable {

    constructor(x: Float) : this(x, false, false)

    constructor(parcel: Parcel) : this(parcel.readFloat(), parcel.readInt() == 1, parcel.readInt() == 1)

    override fun compareTo(other: BarPartition) = when {
        x < other.x -> -1
        x > other.x -> 1
        else -> 0
    }

    override fun describeContents() = 0

    override fun writeToParcel(parcel: Parcel?, int: Int) {
        parcel?.writeFloat(x)
        parcel?.writeInt(if (leftBeginRepeat) 1 else 0)
        parcel?.writeInt(if (rightEndRepeat) 1 else 0)
    }

    companion object CREATOR : Parcelable.Creator<BarPartition> {
        override fun createFromParcel(parcel: Parcel?) = if (parcel != null) {
            BarPartition(parcel)
        } else {
            BarPartition(0f)
        }

        override fun newArray(size: Int) = arrayOfNulls<BarPartition>(size)
    }

}