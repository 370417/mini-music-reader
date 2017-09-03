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
}
