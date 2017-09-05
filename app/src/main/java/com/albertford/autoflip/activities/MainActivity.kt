package com.albertford.autoflip.activities

import android.app.Activity
import android.content.DialogInterface
import android.content.Intent
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.support.v7.app.AlertDialog
import android.widget.Toast
import com.albertford.autoflip.R
import kotlinx.android.synthetic.main.activity_main.*

const val PICK_PDF_REQUEST = 1
const val PICK_IMAGE_REQUEST = 2
const val NO_REQUEST = -1

class MainActivity : AppCompatActivity() {

    private val dialogListener = DialogInterface.OnClickListener { _, i ->
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
        setContentView(R.layout.activity_main)

//        val results = readAllSheets(realm)
//        recycler_view.adapter = SheetAdapter(results)

        setSupportActionBar(toolbar)

//        if (results.isNotEmpty()) {
//            empty_image.visibility = View.GONE
//            empty_overlay.visibility = View.GONE
//        }

        floating_action_button.setOnClickListener {
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

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode != Activity.RESULT_OK || data?.data == null) {
            return
        }
        val intent = Intent(this, PartitionSheetActivity::class.java)
        val key = if (requestCode == PICK_PDF_REQUEST) "PDF" else "IMAGE"
        intent.putExtra(key, data.data.toString())
        startActivity(intent)
    }
}
