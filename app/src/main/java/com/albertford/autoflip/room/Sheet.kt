package com.albertford.autoflip.room

import android.arch.persistence.room.Entity
import android.arch.persistence.room.Index
import android.arch.persistence.room.PrimaryKey

const val PDF_SHEET = 1
const val IMG_SHEET = 2

@Entity(indices = arrayOf(Index("id")))
class Sheet(
        @PrimaryKey(autoGenerate = true)
        var id: Long,
        var name: String?,
        var type: Int
)
