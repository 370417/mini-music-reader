package com.albertford.autoflip

import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.albertford.autoflip.models.Sheet
import io.realm.RealmResults

class SheetAdapter(val realmResults: RealmResults<Sheet>) : RecyclerView.Adapter<SheetAdapter.ViewHolder>() {

  class ViewHolder(val view: View) : RecyclerView.ViewHolder(view) {
      fun setPrimaryText(text: String) {
          val textView = view.findViewById(R.id.primary_list_text) as TextView
          textView.text = text
      }
  }

  override fun onBindViewHolder(holder: ViewHolder?, position: Int) {
    holder?.setPrimaryText(realmResults[position].name)
  }

  override fun getItemCount(): Int {
    return realmResults.size
  }

  override fun onCreateViewHolder(parent: ViewGroup?, viewType: Int): ViewHolder {
    val tileView = LayoutInflater.from(parent?.context).inflate(R.layout.sheet_list_tile, parent, false)
    return ViewHolder(tileView)
  }
}
