package io.github.as_f.barpager

import android.content.Context
import android.database.sqlite.SQLiteOpenHelper
import android.database.sqlite.SQLiteDatabase

val VERSION = 1

class SheetsOpenHelper(context: Context) : SQLiteOpenHelper(context, "a", null, VERSION) {

  override fun onCreate(db: SQLiteDatabase?) {
    db?.execSQL("CREATE TABLE sheets (" +
        "name TEXT NOT NULL," +
        "src TEXT," +
        "thumbnail TEXT," +
        "period REAL NOT NULL)") // screens per minute
    db?.execSQL("CREATE TABLE screens (" +
        "")
  }

  override fun onUpgrade(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int) {
    TODO("not implemented")
  }

}