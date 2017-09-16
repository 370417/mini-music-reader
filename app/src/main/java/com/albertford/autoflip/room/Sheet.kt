package com.albertford.autoflip.room

import android.arch.persistence.room.Entity
import android.arch.persistence.room.Index
import android.arch.persistence.room.PrimaryKey

@Entity(indices = arrayOf(Index("id")))
class Sheet(
        @PrimaryKey(autoGenerate = true)
        var id: Int,
        var name: String?
)
