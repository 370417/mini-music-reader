package com.albertford.autoflip.room

import android.arch.persistence.room.Entity
import android.arch.persistence.room.ForeignKey
import android.arch.persistence.room.Index
import android.arch.persistence.room.PrimaryKey

@Entity(indices = [Index(value = ["sheetId", "pageIndex", "staffIndex"])],
        foreignKeys = [ForeignKey(
                entity = Sheet::class,
                parentColumns = ["id"],
                childColumns = ["sheetId"],
                onDelete = ForeignKey.CASCADE
        )])
class BarLine(
        var x: Float,
        val sheetId: Long,
        val pageIndex: Int,
        val staffIndex: Int
) : Comparable<BarLine> {
    @PrimaryKey(autoGenerate = true)
    var id: Long = 0L

    override fun compareTo(other: BarLine) = x.compareTo(other.x)
}
