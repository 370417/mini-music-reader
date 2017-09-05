package com.albertford.autoflip.room;

import android.arch.persistence.room.Dao;
import android.arch.persistence.room.Insert;
import android.arch.persistence.room.Query;
import io.reactivex.Single;

@Dao
public interface UriDao {

    @Query("SELECT * FROM pageuri WHERE sheetId = :id ORDER BY pagenumber")
    Single<PageUri[]> loadUris(int id);

    @Insert
    void insertUris(PageUri... pageUris);
}
