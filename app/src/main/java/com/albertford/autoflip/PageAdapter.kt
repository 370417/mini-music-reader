package com.albertford.autoflip

import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import android.support.v7.widget.RecyclerView
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ImageView
import com.albertford.autoflip.views.EditPageView
import com.albertford.autoflip.views.LazyAdapter
import java.util.*

class PageAdapter(descriptor: ParcelFileDescriptor, private val callback: PageAdapterCallback)
    : RecyclerView.Adapter<PageViewHolder>(), LazyAdapter {
    private val renderer: PdfRenderer = PdfRenderer(descriptor)
    private val sizes: Array<Size> = Array(renderer.pageCount) { i ->
        renderer.openPage(i).use { page ->
            Size(page.width, page.height)
        }
    }

    private var selectionPosition: Int = -1

    private var scrollState: Int = RecyclerView.SCROLL_STATE_IDLE

    private val unboundHolders: Deque<PageViewHolder> = ArrayDeque()

    override fun onBindViewHolder(holder: PageViewHolder, position: Int) {
        holder.bindSize(sizes[position])
        if (scrollState == RecyclerView.SCROLL_STATE_IDLE) {
            holder.bindImage(renderer)
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

    override fun getItemCount() = renderer.pageCount

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
                holder.bindImage(renderer)
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

    fun bindImage(renderer: PdfRenderer) {
        view.post {
            renderer.openPage(adapterPosition)?.use { page ->
                val bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
                page.render(bitmap, null, null,  PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                view.bitmap = bitmap
            }
        }
    }
}

interface PageAdapterCallback {
    fun onSelectionChange(newSelectionPosition: Int)
}

class Size(val width: Int, val height: Int)
