package com.albertford.autoflip.models

import android.os.Parcel
import android.os.Parcelable
import com.albertford.autoflip.room.Bar

class PagePartition(val pageIndex: Int, var scale: Float, var staffSelected: Boolean, var selectedBarIndex: Int) : Parcelable {

    val staves: MutableList<StaffPartition> = ArrayList()

    constructor(pageIndex: Int, scale: Float) : this(pageIndex, scale, false, -1)

    constructor(parcel: Parcel) : this(parcel.readInt(), parcel.readFloat(), parcel.readInt() == 0, parcel.readInt()) {
        parcel.readTypedList(staves, StaffPartition.CREATOR)
    }

    override fun describeContents() = 0

    override fun writeToParcel(parcel: Parcel?, int: Int) {
        parcel?.writeInt(pageIndex)
        parcel?.writeFloat(scale)
        parcel?.writeInt(if (staffSelected) 1 else 0)
        parcel?.writeInt(selectedBarIndex)
        parcel?.writeTypedList(staves)
    }

    /**
     * Insert a new staff and select it.
     * @param initY inital click y coordinate
     * @param dragY y coordinate of drag
     * @return true if dragY corresponds to the staff's start, not end
     */
    fun insertNewStaff(initY: Float, dragY: Float) : Boolean {
        val staff = StaffPartition(initY)
        staves.add(staff)
        staffSelected = true
        selectedBarIndex = -1
        return if (initY < dragY) {
            staff.end = dragY
            false
        } else {
            staff.start = dragY
            true
        }
    }

    fun deselectStaff() {
        staves.last().bars.sort()
        staffSelected = false
        selectedBarIndex = -1
    }

    fun write(sheetId: Int, pageScale: Int) {
        staves.sort()
        val bars: MutableList<Bar> = ArrayList()
        var totalBarIndex = 0
        val factor = pageScale / scale
        for (staff in staves) {
            for (barIndex in 0 until staff.bars.size - 1) {
                val firstBar = staff.bars[barIndex]
                val secondBar = staff.bars[barIndex + 1]
                bars.add(Bar(
                        sheetId,
                        totalBarIndex,
                        pageIndex,
                        staff.start * factor,
                        firstBar.x * factor,
                        (secondBar.x - firstBar.x) * factor,
                        (staff.end - staff.start) * factor,
                        1f,
                        1,
                        false,
                        false
                ))
                totalBarIndex += 1
            }
        }
    }

    companion object CREATOR : Parcelable.Creator<PagePartition> {
        override fun createFromParcel(parcel: Parcel?) = if (parcel != null) {
            PagePartition(parcel)
        } else {
            PagePartition(0, 1f)
        }

        override fun newArray(size: Int) = arrayOfNulls<PagePartition>(size)
    }

}
