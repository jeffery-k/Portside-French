package com.example.portside.dictionary;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(primaryKeys = {"foreign", "native"})
public class Meaning {
    @ColumnInfo(name = "foreign")
    @NonNull
    public String foreignWord;
    @ColumnInfo(name = "native")
    @NonNull
    public String nativeWord;
    @NonNull
    public String part;

    public Meaning(@NonNull String foreignWord, @NonNull String nativeWord, @NonNull String part) {
        this.foreignWord = foreignWord;
        this.nativeWord = nativeWord;
        this.part = part;
    }
}
