package com.albertford.autoflip.room;

import android.arch.persistence.room.Dao;
import android.arch.persistence.room.Insert;
import android.arch.persistence.room.Query;

@Dao
public interface BarDao {

    @Query("SELECT * FROM bar WHERE id = :id ORDER BY index")
    Bar[] loadBars(int id);

    @Insert
    void insertBars(Bar... bars);
}
