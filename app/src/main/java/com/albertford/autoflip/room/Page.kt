package com.albertford.autoflip.room

import android.arch.persistence.room.Entity
import android.arch.persistence.room.ForeignKey
import android.arch.persistence.room.Ignore
import com.albertford.autoflip.editsheetactivity.Selection

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

    fun getStaff(selection: Selection): Staff {
        return staves[selection.staffIndex]
    }

    override fun toString(): String {
        return "Page $pageIndex: $staves"
    }
}
