package com.albertford.autoflip

import android.app.Application
import android.arch.persistence.room.Room
import com.albertford.autoflip.room.AppDatabase
import io.realm.Realm

class AutoFlip : Application() {
    companion object {
        var database: AppDatabase? = null
    }

    override fun onCreate() {
        super.onCreate()
        database = Room.databaseBuilder(this, AppDatabase::class.java, "AppDatabase").build()
//        Realm.init(this)
//        Realm.deleteRealm(Realm.getDefaultConfiguration())
    }
}