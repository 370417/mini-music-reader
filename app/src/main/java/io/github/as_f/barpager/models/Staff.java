package io.github.as_f.barpager.models;

import android.os.Parcel;
import android.os.Parcelable;
import io.realm.RealmList;
import io.realm.RealmObject;
import java.util.ArrayList;
import java.util.List;

public class Staff extends RealmObject implements Parcelable {
    private float startY = 0f;
    private float endY = 0f;
    private RealmList<BarLine> barLines = new RealmList<>();

    public Staff() {}

    public Staff(float startY, float endY) {
        this.startY = startY;
        this.endY = endY;
    }

    private Staff(Parcel in) {
        startY = in.readFloat();
        endY = in.readFloat();
        List<BarLine> javaBarLines = new ArrayList<>();
        in.readTypedList(javaBarLines, BarLine.CREATOR);
        barLines.addAll(javaBarLines);
    }

    public static final Creator<Staff> CREATOR = new Creator<Staff>() {
        @Override public Staff createFromParcel(Parcel in) {
            return new Staff(in);
        }

        @Override public Staff[] newArray(int size) {
            return new Staff[size];
        }
    };

    public float getStartY() {
        return startY;
    }

    public void setStartY(float startY) {
        this.startY = startY;
    }

    public float getEndY() {
        return endY;
    }

    public void setEndY(float endY) {
        this.endY = endY;
    }

    public RealmList<BarLine> getBarLines() {
        return barLines;
    }

    public void setBarLines(RealmList<BarLine> barLines) {
        this.barLines = barLines;
    }

    @Override public int describeContents() {
        return 0;
    }

    @Override public void writeToParcel(Parcel parcel, int i) {
        parcel.writeFloat(startY);
        parcel.writeFloat(endY);
    }
}
