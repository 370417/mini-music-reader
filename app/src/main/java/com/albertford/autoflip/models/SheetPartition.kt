package com.albertford.autoflip.models

import android.os.Parcel
import android.os.Parcelable
import com.albertford.autoflip.Selection
import com.albertford.autoflip.StaffSelection
import com.albertford.autoflip.suggestStaff
import kotlin.collections.ArrayList

class SheetPartition(val uri: String, val name: String, val bpm: Float, val bpb: Int, var selection: Selection) : Parcelable {

    val pages: MutableList<Page> = ArrayList()

    constructor(uri: String, name: String, bpm: Float, bpb: Int) : this(uri, name, bpm, bpb, StaffSelection(0f, 0f)) {
        selection = suggestStaff(this)
    }

    private constructor(parcel: Parcel) : this(parcel.readString(), parcel.readString(), parcel.readFloat(), parcel.readInt(), parcel.readParcelable(null)) {
        parcel.readTypedList(pages, Page.CREATOR)
    }

    override fun describeContents() = 0

    override fun writeToParcel(parcel: Parcel, i: Int) {
        parcel.writeString(uri)
        parcel.writeString(name)
        parcel.writeFloat(bpm)
        parcel.writeInt(bpb)
        parcel.writeParcelable(selection, 0)
        parcel.writeTypedList(pages)
    }

//    fun countBars(): Int {
//        var barCount = 0
//        for (page in pages) {
//            for (staff in page.staves) {
//                barCount += staff.barLines.size - 1
//            }
//        }
//        return barCount
//    }

    companion object {

        val CREATOR: Parcelable.Creator<SheetPartition> = object : Parcelable.Creator<SheetPartition> {
            override fun createFromParcel(parcel: Parcel) = SheetPartition(parcel)

            override fun newArray(size: Int): Array<SheetPartition?> = arrayOfNulls(size)
        }
    }
}
