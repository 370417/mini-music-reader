package com.albertford.autoflip.room

import android.arch.persistence.room.Dao
import android.arch.persistence.room.Delete
import android.arch.persistence.room.Insert
import android.arch.persistence.room.Query
import io.reactivex.Single
import org.intellij.lang.annotations.Language

@Dao
interface UriDao {

//    @Query("SELECT * FROM pageuri WHERE sheetId = :id ORDER BY pagenumber")
//    fun loadUris(id: Long): Single<Array<PageUri>>

    @Language("RoomSql")
    @Query("SELECT * FROM pageuri")
    fun selectAllUris(): Single<Array<PageUri>>

    @Insert
    fun insertUris(vararg pageUri: PageUri)

    @Delete
    fun deleteUris(vararg pageUri: PageUri)
}
