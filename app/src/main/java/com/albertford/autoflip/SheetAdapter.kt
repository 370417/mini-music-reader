package com.albertford.autoflip

import android.content.Intent
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.albertford.autoflip.activities.ViewSheetActivity

class SheetAdapter() : RecyclerView.Adapter<SheetAdapter.ViewHolder>() {

    class ViewHolder(val view: View) : RecyclerView.ViewHolder(view) {
        val primaryTextView: TextView? = view.findViewById(R.id.primary_list_text)
    }

    override fun onBindViewHolder(holder: ViewHolder?, position: Int) {
//        val sheet = realmResults[position]
//        holder?.primaryTextView?.text = sheet.name
//        holder?.view?.setOnClickListener { view ->
//            val intent = Intent(view.context, ViewSheetActivity::class.java)
//            intent.putExtra(URI_KEY, sheet.uri)
//            view.context.startActivity(intent)
//        }
    }

    override fun getItemCount(): Int = 0//realmResults.size

    override fun onCreateViewHolder(parent: ViewGroup?, viewType: Int): ViewHolder {
        val tileView = LayoutInflater.from(parent?.context).inflate(R.layout.sheet_list_tile,
                parent, false)
        return ViewHolder(tileView)
    }
}
