package com.albertford.autoflip.room

import android.arch.persistence.room.Entity
import android.arch.persistence.room.ForeignKey
import android.arch.persistence.room.PrimaryKey

@Entity(foreignKeys = arrayOf(ForeignKey(entity = Sheet::class, parentColumns = arrayOf("id"),
        childColumns = arrayOf("sheetId"), onDelete = ForeignKey.CASCADE)))
class PageUri (
        @PrimaryKey
        var uri: String,
        var sheetId: Long,
        var pageNumber: Int
)
