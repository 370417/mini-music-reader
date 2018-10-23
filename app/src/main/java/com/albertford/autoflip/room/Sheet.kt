package com.albertford.autoflip.room

import android.arch.persistence.room.Entity
import android.arch.persistence.room.Index
import android.arch.persistence.room.PrimaryKey
import android.os.Parcel
import android.os.Parcelable

@Entity(indices = [(Index("id"))])
class Sheet(
        @PrimaryKey(autoGenerate = true)
        var id: Long,
        var name: String,
        var uri: String/*,
        var inverseAspectRatio: Float*/
)
