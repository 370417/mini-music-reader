package com.albertford.autoflip

import android.content.Intent
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.albertford.autoflip.activities.ViewSheetActivity
import com.albertford.autoflip.room.Sheet

private const val ITEM_EMPTY = 0
private const val ITEM_TEXT = 1

class SheetAdapter(val sheets: MutableList<Sheet>) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    constructor() : this(ArrayList())

    class TextViewHolder(val view: View) : RecyclerView.ViewHolder(view) {
        val primaryTextView: TextView? = view.findViewById(R.id.primary_list_text)
    }

    class EmptyViewHolder(view: View) : RecyclerView.ViewHolder(view)

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder?, position: Int) {
        if (holder is TextViewHolder) {
            val sheet = sheets[position]
            holder.primaryTextView?.text = sheet.name
            holder.view.setOnClickListener { view ->
                val intent = Intent(view.context, ViewSheetActivity::class.java)
                intent.putExtra("SHEET_ID", sheet.id)
                view.context.startActivity(intent)
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

    override fun onCreateViewHolder(parent: ViewGroup?, viewType: Int): RecyclerView.ViewHolder =
            when (viewType) {
                ITEM_EMPTY -> {
                    EmptyViewHolder(inflate(R.layout.quarter_rest_tile, parent))
                }
                else -> {
                    TextViewHolder(inflate(R.layout.sheet_list_tile, parent))
                }
            }
}

private fun inflate(id: Int, parent: ViewGroup?) = LayoutInflater.from(parent?.context).inflate(id, parent, false)
