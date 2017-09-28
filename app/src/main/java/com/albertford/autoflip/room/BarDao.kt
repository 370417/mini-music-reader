package com.albertford.autoflip.room

import android.arch.persistence.room.Dao
import android.arch.persistence.room.Delete
import android.arch.persistence.room.Insert
import android.arch.persistence.room.Query
import io.reactivex.Single

@Dao
interface BarDao {

//    @Query("SELECT * FROM bar WHERE sheetId = :id ORDER BY barIndex")
//    fun loadBars(id: Long): Single<Array<Bar>>

    @Query("SELECT * FROM bar")
    fun selectAllBars(): Single<Array<Bar>>

    @Insert
    fun insertBars(vararg bar: Bar)

    @Delete
    fun deleteBars(vararg bar: Bar)
}
