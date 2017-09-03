package com.albertford.autoflip.room;

import android.arch.persistence.room.Entity;
import android.arch.persistence.room.ForeignKey;
import android.arch.persistence.room.PrimaryKey;

@Entity(foreignKeys = @ForeignKey(
    entity = Sheet.class,
    parentColumns = "id",
    childColumns = "sheetId"))
public class PageUri {
    @PrimaryKey
    private String uri;

    private int sheetId;

    // For pdfs, this is always -1
    private int pageNumber;
}
