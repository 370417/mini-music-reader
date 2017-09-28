package com.albertford.autoflip.room

import android.arch.persistence.room.Entity
import android.arch.persistence.room.ForeignKey

@Entity(primaryKeys = arrayOf("sheetId", "barIndex"),
        foreignKeys = arrayOf(ForeignKey(entity = Sheet::class, parentColumns = arrayOf("id"),
                childColumns = arrayOf("sheetId"))))
data class Bar(
        var sheetId: Long,
        var barIndex: Int,
        var pageIndex: Int,
        var top: Float,
        var left: Float,
        var width: Float,
        var height: Float,
        var beatsPerMinute: Float,
        var beatsPerMeasure: Int,
        var isLeftBeginRepeat: Boolean,
        var isRightEndRepeat: Boolean
)
