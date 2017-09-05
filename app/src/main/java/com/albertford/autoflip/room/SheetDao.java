package com.albertford.autoflip.room;

import android.arch.persistence.room.Dao;
import android.arch.persistence.room.Insert;
import android.arch.persistence.room.Query;
import io.reactivex.Flowable;
import io.reactivex.Maybe;

@Dao
public interface SheetDao {

    @Query("SELECT * FROM sheet ORDER BY name")
    Flowable<Sheet[]> loadAllSheets();

    @Query("SELECT * FROM sheet WHERE id = :id")
    Maybe<Sheet> loadSheet(int id);

    @Insert
    long insertSheet(Sheet sheet);
}
