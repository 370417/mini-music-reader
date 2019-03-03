package com.albertford.autoflip.mainactivity

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Matrix
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import com.albertford.autoflip.R
import com.albertford.autoflip.editsheetactivity.EditSheetActivity
import com.albertford.autoflip.room.Sheet
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val ITEM_EMPTY = 0
private const val ITEM_TEXT = 1

class SheetAdapter(val sheets: MutableList<Sheet>, val parent: Activity, private val coroutineScope: CoroutineScope) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    constructor(parent: Activity, coroutineScope: CoroutineScope) : this(ArrayList(), parent, coroutineScope)

    class TextViewHolder(val view: View) : RecyclerView.ViewHolder(view) {
        val primaryTextView: TextView? = view.findViewById(
                R.id.primary_list_text)
        val thumbnail: ImageView? = view.findViewById(R.id.thumbnail)
    }

    class EmptyViewHolder(view: View) : RecyclerView.ViewHolder(view)

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (holder is TextViewHolder) {
            val sheet = sheets[position]
            holder.primaryTextView?.text = sheet.name
            holder.view.setOnClickListener { view ->
                val intent = Intent(view.context, EditSheetActivity::class.java)
                intent.putExtra("SHEET", sheet)
                view.context.startActivity(intent)
            }
            val top = sheet.firstStaffTop
            val bottom = sheet.firstStaffBottom
            val pageIndex = sheet.firstStaffPageIndex
            if (top != null && bottom != null && pageIndex != null) {
                holder.view.requestLayout() // make sure width/height aren't 0
                coroutineScope.launch {
                    var bitmap: Bitmap? = null
                    withContext(Dispatchers.Default) {
                        bitmap = renderPage(Uri.parse(sheet.uri), parent, top, bottom, pageIndex, holder.view.width)
                    }
                    holder.thumbnail?.setImageBitmap(bitmap)
                }
            }
        }
    }

    override fun getItemCount(): Int = Math.max(sheets.size, 1)

    override fun getItemViewType(position: Int): Int {
        return if (sheets.isEmpty()) {
            ITEM_EMPTY
        } else {
            ITEM_TEXT
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            ITEM_EMPTY -> {
                EmptyViewHolder(inflate(R.layout.quarter_rest_tile, parent))
            }
            else -> {
                TextViewHolder(inflate(R.layout.sheet_list_tile, parent))
            }
        }
    }
}

private fun inflate(id: Int, parent: ViewGroup) = LayoutInflater.from(parent.context).inflate(id, parent, false)

private fun renderPage(uri: Uri, context: Context, top: Float, bottom: Float, pageIndex: Int, viewWidth: Int): Bitmap? {
    val descriptor = context.contentResolver.openFileDescriptor(uri, "r") ?: return null
    return PdfRenderer(descriptor).openPage(pageIndex)?.use { page ->
        val scale = viewWidth.toFloat() / page.width
        val height = (bottom - top) * viewWidth
        val bitmap = Bitmap.createBitmap(viewWidth, height.toInt(), Bitmap.Config.ARGB_8888)
        val matrix = Matrix()
        matrix.postTranslate(0f, -top * page.width)
        matrix.postScale(scale, scale)
        page.render(bitmap, null, matrix, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
        bitmap
    }
}
