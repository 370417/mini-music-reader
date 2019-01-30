package com.albertford.autoflip.deprecated

import android.os.Parcel
import android.os.Parcelable
import com.albertford.autoflip.room.Bar

private const val DEFAULT_BPM = 60f
private const val DEFAULT_BPB = 4

@Deprecated("")
class Page(
        private val pageIndex: Int,
        private var scale: Float,
        var staffSelected: Boolean,
        var selectedBarIndex: Int,
        private var barIndex: Int,
        private val initBpm: Float,
        private val initBpb: Int
) : Parcelable {

    val staves: MutableList<Staff> = mutableListOf()

    constructor(pageIndex: Int, scale: Float) : this(pageIndex, scale, false, -1, 0,
            DEFAULT_BPM,
            DEFAULT_BPB)

    constructor(page: Page, initBpm: Float?, initBpb: Int?) :
            this(page.pageIndex + 1, page.scale, false, -1, page.barIndex + 1, initBpm ?: DEFAULT_BPM, initBpb ?: DEFAULT_BPB)

    constructor(parcel: Parcel) : this(
            parcel.readInt(),
            parcel.readFloat(),
            parcel.readInt() == 0,
            parcel.readInt(),
            parcel.readInt(),
            parcel.readFloat(),
            parcel.readInt()
    ) {
        parcel.readTypedList(staves, Staff)
    }

    override fun describeContents() = 0

    override fun writeToParcel(parcel: Parcel?, int: Int) {
        parcel?.run {
            writeInt(pageIndex)
            writeFloat(scale)
            writeInt(if (staffSelected) 1 else 0)
            writeInt(selectedBarIndex)
            writeInt(barIndex)
            writeFloat(initBpm)
            writeInt(initBpb)
            writeTypedList(staves)
        }
    }

    /**
     * @return true if dragY corresponds to the staff's start, not end
     */
    fun insertNewStaff(initY: Float, dragY: Float) : Boolean {
        val staff = Staff(initY)
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
        staves.last().barLines.sort()
        staffSelected = false
        selectedBarIndex = -1
    }

    fun toBarArray(sheetId: Long, pageScale: Int): Array<Bar> {
        val factor = pageScale / scale
        var currentBpm = initBpm
        var currentBpb = initBpb
        val bars: MutableList<Bar> = ArrayList()
        for (staff in staves) {
            for (barLineIndex in 0 until staff.barLines.size - 1) {
                val firstBarLine = staff.barLines[barLineIndex]
                val secondBarLine = staff.barLines[barLineIndex + 1]
                if (firstBarLine.bpb > 0) {
                    currentBpb = firstBarLine.bpb
                }
                if (firstBarLine.bpm > 0) {
                    currentBpm = firstBarLine.bpm
                }
                bars.add(Bar(
                        sheetId = sheetId,
                        barIndex = barIndex,
                        pageIndex = pageIndex,
                        top = staff.start * factor,
                        left = firstBarLine.x * factor,
                        width = (secondBarLine.x - firstBarLine.x) * factor,
                        height = (staff.end - staff.start) * factor,
                        beatsPerMinute = currentBpm,
                        beatsPerMeasure = currentBpb,
                        isLeftBeginRepeat = firstBarLine.beginRepeat,
                        isRightEndRepeat = secondBarLine.endRepeat
                ))
                barIndex++
            }
        }
        return bars.toTypedArray()
    }

    /**
     * @return the y-coordinate of the midpoint of the selected staff
     */
    fun selectedStaffPosition(): Float {
        val staff = staves.last()
        return (staff.start + staff.end) / 2
    }

    companion object CREATOR : Parcelable.Creator<Page> {
        override fun createFromParcel(parcel: Parcel?): Page? {
            return if (parcel != null) {
                Page(parcel)
            } else {
                null
            }
        }

        override fun newArray(size: Int): Array<Page?> = arrayOfNulls(size)
    }

}
