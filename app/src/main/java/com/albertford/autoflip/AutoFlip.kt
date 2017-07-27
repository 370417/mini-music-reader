package com.albertford.autoflip

import android.app.Application
import io.realm.Realm

class AutoFlip : Application() {
    override fun onCreate() {
        super.onCreate()
        Realm.init(this)
        Realm.deleteRealm(Realm.getDefaultConfiguration())
    }
}