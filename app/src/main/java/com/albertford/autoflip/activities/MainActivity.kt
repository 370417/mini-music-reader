package com.albertford.autoflip.activities

import android.app.Activity
import android.content.DialogInterface
import android.content.Intent
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.support.v7.app.AlertDialog
import android.view.View
import android.widget.Toast
import com.albertford.autoflip.R
import com.albertford.autoflip.SheetAdapter
import com.albertford.autoflip.models.Sheet
import com.albertford.autoflip.readAllSheets
import io.realm.Realm
import io.realm.RealmResults
import kotlinx.android.synthetic.main.activity_main.*

const val PICK_PDF_REQUEST = 0
const val PICK_IMAGE_REQUEST = 1

class MainActivity : AppCompatActivity() {
    private lateinit var realm: Realm

    private val dialogListener = DialogInterface.OnClickListener { dialog, i ->
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT)
        val requestCode: Int
        if (i == 0) {
            intent.type = "application/pdf"
            requestCode = PICK_PDF_REQUEST
        } else {
            intent.type = "image/*"
            requestCode = PICK_IMAGE_REQUEST
        }
        if (intent.resolveActivity(packageManager) != null) {
            startActivityForResult(intent, requestCode)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        realm = Realm.getDefaultInstance()
        setContentView(R.layout.activity_main)

        val results = readAllSheets(realm)
        recycler_view.adapter = SheetAdapter(results)

        setSupportActionBar(toolbar)

        if (results.isNotEmpty()) {
            empty_image.visibility = View.GONE
            empty_overlay.visibility = View.GONE
        }

        floating_action_button.setOnClickListener {
//            val intent = Intent(this, CreateSheetActivity::class.java)
//            startActivity(intent)
            val builder = AlertDialog.Builder(this)
            builder.setTitle(R.string.new_sheet_label)
                    .setItems(R.array.sheet_type, dialogListener)
                    .create()
                    .show()
        }

        floating_action_button.setOnLongClickListener {
            val toast = Toast.makeText(this, R.string.new_sheet_label, Toast.LENGTH_SHORT)
            toast.show()
            false
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        realm.close()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode != Activity.RESULT_OK || data?.data == null) {
            return
        }
        when (requestCode) {
            PICK_PDF_REQUEST -> {
                data.data
            }
            PICK_IMAGE_REQUEST -> {}
        }
    }
}
