package com.albertford.autoflip.room;

import android.arch.persistence.room.Entity;
import android.arch.persistence.room.PrimaryKey;

@Entity
public class Sheet {
    @PrimaryKey(autoGenerate = true)
    private int id;

    private String name;

    private int bpm;

    private int bpb;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getBpm() {
        return bpm;
    }

    public void setBpm(int bpm) {
        this.bpm = bpm;
    }

    public int getBpb() {
        return bpb;
    }

    public void setBpb(int bpb) {
        this.bpb = bpb;
    }
}
