package io.github.as_f.barpager.models;

import android.os.Parcel;
import android.os.Parcelable;
import io.realm.RealmList;
import io.realm.RealmObject;
import io.realm.annotations.PrimaryKey;
import java.util.ArrayList;
import java.util.List;

public class Sheet extends RealmObject implements Parcelable {
    @PrimaryKey
    private String uri = "";
    private String name = "";
    private float bpm = 0f;
    private int bpb = 0;
    private RealmList<Page> pages = new RealmList<>();

    public Sheet() {}

    public Sheet(String name, String uri, float bpm, int bpb) {
        this.uri = uri;
        this.name = name;
        this.bpm = bpm;
        this.bpb = bpb;
    }

    private Sheet(Parcel in) {
        uri = in.readString();
        name = in.readString();
        bpm = in.readFloat();
        bpb = in.readInt();
        List<Page> javaPages = new ArrayList<>();
        in.readTypedList(javaPages, Page.CREATOR);
        pages.addAll(javaPages);
    }

    public static final Creator<Sheet> CREATOR = new Creator<Sheet>() {
        @Override public Sheet createFromParcel(Parcel in) {
            return new Sheet(in);
        }

        @Override public Sheet[] newArray(int size) {
            return new Sheet[size];
        }
    };

    public String getUri() {
        return uri;
    }

    public void setUri(String uri) {
        this.uri = uri;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public float getBpm() {
        return bpm;
    }

    public void setBpm(float bpm) {
        this.bpm = bpm;
    }

    public int getBpb() {
        return bpb;
    }

    public void setBpb(int bpb) {
        this.bpb = bpb;
    }

    public RealmList<Page> getPages() {
        return pages;
    }

    public void setPages(RealmList<Page> pages) {
        this.pages = pages;
    }

    @Override public int describeContents() {
        return 0;
    }

    @Override public void writeToParcel(Parcel parcel, int i) {
        parcel.writeString(uri);
        parcel.writeString(name);
        parcel.writeFloat(bpm);
        parcel.writeInt(bpb);
        parcel.writeTypedList(pages);
    }
}
