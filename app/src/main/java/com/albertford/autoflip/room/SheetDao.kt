package com.albertford.autoflip.room

import android.arch.persistence.room.*
import io.reactivex.Maybe
import io.reactivex.Single
import org.intellij.lang.annotations.Language

@Dao
interface SheetDao {

    @Language("RoomSql")
    @Query("SELECT * FROM sheet ORDER BY name")
    fun selectAllSheets(): Single<Array<Sheet>>

    @Language("RoomSql")
    @Transaction @Query("SELECT * FROM sheet WHERE id = :id")
    fun selectSheetById(id: Long): Maybe<SheetAndRelations>

    @Insert
    fun insertSheet(sheet: Sheet): Long

    @Update
    fun updateSheet(sheet: Sheet)

    @Delete
    fun deleteSheets(vararg sheet: Sheet)
}
