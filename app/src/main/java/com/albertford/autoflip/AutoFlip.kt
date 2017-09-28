package com.albertford.autoflip

import android.app.Application
import android.arch.persistence.room.Room
import com.albertford.autoflip.room.AppDatabase

var database: AppDatabase? = null

class AutoFlip : Application() {
    override fun onCreate() {
        super.onCreate()
        database = Room.databaseBuilder(this, AppDatabase::class.java, "AppDatabase")
                .fallbackToDestructiveMigration()
                .build()
    }
}