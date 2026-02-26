package com.example.portside;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.portside.dictionary.DictionaryDao;
import com.example.portside.dictionary.Foreign;
import com.example.portside.dictionary.Native;

public class WordWrapper {
    private @Nullable Foreign foreignWord;
    private @Nullable Native nativeWord;

    public WordWrapper(@NonNull Foreign foreignWord) {
        this.foreignWord = foreignWord;
        this.nativeWord = null;
    }

    public WordWrapper(@NonNull Native nativeWord) {
        this.nativeWord = nativeWord;
        this.foreignWord = null;
    }

    public void attempt(DictionaryDao dao, boolean correct) {
        if (foreignWord != null) {
            this.foreignWord.attempts = createUpdatedAttempts(foreignWord.attempts, correct);
            this.foreignWord.modified = System.currentTimeMillis();
            dao.updateForeign(foreignWord);
        } else if (nativeWord != null) {
            this.nativeWord.attempts = createUpdatedAttempts(nativeWord.attempts, correct);
            this.nativeWord.modified = System.currentTimeMillis();
            dao.updateNative(nativeWord);
        }
    }

    private String createUpdatedAttempts(String attempts, boolean correct) {
        String nextAttempt = correct ? "1" : "0";
        return attempts.substring(1) + nextAttempt;
    }

    public double getSuccess() {
        if (foreignWord != null) {
            return getSuccess(foreignWord.attempts);
        } else {
            assert nativeWord != null;
            return getSuccess(nativeWord.attempts);
        }
    }

    private double getSuccess(String attempts) {
        double success = 0;
        double total = 0;
        for (char attempt : attempts.toCharArray()) {
            total++;
            if (attempt == '1') {
                success++;
            }
        }
        return success / total;
    }

    public double getDaysSinceModified() {
        Long modified = foreignWord != null? foreignWord.modified : nativeWord.modified;
        if (modified == null) {
            return 0;
        }
        return (System.currentTimeMillis() - modified) / (1000.0 * 60 * 60 * 24);
    }

    public long getModified() {
        Long modified = foreignWord != null? foreignWord.modified : nativeWord.modified;
        return modified != null ? modified : 0;
    }

    public boolean isForeign() {
        return foreignWord != null;
    }

    public String getWord() {
        if (foreignWord != null) {
            return foreignWord.word;
        } else {
            assert nativeWord != null;
            return nativeWord.word;
        }
    }
}
