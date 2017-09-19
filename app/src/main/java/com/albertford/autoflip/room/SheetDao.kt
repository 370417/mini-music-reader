package com.albertford.autoflip.room

import android.arch.persistence.room.Dao
import android.arch.persistence.room.Insert
import android.arch.persistence.room.Query
import io.reactivex.Flowable
import io.reactivex.Maybe

@Dao
interface SheetDao {

    @Query("SELECT * FROM sheet ORDER BY name")
    fun loadAllSheets(): Flowable<Array<Sheet>>

    @Query("SELECT * FROM sheet WHERE id = :id")
    fun loadSheet(id: Int): Maybe<Sheet>

    @Insert
    fun insertSheet(sheet: Sheet): Long
}
