package com.albertford.autoflip

import android.content.Context
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.util.AttributeSet
import android.view.Menu
import android.view.View
import com.albertford.autoflip.R
import io.realm.Realm
import kotlinx.android.synthetic.main.activity_create_sheet.*

class CreateSheetActivity : AppCompatActivity() {

  lateinit var realm: Realm

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    realm = Realm.getDefaultInstance()
    setContentView(R.layout.activity_create_sheet)

    setSupportActionBar(toolbar)
    supportActionBar?.setDisplayHomeAsUpEnabled(true)
  }

  override fun onDestroy() {
    super.onDestroy()
    realm.close()
  }

  override fun onCreateOptionsMenu(menu: Menu?): Boolean {
    menuInflater.inflate(R.menu.menu_form, menu)
    return super.onCreateOptionsMenu(menu)
  }
}
