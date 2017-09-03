package com.albertford.autoflip.room;

import android.arch.persistence.room.Dao;
import android.arch.persistence.room.Insert;
import android.arch.persistence.room.Query;

@Dao
public interface SheetDao {

    @Query("SELECT * FROM sheet ORDER BY name")
    Sheet[] loadAllSheets();

    @Query("SELECT * FROM sheet WHERE id = :id")
    Sheet loadSheet(int id);

    @Insert
    long insertSheet(Sheet sheet);
}
