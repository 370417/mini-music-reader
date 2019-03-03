package com.albertford.autoflip.room

import android.arch.persistence.room.Dao
import android.arch.persistence.room.Insert

@Dao @Deprecated("")
interface BarDao {

    @Insert
    fun insertBars(vararg bar: Bar)
}
