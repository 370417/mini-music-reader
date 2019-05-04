package com.albertford.autoflip.editsheetactivity.pagerecycler

import android.content.Context
import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.ViewGroup
import com.albertford.autoflip.R
import com.albertford.autoflip.editsheetactivity.EditPageObserver
import com.albertford.autoflip.room.Page
import com.albertford.autoflip.room.Sheet
import com.albertford.autoflip.editsheetactivity.EditPageView
import com.albertford.autoflip.editsheetactivity.EditSheetObserver
import kotlinx.coroutines.*

/**
 * Adapter for pages of a sheet.
 *
 * Unlike PlaceholderPageAdapter, this adapter requires a sheet & its pages to be initialized.
 */

class PageAdapter(
        val sheet: Sheet,
        val pages: Array<Page>,
        var editable: Boolean,
        private val context: Context,
        private val coroutineScope: CoroutineScope,
        private val editPageObserver: EditPageObserver,
        private val editSheetObservers: MutableSet<EditSheetObserver>
) : RecyclerView.Adapter<PageViewHolder>(), EditSheetObserver {

    init {
        // The PageAdapter needs to observe changes to the editability so that it knows if newly
        // bound viewholders should be editable or not
        editSheetObservers.add(this)
    }

    override fun onBindViewHolder(holder: PageViewHolder, position: Int) {
        editSheetObservers.add(holder.view)
        holder.bindSize(pages[position])
        holder.view.setPage(pages[position], editPageObserver)
        holder.view.onEditEnabledChanged(editable)
        holder.bindImage(Uri.parse(sheet.uri), context, coroutineScope)
    }

    override fun getItemCount() = pages.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PageViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val view = inflater.inflate(R.layout.page_tile, parent, false)
        val imageWidth = parent.width - parent.paddingStart - parent.paddingEnd
        return PageViewHolder(view as EditPageView, imageWidth)
    }

    override fun onViewRecycled(holder: PageViewHolder) {
        editSheetObservers.remove(holder.view)
        holder.view.bitmap = null
        super.onViewRecycled(holder)
    }

    override fun onEditEnabledChanged(editEnabled: Boolean) {
        editable = editEnabled
    }
}

class PageViewHolder(val view: EditPageView, private val width: Int) : RecyclerView.ViewHolder(view) {
    fun bindSize(page: Page) {
        view.layoutParams.width = width
        view.layoutParams.height = width * page.height / page.width
        view.requestLayout()
        view.bindWidth(width)
    }

    fun bindImage(uri: Uri, context: Context, coroutineScope: CoroutineScope) {
        view.post {
            val position = adapterPosition
            val width = view.width
            val height = view.height
            coroutineScope.launch {
                val bitmap = withContext(Dispatchers.Default) {
                    renderPage(uri, context,
                            position, width, height)
                }
                view.bitmap = bitmap
                view.invalidate()
            }
        }
    }
}

private fun renderPage(uri: Uri, context: Context, position: Int, viewWidth: Int, viewHeight: Int): Bitmap? {
    return context.contentResolver.openFileDescriptor(uri, "r")?.use { descriptor ->
        PdfRenderer(descriptor).openPage(position)?.use { page ->
            val bitmap = Bitmap.createBitmap(viewWidth, viewHeight, Bitmap.Config.ARGB_8888)
            page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
            bitmap
        }
    }
}

//class PageSize(val width: Int, val height: Int)
//
///**
// * Calculate the size of each page.
// * We do this in advance so that we can show properly sized placeholder rectangles while the images
// * load.
// */
//fun calcSizes(uri: Uri, context: Context): Array<PageSize>? {
//    return context.contentResolver.openFileDescriptor(uri, "r")?.use { descriptor ->
//        val renderer = PdfRenderer(descriptor)
//        Array(renderer.pageCount) { i ->
//            renderer.openPage(i).use { page ->
//                PageSize(page.width, page.height)
//            }
//        }
//    }
//}
