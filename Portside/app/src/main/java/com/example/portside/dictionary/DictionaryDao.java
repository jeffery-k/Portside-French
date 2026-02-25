package com.example.portside.dictionary;

import androidx.room.Dao;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

@Dao
public interface DictionaryDao {
    @Query("SELECT * FROM `Foreign`")
    List<Foreign> getAllForeigns();

    @Query("SELECT * FROM `Native`")
    List<Native> getAllNatives();

    @Query("SELECT * FROM `Meaning`")
    List<Meaning> getAllMeanings();

    @Update
    void updateForeign(Foreign foreigns);

    @Update
    void updateNative(Native natives);
}
