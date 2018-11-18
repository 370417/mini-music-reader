package com.albertford.autoflip.room

import android.arch.persistence.room.Embedded
import android.arch.persistence.room.Entity
import android.arch.persistence.room.Index
import android.arch.persistence.room.PrimaryKey
import android.os.Parcel
import android.os.Parcelable

@Entity(indices = [(Index("id"))])
class Sheet(
        var name: String,
        var uri: String,
        val pageCount: Int
) {
    @PrimaryKey(autoGenerate = true)
    var id: Long = 0L
}

class SheetAndFirstBar {
    @Embedded
    var sheet: Sheet? = null

    @Embedded
    var bar: Bar? = null
}
