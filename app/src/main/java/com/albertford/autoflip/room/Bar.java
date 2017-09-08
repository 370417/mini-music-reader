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

    private float beatsPerMinute;

    private int beatsPerMeasure;

    private boolean leftBeginRepeat;

    private boolean rightEndRepeat;

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

    public float getBeatsPerMinute() {
        return beatsPerMinute;
    }

    public void setBeatsPerMinute(float beatsPerMinute) {
        this.beatsPerMinute = beatsPerMinute;
    }

    public int getBeatsPerMeasure() {
        return beatsPerMeasure;
    }

    public void setBeatsPerMeasure(int beatsPerMeasure) {
        this.beatsPerMeasure = beatsPerMeasure;
    }

    public boolean isLeftBeginRepeat() {
        return leftBeginRepeat;
    }

    public void setLeftBeginRepeat(boolean leftBeginRepeat) {
        this.leftBeginRepeat = leftBeginRepeat;
    }

    public boolean isRightEndRepeat() {
        return rightEndRepeat;
    }

    public void setRightEndRepeat(boolean rightEndRepeat) {
        this.rightEndRepeat = rightEndRepeat;
    }
}
