package com.albertford.autoflip.models;

import android.os.Parcel;
import android.os.Parcelable;
import io.realm.RealmList;
import io.realm.RealmObject;
import java.util.ArrayList;
import java.util.List;

public class Page extends RealmObject implements Parcelable {
    private RealmList<Staff> staves = new RealmList<>();

    public Page() {}

    private Page(Parcel in) {
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
        parcel.writeTypedList(staves);
    }
}
