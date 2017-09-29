package com.albertford.autoflip.activities

import android.app.Activity
import android.content.DialogInterface
import android.content.Intent
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.support.v7.app.AlertDialog
import android.support.v7.widget.helper.ItemTouchHelper
import android.widget.Toast
import com.albertford.autoflip.*
import com.albertford.autoflip.room.Sheet
import io.reactivex.Completable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.activity_main.*

const val PICK_PDF_REQUEST = 1
const val PICK_IMAGE_REQUEST = 2

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

        setSupportActionBar(toolbar)

        val adapter = SheetAdapter()
        recycler_view.adapter = adapter
        val callback = ItemTouchHelperCallback(adapter)
        val touchHelper = ItemTouchHelper(callback)
        touchHelper.attachToRecyclerView(recycler_view)

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
        if (resultCode != Activity.RESULT_OK) {
            return
        }
        when (requestCode) {
            PICK_PDF_REQUEST -> {
                data ?: return
                val intent = Intent(this, PartitionSheetActivity::class.java)
                intent.putExtra("PDF", data.data.toString())
                startActivity(intent)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        database?.sheetDao()?.selectAllSheets()
                ?.subscribeOn(Schedulers.io())
                ?.observeOn(AndroidSchedulers.mainThread())
                ?.subscribe { sheets ->
                    val adapter = recycler_view.adapter
                    if (adapter is SheetAdapter) {
                        adapter.sheets.clear()
                        adapter.sheets.addAll(sheets)
                        adapter.notifyDataSetChanged()
                    }
                }
    }
}
