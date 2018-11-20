package com.albertford.autoflip.models

import android.os.Parcel
import android.os.Parcelable

@Deprecated("")
class BarLine(var x: Float, var bpm: Float, var bpb: Int, var beginRepeat: Boolean, var endRepeat: Boolean) : Comparable<BarLine>, Parcelable {

    constructor(x: Float) : this(x, -1f, -1, false, false)

    constructor(parcel: Parcel) :
            this(parcel.readFloat(), parcel.readFloat(), parcel.readInt(), parcel.readInt() == 1, parcel.readInt() == 1)

    override fun compareTo(other: BarLine) = when {
        x < other.x -> -1
        x > other.x -> 1
        else -> 0
    }

    override fun describeContents() = 0

    override fun writeToParcel(parcel: Parcel?, int: Int) {
        parcel?.run {
            writeFloat(x)
            writeFloat(bpm)
            writeInt(bpb)
            writeInt(if (beginRepeat) 1 else 0)
            writeInt(if (endRepeat) 1 else 0)
        }
    }

    companion object CREATOR : Parcelable.Creator<BarLine> {
        override fun createFromParcel(parcel: Parcel) = BarLine(parcel)

        override fun newArray(size: Int) = arrayOfNulls<BarLine>(size)
    }

}