package com.albertford.autoflip.room;

import android.arch.persistence.room.Dao;
import android.arch.persistence.room.Insert;
import android.arch.persistence.room.Query;
import io.reactivex.Single;

@Dao
public interface BarDao {

    @Query("SELECT * FROM bar WHERE sheetId = :id ORDER BY barIndex")
    Single<Bar[]> loadBars(int id);

    @Insert
    void insertBars(Bar... bars);
}
