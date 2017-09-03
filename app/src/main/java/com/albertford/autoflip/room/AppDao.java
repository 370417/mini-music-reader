package com.albertford.autoflip.room;

import android.arch.persistence.room.Dao;
import android.arch.persistence.room.Insert;
import android.arch.persistence.room.Query;

@Dao
public interface AppDao {
    @Query("SELECT * FROM sheet ORDER BY name")
    Sheet[] loadAllSheets();

    @Query("SELECT * FROM sheet WHERE id = :id")
    Sheet loadSheet(int id);

    @Query("SELECT * FROM pageuri WHERE id = :id ORDER BY pagenumber")
    PageUri[] loadUris(int id);

    @Query("SELECT * FROM bar WHERE id = :id ORDER BY index")
    Bar[] loadBars(int id);

    @Insert
    long insertSheet(Sheet sheet);

    @Insert
    void insertUris(PageUri... pageUris);

    @Insert
    void insertBars(Bar... bars);
}
