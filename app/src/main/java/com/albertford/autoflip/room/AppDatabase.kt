package com.albertford.autoflip.room

import android.arch.persistence.room.Database
import android.arch.persistence.room.RoomDatabase

@Database(version = 2, entities = [(Sheet::class), (PageUri::class), (Bar::class)])
abstract class AppDatabase : RoomDatabase() {
    abstract fun sheetDao(): SheetDao
    abstract fun uriDao(): UriDao
    abstract fun barDao(): BarDao
}
