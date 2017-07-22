package io.github.as_f.barpager;

import io.realm.RealmList;
import io.realm.RealmObject;
import io.realm.annotations.PrimaryKey;

public class Sheet extends RealmObject {
    @PrimaryKey
    private String uri = "";
    private String name = "";
    private float bpm = 0f;
    private int bpb = 0;
    private RealmList<Page> pages = new RealmList<>();

    public Sheet() {

    }

    public Sheet(String name, String uri, float bpm, int bpb) {
        this.uri = uri;
        this.name = name;
        this.bpm = bpm;
        this.bpb = bpb;
    }

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
}
