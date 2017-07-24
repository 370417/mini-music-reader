package com.albertford.autoflip

import android.content.Intent
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import com.albertford.autoflip.models.Sheet
import io.realm.Realm
import io.realm.RealmResults
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {
  lateinit var realm: Realm

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    realm = Realm.getDefaultInstance()
    setContentView(R.layout.activity_main)

    recycler_view.adapter = SheetAdapter(readFromRealm())

//    val intent = Intent(this, NewSheetActivity::class.java)
//    startActivity(intent)
  }

  override fun onDestroy() {
    super.onDestroy()
    realm.close()
  }

  private fun readFromRealm(): RealmResults<Sheet> {
    realm.beginTransaction()
    val results = realm.where(Sheet::class.java)
        .findAll()
    realm.commitTransaction()
    return results.sort("name")
  }
}
