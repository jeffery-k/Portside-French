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
    public boolean enabled;

    public Meaning(
            @NonNull String foreignWord, @NonNull String nativeWord,
            @NonNull String part, boolean enabled
    ) {
        this.foreignWord = foreignWord;
        this.nativeWord = nativeWord;
        this.part = part;
        this.enabled = enabled;
    }

    public Gender getGender() {
        boolean isMasculine = part.toLowerCase().matches(".*masculine.*");
        boolean isFeminine = part.toLowerCase().matches(".*feminine.*");
        if (isMasculine && !isFeminine) {
            return Gender.MASCULINE;
        } else if (isFeminine && !isMasculine) {
            return Gender.FEMININE;
        } else {
            return Gender.NEUTER;
        }
    }
}
