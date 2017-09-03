package com.albertford.autoflip.room;

import android.arch.persistence.room.Dao;
import android.arch.persistence.room.Insert;
import android.arch.persistence.room.Query;

@Dao
public interface UriDao {

    @Query("SELECT * FROM pageuri WHERE id = :id ORDER BY pagenumber")
    PageUri[] loadUris(int id);

    @Insert
    void insertUris(PageUri... pageUris);
}
