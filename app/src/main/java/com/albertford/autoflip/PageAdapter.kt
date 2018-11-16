package com.albertford.autoflip

import android.content.Context
import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.ViewGroup
import com.albertford.autoflip.views.EditPageView
import com.albertford.autoflip.views.LazyAdapter
import kotlinx.coroutines.*
import java.util.*
import kotlin.coroutines.CoroutineContext

class PageAdapter(
        private val uri: Uri,
        private val context: Context,
        private val callback: PageAdapterCallback,
        private val coroutineScope: CoroutineScope
) : RecyclerView.Adapter<PageViewHolder>(), LazyAdapter {
    private val sizes: Array<Size>
    private val pageCount: Int

    private var selectionPosition: Int = -1

    private var scrollState: Int = RecyclerView.SCROLL_STATE_IDLE

    private val unboundHolders: Deque<PageViewHolder> = ArrayDeque()

    init {
        val descriptor = context.contentResolver.openFileDescriptor(uri, "r")
        if (descriptor != null) {
            val renderer = PdfRenderer(descriptor)
            pageCount = renderer.pageCount
            sizes = Array(pageCount) { i ->
                renderer.openPage(i).use { page ->
                    Size(page.width, page.height)
                }
            }
        } else {
            sizes = arrayOf()
            pageCount = 0
        }
    }

    override fun onBindViewHolder(holder: PageViewHolder, position: Int) {
        holder.bindSize(sizes[position])
        if (scrollState == RecyclerView.SCROLL_STATE_IDLE) {
            holder.bindImage(uri, context, coroutineScope)
        } else {
            unboundHolders.push(holder)
        }

        holder.view.setOnClickListener { view ->
            if (holder.adapterPosition != selectionPosition) {
                selectionPosition = holder.adapterPosition
                callback.onSelectionChange(selectionPosition)
            }
        }
    }

    override fun getItemCount() = pageCount

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PageViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val view = inflater.inflate(R.layout.page_tile, parent, false)
        val imageWidth = parent.width - parent.paddingStart - parent.paddingEnd
        return PageViewHolder(view as EditPageView, imageWidth)
    }

    // deselect a viewholder if it is being recycled
    override fun onViewRecycled(holder: PageViewHolder) {
        if (holder.adapterPosition == selectionPosition) {
            selectionPosition = -1
            callback.onSelectionChange(selectionPosition)
        }
        holder.view.bitmap = null
        super.onViewRecycled(holder)
    }

    override fun onScrollStateChanged(state: Int) {
        scrollState = state
        if (state == RecyclerView.SCROLL_STATE_IDLE) {
            for (holder in unboundHolders) {
                holder.bindImage(uri, context, coroutineScope)
            }
            unboundHolders.clear()
        }
    }
}

class PageViewHolder(val view: EditPageView, private val width: Int) : RecyclerView.ViewHolder(view) {
    fun bindSize(size: Size) {
        view.layoutParams.width = width
        view.layoutParams.height = width * size.height / size.width
        view.requestLayout()
    }

    fun bindImage(uri: Uri, context: Context, coroutineScope: CoroutineScope) {
        view.post {
            val position = adapterPosition
            val width = view.width
            val height = view.height
            coroutineScope.launch(Dispatchers.Main) {
                val bitmap = withContext(Dispatchers.Default) {
                    renderPage(uri, context, position, width, height)
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

interface PageAdapterCallback {
    fun onSelectionChange(newSelectionPosition: Int)
}

class Size(val width: Int, val height: Int)
