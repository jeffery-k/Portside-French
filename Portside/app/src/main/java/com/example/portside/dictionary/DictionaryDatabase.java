package com.example.portside.dictionary;

import androidx.room.Database;
import androidx.room.RoomDatabase;

@Database(entities = {Foreign.class, Native.class, Meaning.class}, version = 1, exportSchema = false)
public abstract class DictionaryDatabase extends RoomDatabase {
    public abstract DictionaryDao dictionaryDao();
}
