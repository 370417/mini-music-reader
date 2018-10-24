package com.albertford.autoflip.room

import android.arch.persistence.room.Database
import android.arch.persistence.room.RoomDatabase

@Database(version = 2, entities = [(Sheet::class), (Bar::class)])
abstract class AppDatabase : RoomDatabase() {
    abstract fun sheetDao(): SheetDao
    abstract fun barDao(): BarDao
}
