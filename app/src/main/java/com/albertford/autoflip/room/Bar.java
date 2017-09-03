package com.albertford.autoflip.room;

import android.arch.persistence.room.Entity;
import android.arch.persistence.room.ForeignKey;

@Entity(primaryKeys = {"sheetId", "index"}, foreignKeys = @ForeignKey(
    entity = Sheet.class,
    parentColumns = "id",
    childColumns = "sheetId"))
public class Bar {
    private int sheetId;

    private int index;

    private int pageNumber;

    private float top;

    private float left;

    private float width;

    private float height;
}
