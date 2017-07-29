package com.albertford.autoflip.activities

import android.content.Intent
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import com.albertford.autoflip.R
import com.albertford.autoflip.SheetAdapter
import com.albertford.autoflip.models.Sheet
import io.realm.Realm
import io.realm.RealmResults
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {
    private lateinit var realm: Realm

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        realm = Realm.getDefaultInstance()
        setContentView(R.layout.activity_main)

        val results = readFromRealm()
        recycler_view.adapter = SheetAdapter(results)

        setSupportActionBar(toolbar)

        if (results.isNotEmpty()) {
            empty_image.visibility = View.GONE
            empty_overlay.visibility = View.GONE
        }

        floating_action_button.setOnClickListener {
            val intent = Intent(this, CreateSheetActivity::class.java)
            startActivity(intent)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        realm.close()
    }

    private fun readFromRealm(): RealmResults<Sheet> {
        realm.beginTransaction()
        val results = realm.where(Sheet::class.java)
                .findAllSorted("name")
        realm.commitTransaction()
        return results
    }
}
