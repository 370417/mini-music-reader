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

    public String getUri() {
        return uri;
    }

    public void setUri(String uri) {
        this.uri = uri;
    }

    public int getSheetId() {
        return sheetId;
    }

    public void setSheetId(int sheetId) {
        this.sheetId = sheetId;
    }

    public int getPageNumber() {
        return pageNumber;
    }

    public void setPageNumber(int pageNumber) {
        this.pageNumber = pageNumber;
    }
}
