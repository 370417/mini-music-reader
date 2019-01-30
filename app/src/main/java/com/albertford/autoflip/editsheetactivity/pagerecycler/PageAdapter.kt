package com.albertford.autoflip.editsheetactivity.pagerecycler

import android.content.Context
import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.ViewGroup
import com.albertford.autoflip.R
import com.albertford.autoflip.editsheetactivity.EditPageListener
import com.albertford.autoflip.room.Page
import com.albertford.autoflip.room.Sheet
import com.albertford.autoflip.editsheetactivity.EditPageView
import kotlinx.coroutines.*

class PageAdapter(
        private val sheet: Sheet,
        private val pages: Array<Page>,
        private val uri: Uri,
        private val context: Context,
        private val coroutineScope: CoroutineScope,
        private val editPageListener: EditPageListener
) : RecyclerView.Adapter<PageViewHolder>(), EditPageListener {

    private var selectedPageIndex: Int = -1

    override fun onBindViewHolder(holder: PageViewHolder, position: Int) {
        holder.view.page = pages[position]
        holder.view.listener = editPageListener
        holder.bindSize(pages[position])
        holder.bindImage(uri, context, coroutineScope)
    }

    override fun getItemCount() = pages.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PageViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val view = inflater.inflate(R.layout.page_tile, parent, false)
        val imageWidth = parent.width - parent.paddingStart - parent.paddingEnd
        return PageViewHolder(view as EditPageView, imageWidth)
    }

    override fun onViewRecycled(holder: PageViewHolder) {
        holder.view.bitmap = null
        super.onViewRecycled(holder)
    }

    override fun cancelSelection() {
        selectedPageIndex = -1
        editPageListener.cancelSelection()
    }

    override fun changeSelection() {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun confirmSelection() {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun initalSelection(pageIndex: Int) {
        selectedPageIndex = pageIndex
        editPageListener.initalSelection(pageIndex)
    }
}

class PageViewHolder(val view: EditPageView, private val width: Int) : RecyclerView.ViewHolder(view) {
    fun bindSize(page: Page) {
        view.layoutParams.width = width
        view.layoutParams.height = width * page.height / page.width
        view.requestLayout()
    }

    fun bindImage(uri: Uri, context: Context, coroutineScope: CoroutineScope) {
        view.post {
            val position = adapterPosition
            val width = view.width
            val height = view.height
            coroutineScope.launch(Dispatchers.Main) {
                val bitmap = withContext(Dispatchers.Default) {
                    renderPage(uri, context,
                            position, width, height)
                }
                view.bitmap = bitmap
            }
        }
    }
}

fun renderPage(uri: Uri, context: Context, position: Int, width: Int, height: Int): Bitmap? {
    val descriptor = context.contentResolver.openFileDescriptor(uri, "r") ?: return null
    return PdfRenderer(descriptor).openPage(position)?.use { page ->
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
        bitmap
    }
}

class Size(val width: Int, val height: Int)

fun calcSizes(uri: Uri, context: Context): Array<Size>? {
    val descriptor = context.contentResolver.openFileDescriptor(uri, "r") ?: return null
    val renderer = PdfRenderer(descriptor)
    return Array(renderer.pageCount) { i ->
        renderer.openPage(i).use { page ->
            Size(page.width, page.height)
        }
    }
}
