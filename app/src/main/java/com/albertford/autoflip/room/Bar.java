package com.albertford.autoflip.room;

import android.arch.persistence.room.Entity;
import android.arch.persistence.room.ForeignKey;

@Entity(primaryKeys = {"sheetId", "barIndex"}, foreignKeys = @ForeignKey(
    entity = Sheet.class,
    parentColumns = "id",
    childColumns = "sheetId"))
public class Bar {
    private int sheetId;

    private int barIndex;

    private int pageNumber;

    private float top;

    private float left;

    private float width;

    private float height;

    public int getSheetId() {
        return sheetId;
    }

    public void setSheetId(int sheetId) {
        this.sheetId = sheetId;
    }

    public int getBarIndex() {
        return barIndex;
    }

    public void setBarIndex(int barIndex) {
        this.barIndex = barIndex;
    }

    public int getPageNumber() {
        return pageNumber;
    }

    public void setPageNumber(int pageNumber) {
        this.pageNumber = pageNumber;
    }

    public float getTop() {
        return top;
    }

    public void setTop(float top) {
        this.top = top;
    }

    public float getLeft() {
        return left;
    }

    public void setLeft(float left) {
        this.left = left;
    }

    public float getWidth() {
        return width;
    }

    public void setWidth(float width) {
        this.width = width;
    }

    public float getHeight() {
        return height;
    }

    public void setHeight(float height) {
        this.height = height;
    }
}
