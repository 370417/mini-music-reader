package io.github.as_f.barpager.models;

import android.os.Parcel;
import android.os.Parcelable;
import io.realm.RealmList;
import io.realm.RealmObject;
import java.util.ArrayList;
import java.util.List;

public class Page extends RealmObject implements Parcelable {
    private int width = 0;
    private int height = 0;
    private RealmList<Staff> staves = new RealmList<>();

    public Page() {}

    public Page(int width, int height) {
        this.width = width;
        this.height = height;
    }

    private Page(Parcel in) {
        width = in.readInt();
        height = in.readInt();
        List<Staff> javaStaves = new ArrayList<>();
        in.readTypedList(javaStaves, Staff.CREATOR);
        staves.addAll(javaStaves);
    }

    public static final Creator<Page> CREATOR = new Creator<Page>() {
        @Override public Page createFromParcel(Parcel in) {
            return new Page(in);
        }

        @Override public Page[] newArray(int size) {
            return new Page[size];
        }
    };

    public int getWidth() {
        return width;
    }

    public void setWidth(int width) {
        this.width = width;
    }

    public int getHeight() {
        return height;
    }

    public void setHeight(int height) {
        this.height = height;
    }

    public RealmList<Staff> getStaves() {
        return staves;
    }

    public void setStaves(RealmList<Staff> staves) {
        this.staves = staves;
    }

    @Override public int describeContents() {
        return 0;
    }

    @Override public void writeToParcel(Parcel parcel, int i) {
        parcel.writeInt(width);
        parcel.writeInt(height);
        parcel.writeTypedList(staves);
    }
}
