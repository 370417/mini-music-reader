package io.github.as_f.barpager.models;

import android.os.Parcel;
import android.os.Parcelable;
import io.realm.RealmObject;

public class BarLine extends RealmObject implements Parcelable {
    private float x = 0f;

    public BarLine() {}

    public BarLine(float x) {
        this.x = x;
    }

    private BarLine(Parcel in) {
        x = in.readFloat();
    }

    public static final Creator<BarLine> CREATOR = new Creator<BarLine>() {
        @Override public BarLine createFromParcel(Parcel in) {
            return new BarLine(in);
        }

        @Override public BarLine[] newArray(int size) {
            return new BarLine[size];
        }
    };

    public float getX() {
        return x;
    }

    public void setX(float x) {
        this.x = x;
    }

    @Override public int describeContents() {
        return 0;
    }

    @Override public void writeToParcel(Parcel parcel, int i) {
        parcel.writeFloat(x);
    }
}
