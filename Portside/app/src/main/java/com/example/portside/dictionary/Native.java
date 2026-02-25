package com.example.portside.dictionary;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity
public class Native {
    @PrimaryKey
    @NonNull
    public String word;
    @NonNull
    public String attempts;
    @Nullable
    public Long modified;

    public Native(@NonNull String word, @NonNull String attempts, @Nullable Long modified) {
        this.word = word;
        this.attempts = attempts;
        this.modified = modified;
    }
}
