package com.albertford.autoflip

import com.albertford.autoflip.models.Sheet
import io.realm.Realm
import io.realm.RealmResults

fun readAllSheets(realm: Realm): RealmResults<Sheet> {
    realm.beginTransaction()
    val results = realm.where(Sheet::class.java)
            .findAllSorted("name")
    realm.commitTransaction()
    return results
}
