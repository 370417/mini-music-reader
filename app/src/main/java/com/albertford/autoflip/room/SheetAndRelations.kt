package com.albertford.autoflip.room

import android.arch.persistence.room.Embedded
import android.arch.persistence.room.Relation

class SheetAndRelations {
    @Embedded
    var sheet: Sheet? = null

    @Relation(parentColumn = "id", entityColumn = "sheetId", entity = Bar::class)
    var bars: List<Bar>? = null
}