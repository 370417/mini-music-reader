package com.albertford.autoflip

import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.albertford.autoflip.models.Sheet
import io.realm.RealmResults

class SheetAdapter(val realmResults: RealmResults<Sheet>) : RecyclerView.Adapter<SheetAdapter.ViewHolder>() {

  class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {}

  override fun onBindViewHolder(holder: ViewHolder?, position: Int) {
    TODO(
        "not implemented") //To change body of created functions use File | Settings | File Templates.
  }

  override fun getItemCount(): Int {
    return realmResults.size
  }

  override fun onCreateViewHolder(parent: ViewGroup?, viewType: Int): ViewHolder {
    val tileView = LayoutInflater.from(parent?.context).inflate(R.layout.sheet_list_tile, parent, false)
    return ViewHolder(tileView)
  }
}
