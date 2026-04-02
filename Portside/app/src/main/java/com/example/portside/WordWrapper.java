package com.example.portside;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.portside.dictionary.DictionaryDao;
import com.example.portside.dictionary.Foreign;
import com.example.portside.dictionary.Native;

public class WordWrapper {
    private static final double MILLISECONDS_IN_DAYS = 1000.0 * 60 * 60 * 24;


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
        this.setAttempts(AttemptsHelper.createUpdatedAttempts(getAttempts(), correct));
        this.setModified(System.currentTimeMillis());
        this.update(dao);
    }

    public void update(DictionaryDao dao) {
        if (foreignWord != null) {
            dao.updateForeign(foreignWord);
        } else if (nativeWord != null) {
            dao.updateNative(nativeWord);
        }
    }

    public void decay(DictionaryDao dao, double rateOfDecay) {
        double currentSuccess = getSuccess();
        double maxDecay = rateOfDecay * getDaysSinceModified();
        int count;
        for (int i = 1; true; i++) {
            String decayedAttempts = AttemptsHelper.createDecayedAttempts(getAttempts(), i);
            double decaySuccess = AttemptsHelper.getSuccess(decayedAttempts);
            if ((currentSuccess - decaySuccess) > maxDecay) {
                count = i - 1;
                break;
            } else if (decaySuccess == 0) {
                count = i;
                break;
            }
        }

        if (count > 0) {
            this.setAttempts(AttemptsHelper.createDecayedAttempts(getAttempts(), count));
            long decayTime = (long) (
                    ((currentSuccess - getSuccess()) / rateOfDecay) * MILLISECONDS_IN_DAYS
            );
            this.setModified(this.getModified() + decayTime);
            this.update(dao);
        }
    }

    public double getSuccess() {
        return AttemptsHelper.getSuccess(getAttempts());
    }

    private String getAttempts() {
        return foreignWord != null ? foreignWord.attempts : nativeWord.attempts;
    }

    private void setAttempts(String attempts) {
        if (foreignWord != null) {
            foreignWord.attempts = attempts;
        } else if (nativeWord != null) {
            nativeWord.attempts = attempts;
        }
    }

    public double getDaysSinceModified() {
        Long modified = foreignWord != null ? foreignWord.modified : nativeWord.modified;
        if (modified == null) {
            return 0;
        }
        return (System.currentTimeMillis() - modified) / MILLISECONDS_IN_DAYS;
    }

    public long getModified() {
        Long modified = foreignWord != null ? foreignWord.modified : nativeWord.modified;
        return modified != null ? modified : 0;
    }

    private void setModified(long modified) {
        if (foreignWord != null) {
            foreignWord.modified = modified;
        } else if (nativeWord != null) {
            nativeWord.modified = modified;
        }
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
