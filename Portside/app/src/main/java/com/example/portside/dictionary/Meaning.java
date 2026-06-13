package com.example.portside.dictionary;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;

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
    public int gender;
    public boolean enabled;

    public Meaning(
            @NonNull String foreignWord, @NonNull String nativeWord,
            @NonNull String part, int gender, boolean enabled
    ) {
        this.foreignWord = foreignWord;
        this.nativeWord = nativeWord;
        this.part = part;
        this.gender = gender;
        this.enabled = enabled;
    }

    public Gender getGender() {
        if (gender == 0) {
            return Gender.NEUTER;
        } else if (gender == 1) {
            return Gender.MASCULINE;
        } else {
            return Gender.FEMININE;
        }
    }
}
