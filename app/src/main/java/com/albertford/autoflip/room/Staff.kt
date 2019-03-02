package com.albertford.autoflip.room

import android.arch.persistence.room.*

@Entity(indices = [Index(value = ["sheetId", "pageIndex"])],
        foreignKeys = [ForeignKey(
                entity = Sheet::class,
                parentColumns = ["id"],
                childColumns = ["sheetId"],
                onDelete = ForeignKey.CASCADE
        )])
class Staff(
        var top: Float,
        var bottom: Float,
        val sheetId: Long,
        val pageIndex: Int
) : Comparable<Staff> {
    @PrimaryKey(autoGenerate = true)
    var id: Long = 0L

    @Ignore
    val barLines: MutableList<BarLine> = mutableListOf()

    override fun compareTo(other: Staff) = top.compareTo(other.top)

    fun barIndices(): IntRange {
        return 0..barLines.size - 2
    }

    override fun toString(): String {
        return "\n\ttop: $top, bottom: $bottom, bars: $barLines"
    }
}
