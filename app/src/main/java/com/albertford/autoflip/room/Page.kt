package com.albertford.autoflip.room

import android.arch.persistence.room.Entity
import android.arch.persistence.room.ForeignKey
import android.arch.persistence.room.Ignore

@Entity(primaryKeys = ["sheetId", "pageIndex"],
        foreignKeys = [ForeignKey(
                entity = Sheet::class,
                parentColumns = ["id"],
                childColumns = ["sheetId"],
                onDelete = ForeignKey.CASCADE
        )])
class Page(
        val width: Int,
        val height: Int,
        val sheetId: Long,
        val pageIndex: Int
) {
    @Ignore
    val staves: MutableList<Staff> = mutableListOf()
}
