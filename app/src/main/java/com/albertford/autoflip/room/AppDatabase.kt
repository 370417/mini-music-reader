package com.albertford.autoflip.room

import android.arch.persistence.room.Database
import android.arch.persistence.room.RoomDatabase

@Database(version = 1, entities = [Sheet::class, Page::class, Staff::class, BarLine::class])
abstract class AppDatabase : RoomDatabase() {
    abstract fun sheetDao(): SheetDao
}
