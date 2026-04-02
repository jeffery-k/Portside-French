package com.example.portside;

import android.graphics.Color;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.room.Room;

import com.example.portside.dictionary.DictionaryDao;
import com.example.portside.dictionary.DictionaryDatabase;
import com.example.portside.dictionary.Foreign;
import com.example.portside.dictionary.Gender;
import com.example.portside.dictionary.Meaning;
import com.example.portside.dictionary.Native;
import com.google.android.material.chip.Chip;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.stream.Collectors;

public class MainActivity extends AppCompatActivity {
    private static final int START_POOL_SIZE = 8;
    private static final double REORDER_POWER_SCALE = 0.7;
    private static final double CONFIDENCE_DAILY_DECAY = 1;
    private static final double CONFIDENCE_GROWTH_THRESHOLD = .8;
    private static final double CONFIDENCE_RANDOMNESS = 1;
    private static final int INCORRECT_SHIFT_MINIMUM = 5;
    private static final int INCORRECT_SHIFT_MAXIMUM = 10;
    private static final int CORRECT_COLOR = Color.rgb(10, 120, 40);
    private static final int INCORRECT_COLOR = Color.rgb(120, 10, 40);

    private DictionaryDao dao;

    private View nextView;
    private TextView correctView;
    private TextView wordView;
    private TextView streakView;
    private TextView poolView;
    private TextView confidenceView;
    private ScrollView translationsView;
    private LinearLayout translationsLayout;
    private RadioGroup genderGroup;
    private RadioButton neuterButton;
    private EditText submissionText;
    private Chip editChip;

    private List<Foreign> allForeigns;
    private List<Native> allNatives;
    private List<Meaning> allMeanings;
    private Map<String, List<Meaning>> foreignMeanings;
    private Map<String, List<Meaning>> nativeMeanings;
    private List<Foreign> foreignReserves;
    private List<Native> nativeReserves;
    private List<WordWrapper> pool;
    private List<Meaning> matches;

    private int wordIndex = 0;
    private int streak = 0;
    private int wordsSinceReorder = 0;
    private long reorderCount = 0;
    private String history = "0000000000";
    private String wrongAnswer = null;

    private void init() {
        DictionaryDatabase db = Room.databaseBuilder(
                getApplicationContext(), DictionaryDatabase.class, "dictionary.db"
        ).createFromAsset("dictionary.db").allowMainThreadQueries().build();
        this.dao = db.dictionaryDao();

        this.allForeigns = dao.getAllForeigns();
        this.allNatives = dao.getAllNatives();
        this.allMeanings = dao.getAllMeanings();
        this.foreignMeanings = new HashMap<>();
        this.nativeMeanings = new HashMap<>();
        this.foreignReserves = new ArrayList<>();
        this.nativeReserves = new ArrayList<>();
        this.pool = new ArrayList<>();
        this.matches = new ArrayList<>();

        for (Meaning meaning : allMeanings) {
            if (!foreignMeanings.containsKey(meaning.foreignWord)) {
                foreignMeanings.put(meaning.foreignWord, new ArrayList<>());
            }
            foreignMeanings.get(meaning.foreignWord).add(meaning);
            if (!nativeMeanings.containsKey(meaning.nativeWord)) {
                nativeMeanings.put(meaning.nativeWord, new ArrayList<>());
            }
            nativeMeanings.get(meaning.nativeWord).add(meaning);
        }

        for (Foreign foreignWord : allForeigns) {
            if (foreignWord.modified != null) {
                this.pool.add(new WordWrapper(foreignWord));
            } else {
                this.foreignReserves.add(foreignWord);
            }
        }

        for (Native nativeWord : allNatives) {
            if (nativeWord.modified != null) {
                this.pool.add(new WordWrapper(nativeWord));
            } else {
                this.nativeReserves.add(nativeWord);
            }
        }

        while (pool.size() < START_POOL_SIZE) {
            this.growPool();
        }

        this.nextView = findViewById(R.id.next);
        this.correctView = findViewById(R.id.correct);
        this.wordView = findViewById(R.id.word);
        this.streakView = findViewById(R.id.streak);
        this.poolView = findViewById(R.id.pool);
        this.confidenceView = findViewById(R.id.confidence);
        this.translationsView = findViewById(R.id.translations);
        this.translationsLayout = findViewById(R.id.translations_text);
        this.genderGroup = findViewById(R.id.gender);
        this.neuterButton = findViewById(R.id.neuter);
        this.submissionText = findViewById(R.id.submission);
        this.editChip = findViewById(R.id.edit);

        this.submissionText.setOnEditorActionListener((v, actionId, event) -> {
            if (
                    actionId == EditorInfo.IME_ACTION_DONE &&
                            correctView.getVisibility() != View.VISIBLE
            ) {
                this.submit(v.getText().toString(), getSelectedGender());
                return true;
            }
            return false;
        });
        this.submissionText.setOnFocusChangeListener(
                (v, hasFocus) -> {
                    if (hasFocus) {
                        try {
                            Thread.sleep(100);
                        } catch (InterruptedException ignored) {
                        }
                        WindowInsetsControllerCompat controller =
                                WindowCompat.getInsetsController(getWindow(), submissionText);
                        controller.show(WindowInsetsCompat.Type.ime());
                    }
                }
        );

        this.nextView.setOnTouchListener(
                (v, event) -> {
                    if (
                            event.getAction() == MotionEvent.ACTION_UP &&
                                    correctView.getVisibility() == View.VISIBLE
                    ) {
                        v.performClick();
                        this.next(getWordMeanings().size() == matches.size());
                    }
                    return true;
                }
        );

        this.editChip.setOnCheckedChangeListener((buttonView, isChecked) -> {
            updateTranslationsLayout();
        });

        this.reorder();
        this.setup();
    }

    private void setup() {
        this.pool = this.pool.stream().filter(
                wordWrapper -> !getWordMeanings(wordWrapper).isEmpty()
        ).collect(Collectors.toList());
        while (getConfidence() >= CONFIDENCE_GROWTH_THRESHOLD) {
            if (!this.growPool()) {
                break;
            }
        }
        this.matches.clear();
        this.wrongAnswer = null;

        this.streakView.setText(String.format("Streak: %d", streak));
        this.poolView.setText(String.format("Pool: %d", pool.size()));
        this.confidenceView.setText(String.format(
                "Confidence: %d | %d | %d",
                (int) (getConfidence(this.pool.get(wordIndex), false) * 100),
                (int) (AttemptsHelper.getSuccess(history) * 100),
                (int) (getConfidence() * 100)
        ));

        this.editChip.setChecked(false);
        this.editChip.setVisibility(View.INVISIBLE);
        this.correctView.setVisibility(View.INVISIBLE);
        this.translationsView.setVisibility(View.INVISIBLE);
        this.translationsLayout.removeAllViews();
        this.genderGroup.setVisibility(View.VISIBLE);
        this.neuterButton.setChecked(true);
        this.submissionText.setVisibility(View.VISIBLE);
        this.submissionText.setText("");

        WordWrapper word = this.pool.get(wordIndex);
        String languageIndicator = word.isForeign() ? "(fr.)" : "(en.)";
        this.wordView.setText(String.format("%s\n%s", word.getWord(), languageIndicator));

        this.submissionText.requestFocus();
    }

    private void showBack() {
        this.editChip.setVisibility(View.VISIBLE);
        this.correctView.setVisibility(View.VISIBLE);
        this.translationsView.setVisibility(View.VISIBLE);
        this.genderGroup.setVisibility(View.INVISIBLE);
        this.submissionText.setVisibility(View.INVISIBLE);

        boolean correct = getWordMeanings().size() == matches.size();
        if (correct) {
            this.correctView.setText("Correct!");
            this.correctView.setBackgroundColor(CORRECT_COLOR);
        } else {
            this.correctView.setText("Wrong");
            this.correctView.setBackgroundColor(INCORRECT_COLOR);
        }
        updateTranslationsLayout();

        WindowInsetsControllerCompat controller =
                WindowCompat.getInsetsController(getWindow(), submissionText);
        controller.hide(WindowInsetsCompat.Type.ime());
    }

    private void updateTranslationsLayout() {
        this.translationsLayout.removeAllViews();

        if (wrongAnswer != null) {
            TextView wrongText = new TextView(this);
            wrongText.setText(String.format("> %s", wrongAnswer));
            wrongText.setTextSize(18);
            wrongText.setPadding(4, 4, 4, 12);
            this.translationsLayout.addView(wrongText);
        }

        WordWrapper word = pool.get(wordIndex);
        List<Meaning> meanings = getWordMeanings(!editChip.isChecked());
        for (Meaning meaning : meanings) {
            TextView meaningText = new TextView(this);
            String translation = word.isForeign() ? meaning.nativeWord : meaning.foreignWord;
            translation += " | " + meaning.part;
            meaningText.setText(translation);
            meaningText.setTextSize(18);
            meaningText.setPadding(4, 4, 12, 12);
            if (matches.contains(meaning)) {
                meaningText.setBackgroundColor(CORRECT_COLOR);
            } else {
                meaningText.setBackgroundColor(INCORRECT_COLOR);
            }
            if (!editChip.isChecked()) {
                this.translationsLayout.addView(meaningText);
            } else {
                HorizontalScrollView horizontalScrollView = new HorizontalScrollView(this);
                LinearLayout rowLayout = new LinearLayout(this);
                rowLayout.setOrientation(LinearLayout.HORIZONTAL);
                LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT);
                rowLayout.setLayoutParams(rowParams);
                rowLayout.addView(meaningText);
                Chip enableChip = new Chip(this);
                enableChip.setText("Enabled");
                enableChip.setCheckable(true);
                enableChip.setChecked(meaning.enabled);
                enableChip.setOnCheckedChangeListener(
                        (buttonView, isChecked) -> {
                            meaning.enabled = isChecked;
                            dao.updateMeaning(meaning);
                        }
                );
                TextView paddingText = new TextView(this);
                paddingText.setText("    ");
                rowLayout.addView(paddingText);
                rowLayout.addView(enableChip);
                horizontalScrollView.addView(rowLayout);
                this.translationsLayout.addView(horizontalScrollView);
            }
        }
    }

    private void next(boolean correct) {
        if (correct) {
            this.streak++;
        } else {
            this.streak = 0;
        }
        this.history = AttemptsHelper.createUpdatedAttempts(history, correct);

        this.pool.get(wordIndex).attempt(dao, correct);
        if (!correct) {
            int moveIndex = (
                    wordIndex +
                            (new Random()).nextInt(
                                    INCORRECT_SHIFT_MAXIMUM - INCORRECT_SHIFT_MINIMUM
                            ) +
                            INCORRECT_SHIFT_MINIMUM
            );
            moveIndex = Math.min(moveIndex, pool.size());
            this.pool.add(moveIndex, pool.get(wordIndex));
            this.pool.remove(wordIndex);
        } else {
            this.wordIndex = (wordIndex + 1) % this.pool.size();
        }

        this.wordsSinceReorder++;
        if (wordsSinceReorder > Math.pow(pool.size(), REORDER_POWER_SCALE)) {
            this.reorder();
        }
        this.setup();
    }

    private void submit(String submission, Gender gender) {
        List<Meaning> meanings = getWordMeanings();
        boolean isForeign = pool.get(wordIndex).isForeign();
        Meaning match = null;
        for (Meaning meaning : meanings) {
            String translation = isForeign ? meaning.nativeWord : meaning.foreignWord;
            if (
                    translation.trim().equalsIgnoreCase(submission.trim()) &&
                            meaning.getGender() == gender
            ) {
                match = meaning;
                break;
            }
        }

        if (match == null) {
            if (!submission.isEmpty()) {
                this.wrongAnswer = submission;
                this.wrongAnswer += gender == Gender.NEUTER ? "" : " | " + gender.name().toLowerCase();
            }
            this.showBack();
        } else {
            if (!matches.contains(match)) {
                this.matches.add(match);
            }

            if (matches.size() == meanings.size()) {
                this.showBack();
            } else {
                Toast.makeText(
                        this,
                        matches.size() + " / " + meanings.size(),
                        Toast.LENGTH_SHORT
                ).show();
            }
        }

        this.submissionText.setText("");
    }

    private void reorder() {
        this.pool.forEach(wordWrapper -> {
            wordWrapper.decay(dao, CONFIDENCE_DAILY_DECAY / this.pool.size());
        });
        this.pool.sort(
                (word1, word2) ->
                        (int) (
                                (
                                        getConfidence(word1, true) -
                                                getConfidence(word2, true)
                                ) * 100
                        )
        );
        this.wordIndex = 0;
        this.wordsSinceReorder = 0;
        this.reorderCount = (reorderCount + 1) % (Long.MAX_VALUE - 1);
    }

    private boolean growPool() {
        boolean grown = false;
        Random random = new Random();

        if (!foreignReserves.isEmpty()) {
            int foreignIndex = random.nextInt(foreignReserves.size());
            Foreign foreignWord = foreignReserves.get(foreignIndex);
            foreignReserves.remove(foreignIndex);
            WordWrapper newWord = new WordWrapper(foreignWord);
            this.pool.add(newWord);
            grown = true;

            List<Meaning> meanings = getWordMeanings(newWord);
            StringBuilder meaningsString = new StringBuilder();
            for (int i = 0; i < meanings.size(); i++) {
                Meaning meaning = meanings.get(i);
                meaningsString.
                        append(" ").
                        append(meaning.nativeWord).
                        append(" (").
                        append(meaning.getGender().getShortHand()).
                        append(i < meanings.size() - 1 ? ")," : ")");
            }

            Toast.makeText(
                    this,
                    foreignWord.word + " (fr.) -" + meaningsString,
                    Toast.LENGTH_LONG
            ).show();
        }

        if (!nativeReserves.isEmpty()) {
            int nativeIndex = random.nextInt(nativeReserves.size());
            Native nativeWord = nativeReserves.get(nativeIndex);
            nativeReserves.remove(nativeIndex);
            WordWrapper newWord = new WordWrapper(nativeWord);
            this.pool.add(newWord);
            grown = true;

            List<Meaning> meanings = getWordMeanings(newWord);
            StringBuilder meaningsString = new StringBuilder();
            for (int i = 0; i < meanings.size(); i++) {
                Meaning meaning = meanings.get(i);
                meaningsString.
                        append(" ").
                        append(meaning.foreignWord).
                        append(" (").
                        append(meaning.getGender().getShortHand()).
                        append(i < meanings.size() - 1 ? ")," : ")");
            }

            Toast.makeText(
                    this,
                    nativeWord.word + " (en.) -" + meaningsString,
                    Toast.LENGTH_LONG
            ).show();
        }

        if (grown) {
            this.reorder();
        }
        return grown;
    }

    private Gender getSelectedGender() {
        if (genderGroup.getCheckedRadioButtonId() == R.id.masculine) {
            return Gender.MASCULINE;
        } else if (genderGroup.getCheckedRadioButtonId() == R.id.feminine) {
            return Gender.FEMININE;
        } else {
            return Gender.NEUTER;
        }
    }

    private double getConfidence() {
        double totalConfidence = 0;
        double totalWeight = 0;
        for (WordWrapper word : pool) {
            double weight = getWordMeanings(word).size();
            totalConfidence += getConfidence(word, false) * weight;
            totalWeight += weight;
        }
        return totalConfidence / totalWeight;
    }

    private double getConfidence(WordWrapper word, boolean randomness) {
        double confidence = word.getSuccess();
        if (randomness) {
            long seed = word.getModified() + pool.size() + reorderCount;
            confidence += (new Random(seed)).nextGaussian() *
                    (CONFIDENCE_RANDOMNESS / ((AttemptsHelper.getSuccess(history) * 19) + 1));
        }
        return confidence;
    }

    private List<Meaning> getWordMeanings() {
        return getWordMeanings(true);
    }

    private List<Meaning> getWordMeanings(boolean filtered) {
        WordWrapper word = this.pool.get(wordIndex);
        return getWordMeanings(word, filtered);
    }

    private List<Meaning> getWordMeanings(@NonNull WordWrapper word) {
        return getWordMeanings(word, true);
    }

    private List<Meaning> getWordMeanings(@NonNull WordWrapper word, boolean filtered) {
        if (word.isForeign()) {
            return this.foreignMeanings.get(word.getWord()).stream().filter(
                    meaning -> !filtered || meaning.enabled
            ).collect(Collectors.toList());
        } else {
            return this.nativeMeanings.get(word.getWord()).stream().filter(
                    meaning -> !filtered || meaning.enabled
            ).collect(Collectors.toList());
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        this.init();
    }
}