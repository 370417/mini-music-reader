package com.albertford.autoflip

import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup

class PlaceholderPageAdapter : RecyclerView.Adapter<PlaceholderPageAdapter.ViewHolder>() {

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {}

    override fun getItemCount() = 1

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val view = inflater.inflate(R.layout.page_tile, parent, false)
        view.layoutParams.width = parent.width - parent.paddingStart - parent.paddingEnd
        view.layoutParams.height = parent.height - parent.paddingTop - parent.paddingBottom
        view.requestLayout()
        return ViewHolder(view)
    }

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view)
}
