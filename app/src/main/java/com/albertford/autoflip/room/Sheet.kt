package com.albertford.autoflip.room

import android.arch.persistence.room.Entity
import android.arch.persistence.room.Index
import android.arch.persistence.room.PrimaryKey
import android.os.Parcel
import android.os.Parcelable

@Entity(indices = [(Index("id"))])
class Sheet(
        var name: String,
        var uri: String,
        val pageCount: Int
) : Parcelable {
    @PrimaryKey(autoGenerate = true)
    var id: Long = 0L

    var firstStaffTop: Float? = null
    var firstStaffBottom: Float? = null
    var firstStaffPageIndex: Int? = null

    fun updateFirstStaff(pages: Array<Page>) {
        val firstStaff = pages.firstOrNull { page ->
            page.staves.size > 0
        }?.staves?.first()
        firstStaffTop = firstStaff?.top
        firstStaffBottom = firstStaff?.bottom
        firstStaffPageIndex = firstStaff?.pageIndex
    }

    constructor(parcel: Parcel) : this(
            parcel.readString() ?: "",
            parcel.readString() ?: "",
            parcel.readInt()) {
        id = parcel.readLong()
        val top = parcel.readFloat()
        val bottom = parcel.readFloat()
        val pageIndex = parcel.readInt()
        if (top >= 0f) {
            firstStaffTop = top
        }
        if (bottom >= 0f) {
            firstStaffBottom = bottom
        }
        if (pageIndex >= 0) {
            firstStaffPageIndex = pageIndex
        }
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(name)
        parcel.writeString(uri)
        parcel.writeInt(pageCount)
        parcel.writeLong(id)
        parcel.writeFloat(firstStaffTop ?: -1f)
        parcel.writeFloat(firstStaffBottom ?: -1f)
        parcel.writeInt(firstStaffPageIndex ?: -1)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<Sheet> {
        override fun createFromParcel(parcel: Parcel): Sheet {
            return Sheet(parcel)
        }

        override fun newArray(size: Int): Array<Sheet?> {
            return arrayOfNulls(size)
        }
    }
}
