package com.albertford.autoflip.models

import android.os.Parcel
import android.os.Parcelable
import com.albertford.autoflip.AutoFlip
import com.albertford.autoflip.room.Bar
import io.reactivex.Single

class Page(
        private val pageIndex: Int,
        private var scale: Float,
        var staffSelected: Boolean,
        var selectedBarIndex: Int,
        private val initBpm: Float,
        private val initBpb: Int
) : Parcelable {

    val staves: MutableList<Staff> = ArrayList()

    constructor(pageIndex: Int, scale: Float) : this(pageIndex, scale, false, -1, 60f, 4)

    constructor(pageIndex: Int, scale: Float, initBpm: Float, initBpb: Int) :
            this(pageIndex, scale, false, -1, initBpm, initBpb)

    constructor(parcel: Parcel) : this(
            parcel.readInt(),
            parcel.readFloat(),
            parcel.readInt() == 0,
            parcel.readInt(),
            parcel.readFloat(),
            parcel.readInt()
    ) {
        parcel.readTypedList(staves, Staff.CREATOR)
    }

    override fun describeContents() = 0

    override fun writeToParcel(parcel: Parcel?, int: Int) {
        parcel?.writeInt(pageIndex)
        parcel?.writeFloat(scale)
        parcel?.writeInt(if (staffSelected) 1 else 0)
        parcel?.writeInt(selectedBarIndex)
        parcel?.writeFloat(initBpm)
        parcel?.writeInt(initBpb)
        parcel?.writeTypedList(staves)
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

    fun toBarArray(sheetId: Int, pageScale: Int): Array<Bar> {
        val factor = pageScale / scale
        var currentBpm = initBpm
        var currentBpb = initBpb
        val bars: MutableList<Bar> = ArrayList()
        for (staff in staves) {
            for (barIndex in 0 until staff.barLines.size - 1) {
                val firstBarLine = staff.barLines[barIndex]
                val secondBarLine = staff.barLines[barIndex + 1]
                if (firstBarLine.bpb > 0) {
                    currentBpb = firstBarLine.bpb
                }
                if (firstBarLine.bpm > 0) {
                    currentBpm = firstBarLine.bpm
                }
                bars.add(Bar(
                        sheetId = sheetId,
                        barIndex = barIndex,
                        pageNumber = pageIndex,
                        top = staff.start * factor,
                        left = firstBarLine.x * factor,
                        width = (secondBarLine.x - firstBarLine.x) * factor,
                        height = (staff.end - staff.start) * factor,
                        beatsPerMinute = currentBpm,
                        beatsPerMeasure = currentBpb,
                        isLeftBeginRepeat = firstBarLine.beginRepeat,
                        isRightEndRepeat = secondBarLine.endRepeat
                ))
            }
        }
        return bars.toTypedArray()
    }

    fun write(sheetId: Int, pageScale: Int): Page {
        staves.sort()
        val bars: MutableList<Bar> = ArrayList()
        var totalBarIndex = 0
        val factor = pageScale / scale
        var currentBpm = initBpm
        var currentBpb = initBpb
        for (staff in staves) {
            for (barIndex in 0 until staff.barLines.size - 1) {
                val firstBarLine = staff.barLines[barIndex]
                val secondBarLine = staff.barLines[barIndex + 1]
                if (firstBarLine.bpb > 0) {
                    currentBpb = firstBarLine.bpb
                }
                if (firstBarLine.bpm > 0) {
                    currentBpm = firstBarLine.bpm
                }
                bars.add(Bar(
                        sheetId,
                        totalBarIndex,
                        pageIndex,
                        staff.start * factor,
                        firstBarLine.x * factor,
                        (secondBarLine.x - firstBarLine.x) * factor,
                        (staff.end - staff.start) * factor,
                        currentBpm,
                        currentBpb,
                        false,
                        false
                ))
                totalBarIndex += 1
            }
        }
        Single.fromCallable {
            val barArray = bars.toTypedArray()
            AutoFlip.database?.barDao()?.insertBars(*barArray)
        }
        return Page(pageIndex + 1, scale, currentBpm, currentBpb)
    }

    companion object CREATOR : Parcelable.Creator<Page> {
        override fun createFromParcel(parcel: Parcel?) = if (parcel != null) {
            Page(parcel)
        } else {
            Page(0, 1f)
        }

        override fun newArray(size: Int) = arrayOfNulls<Page>(size)
    }

}
