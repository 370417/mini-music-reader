package com.albertford.autoflip

import android.content.Intent
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.albertford.autoflip.activities.URI_KEY
import com.albertford.autoflip.activities.ViewSheetActivity
import com.albertford.autoflip.models.Sheet
import io.realm.RealmResults

class SheetAdapter(
        private val realmResults: RealmResults<Sheet>) : RecyclerView.Adapter<SheetAdapter.ViewHolder>() {

    class ViewHolder(val view: View) : RecyclerView.ViewHolder(view) {
        val primaryTextView = view.findViewById(R.id.primary_list_text) as TextView
    }

    override fun onBindViewHolder(holder: ViewHolder?, position: Int) {
        val sheet = realmResults[position]
        holder?.primaryTextView?.text = sheet.name
        holder?.view?.setOnClickListener { view ->
            val intent = Intent(view.context, ViewSheetActivity::class.java)
            intent.putExtra(URI_KEY, sheet.uri)
            view.context.startActivity(intent)
        }
    }

    override fun getItemCount(): Int = realmResults.size

    override fun onCreateViewHolder(parent: ViewGroup?, viewType: Int): ViewHolder {
        val tileView = LayoutInflater.from(parent?.context).inflate(R.layout.sheet_list_tile,
                parent, false)
        return ViewHolder(tileView)
    }
}
