package com.albertford.autoflip.room

import android.arch.persistence.room.Dao
import android.arch.persistence.room.Insert
import android.arch.persistence.room.Query
import io.reactivex.Single

@Dao
interface UriDao {

    @Query("SELECT * FROM pageuri WHERE sheetId = :id ORDER BY pagenumber")
    fun loadUris(id: Int): Single<Array<PageUri>>

    @Insert
    fun insertUris(vararg pageUris: PageUri)
}
