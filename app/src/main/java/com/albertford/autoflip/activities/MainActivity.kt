package com.albertford.autoflip.activities

import android.app.Activity
import android.content.Intent
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.support.v7.widget.StaggeredGridLayoutManager
import android.support.v7.widget.helper.ItemTouchHelper
import android.widget.Toast
import com.albertford.autoflip.*
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.activity_main.*

const val PDF_REQUEST = 1

class MainActivity : AppCompatActivity() {

    private val compositeDisposable = CompositeDisposable()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        setSupportActionBar(toolbar)

        val adapter = SheetAdapter(this)
        recycler_view.adapter = adapter
        val callback = ItemTouchHelperCallback(adapter)
        val touchHelper = ItemTouchHelper(callback)
        touchHelper.attachToRecyclerView(recycler_view)

        floating_action_button.setOnClickListener {
            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT)
            intent.type = "application/pdf"
            if (intent.resolveActivity(packageManager) != null) {
                startActivityForResult(intent, PDF_REQUEST)
            }
        }

        floating_action_button.setOnLongClickListener {
            val toast = Toast.makeText(this, R.string.new_sheet_label, Toast.LENGTH_SHORT)
            toast.show()
            false
        }
    }

    /** Dispose of asynchronous tasks */
    override fun onDestroy() {
        super.onDestroy()
        compositeDisposable.dispose()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode != Activity.RESULT_OK) {
            return
        }
        val uri = data?.data ?: return
        contentResolver.takePersistableUriPermission(uri, data.flags and Intent.FLAG_GRANT_READ_URI_PERMISSION)
        val intent = Intent(this, EditSheetActivity::class.java)
        intent.putExtra("URI", uri.toString())
        startActivity(intent)
    }

    override fun onResume() {
        super.onResume()
        val dispoable = database?.sheetDao()?.selectAllSheetsWithThumb()
                ?.subscribeOn(Schedulers.io())
                ?.observeOn(AndroidSchedulers.mainThread())
                ?.subscribe { sheetsWithThumb ->
                    val adapter = recycler_view.adapter
                    if (adapter is SheetAdapter) {
                        adapter.sheets.clear()
                        adapter.sheets.addAll(sheetsWithThumb)
                        adapter.notifyDataSetChanged()
                    }
                }
        if (dispoable != null) {
            compositeDisposable.add(dispoable)
        }
    }
}
