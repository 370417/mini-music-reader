package com.albertford.autoflip.room;

import android.arch.persistence.room.Database;
import android.arch.persistence.room.RoomDatabase;

@Database(version = 1, entities = {Sheet.class, PageUri.class, Bar.class})
public abstract class AppDatabase extends RoomDatabase {
    abstract public SheetDao sheetDao();
    abstract public UriDao uriDao();
    abstract public BarDao barDao();
}
