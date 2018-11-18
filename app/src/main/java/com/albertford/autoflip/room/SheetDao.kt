package com.albertford.autoflip.room

import android.arch.persistence.room.*
import io.reactivex.Maybe
import io.reactivex.Single

@Dao
interface SheetDao {

    @Query("SELECT * FROM sheet LEFT JOIN bar ON sheet.id = bar.sheetId AND bar.barIndex = 1 ORDER BY name")
    fun selectAllSheetsWithThumb(): Single<Array<SheetAndFirstBar>>

    @Transaction @Query("SELECT * FROM sheet WHERE id = :id")
    fun selectSheetById(id: Long): Maybe<SheetAndRelations>

    @Insert
    fun insertSheet(sheet: Sheet): Long

    @Update
    fun updateSheet(sheet: Sheet)

    @Delete
    fun deleteSheets(vararg sheet: Sheet)

    //// ---- ////

    @Query("SELECT * FROM sheet WHERE id = :id LIMIT 1")
    fun findSheet(id: Long): Array<Sheet>

    @Query("SELECT * FROM page WHERE sheetId = :id ORDER BY pageIndex")
    fun findPagesBySheet(id: Long): Array<Page>

    @Query("SELECT * FROM staff WHERE sheetId = :id")
    fun findStavesBySheet(id: Long): Array<Staff>

    @Query("SELECT * FROM barline WHERE sheetId = :id")
    fun findBarLinesBySheet(id: Long): Array<BarLine>

    @Transaction
    fun findFullPagesBySheet(id: Long) : Array<Page>? {
        val pages = findPagesBySheet(id)
        val staves = findStavesBySheet(id)
        val barLines = findBarLinesBySheet(id)
        for (staff in staves) {
            val page = pages[staff.pageIndex]
            page.staves.add(staff)
        }
        for (page in pages) {
            page.staves.sort()
        }
        for (barLine in barLines) {
            val page = pages[barLine.pageIndex]
            val staff = page.staves[barLine.staffIndex]
            staff.barLines.add(barLine)
        }
        for (page in pages) {
            for (staff in page.staves) {
                staff.barLines.sort()
            }
        }
        return pages
    }

    @Insert
    fun insertPages(pages: Array<Page>)
}
