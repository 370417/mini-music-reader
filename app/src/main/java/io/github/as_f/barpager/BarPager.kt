package io.github.as_f.barpager

import android.app.Application
import io.realm.Realm

class BarPager : Application() {
  override fun onCreate() {
    super.onCreate()
    Realm.init(this)
  }
}