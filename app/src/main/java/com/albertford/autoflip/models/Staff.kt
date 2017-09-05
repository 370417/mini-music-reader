package com.albertford.autoflip.models

import android.os.Parcel
import android.os.Parcelable

class Staff(var startY: Float, var endY: Float) : Parcelable {

    var barLines: MutableList<Float> = ArrayList()

    private constructor(parcel: Parcel) : this(parcel.readFloat(), parcel.readFloat()) {
        val length = parcel.readInt()
        val barLinesArray = FloatArray(length)
        parcel.readFloatArray(barLinesArray)
        barLines = barLinesArray.toMutableList()
    }

    override fun describeContents() = 0

    override fun writeToParcel(parcel: Parcel, i: Int) {
        parcel.writeFloat(startY)
        parcel.writeFloat(endY)
        parcel.writeInt(barLines.size)
        parcel.writeFloatArray(barLines.toFloatArray())
    }

    companion object {

        val CREATOR: Parcelable.Creator<Staff> = object : Parcelable.Creator<Staff> {
            override fun createFromParcel(parcel: Parcel) = Staff(parcel)

            override fun newArray(size: Int): Array<Staff?> = arrayOfNulls(size)
        }
    }
}
