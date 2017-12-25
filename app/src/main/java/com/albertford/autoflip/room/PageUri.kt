package com.albertford.autoflip.room

import android.arch.persistence.room.ColumnInfo
import android.arch.persistence.room.Entity
import android.arch.persistence.room.ForeignKey
import android.arch.persistence.room.PrimaryKey

@Entity(foreignKeys = [(ForeignKey(entity = Sheet::class, parentColumns = ["id"],
        childColumns = ["sheetId"], onDelete = ForeignKey.CASCADE))])
class PageUri (
        @PrimaryKey
        var uri: String,
        @ColumnInfo(index = true)
        var sheetId: Long,
        var pageNumber: Int
)
