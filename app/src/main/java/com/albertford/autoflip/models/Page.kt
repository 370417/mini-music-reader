package com.albertford.autoflip.models

import android.os.Parcel
import android.os.Parcelable
import kotlin.collections.ArrayList

class Page() : Parcelable {

    val staves: MutableList<Staff> = ArrayList()

    private constructor(parcel: Parcel) : this() {
        parcel.readTypedList(staves, Staff.CREATOR)
    }

    override fun describeContents() = 0

    override fun writeToParcel(parcel: Parcel, i: Int) {
        parcel.writeTypedList(staves)
    }

    companion object {

        val CREATOR: Parcelable.Creator<Page> = object : Parcelable.Creator<Page> {
            override fun createFromParcel(parcel: Parcel) = Page(parcel)

            override fun newArray(size: Int): Array<Page?> = arrayOfNulls(size)
        }
    }
}
