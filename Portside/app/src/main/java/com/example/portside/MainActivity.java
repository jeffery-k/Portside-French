package com.example.portside;

import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.room.Room;

import com.example.portside.dictionary.DictionaryDao;
import com.example.portside.dictionary.DictionaryDatabase;
import com.example.portside.dictionary.Foreign;
import com.example.portside.dictionary.Meaning;
import com.example.portside.dictionary.Native;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class MainActivity extends AppCompatActivity {
    private static final int START_POOL_SIZE = 10;
    private static final int WORDS_UNTIL_REORDER = 10;
    private static final double CONFIDENCE_COEFFICIENT = 1;
    private static final double CONFIDENCE_GROWTH_THRESHOLD = .5;

    private DictionaryDao dao;

    private View flipView;
    private TextView sideView;
    private TextView wordView;
    private TextView streakView;
    private TextView poolView;
    private Button correctButton;
    private Button wrongButton;

    private List<Foreign> allForeigns;
    private List<Native> allNatives;
    private List<Meaning> allMeanings;
    private Map<String, List<Meaning>> foreignMeanings;
    private Map<String, List<Meaning>> nativeMeanings;
    private List<Foreign> foreignReserves;
    private List<Native> nativeReserves;
    private List<WordWrapper> pool;
    private int poolSize;

    private boolean front = true;
    private int wordIndex = 0;
    private int streak = 0;
    private int wordsSinceReorder = 0;

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

        this.flipView = findViewById(R.id.flip);
        this.sideView = findViewById(R.id.side);
        this.wordView = findViewById(R.id.word);
        this.streakView = findViewById(R.id.streak);
        this.poolView = findViewById(R.id.pool);
        this.correctButton = findViewById(R.id.correct);
        this.wrongButton = findViewById(R.id.wrong);

        this.flipView.setOnTouchListener(
                (v, event) -> {
                    if (event.getAction() == MotionEvent.ACTION_UP) {
                        v.performClick();
                        this.flip();
                    }
                    return true;
                }
        );
        this.correctButton.setOnClickListener(
                (v) -> {
                    this.next(true);
                }
        );
        this.wrongButton.setOnClickListener(
                (v) -> {
                    this.next(false);
                }
        );

        this.reorder();
        this.setup();
    }

    private void setup() {
        this.front = true;
        while (getConfidence() >= CONFIDENCE_GROWTH_THRESHOLD) {
            this.growPool();
            Toast.makeText(this, "Growing Pool!", Toast.LENGTH_SHORT).show();
        }

        this.streakView.setText("Streak: " + streak);
        this.poolView.setText("Pool: " + pool.size());
        this.showFront();
    }

    private double getConfidence() {
        double totalConfidence = 0;
        for (WordWrapper word : pool) {
            totalConfidence += getConfidence(word);
        }
        return totalConfidence / pool.size();
    }

    private double getConfidence(WordWrapper word) {
        return (
                word.getSuccess() -
                        (word.getDaysSinceModified() * (CONFIDENCE_COEFFICIENT / pool.size()))
        );
    }

    private void flip() {
        this.front = !front;
        if (front) {
            this.showFront();
        } else {
            this.showBack();
        }
    }

    private void showFront() {
        this.sideView.setText("Front");
        WordWrapper word = pool.get(wordIndex);
        this.wordView.setText(
                word.getWord() + (word.isForeign() ? "\n(fr.)" : "\n(eng.)")
        );
        this.correctButton.setVisibility(View.GONE);
        this.wrongButton.setVisibility(View.GONE);
    }

    private void showBack() {
        this.sideView.setText("Back");
        WordWrapper word = pool.get(wordIndex);
        List<Meaning> meanings = word.isForeign() ?
                foreignMeanings.get(word.getWord()) : nativeMeanings.get(word.getWord());
        StringBuilder text = new StringBuilder();
        for (int i = 0; i < meanings.size(); i++) {
            Meaning meaning = meanings.get(i);
            if (i != 0) {
                text.append("\n\n");
            }
            text.append(meaning.part).append("\n");
            text.append(word.isForeign() ? meaning.nativeWord : meaning.foreignWord);
        }
        this.wordView.setText(text.toString());
        this.correctButton.setVisibility(View.VISIBLE);
        this.wrongButton.setVisibility(View.VISIBLE);
    }

    private void next(boolean correct) {
        if (correct) {
            this.streak++;
            if (streak != 0 && streak % 5 == 0) {
                Toast.makeText(
                        this,
                        Affirmations.AFFIRMATIONS[
                                new Random().nextInt(Affirmations.AFFIRMATIONS.length)
                                ],
                        Toast.LENGTH_LONG
                ).show();
            }
        } else {
            this.streak = 0;
        }

        this.pool.get(wordIndex).attempt(dao, correct);
        this.wordIndex = (wordIndex + 1) % this.pool.size();
        this.wordsSinceReorder++;
        if (wordsSinceReorder >= WORDS_UNTIL_REORDER) {
            this.reorder();
        }
        this.setup();
    }

    private void reorder() {
        this.pool.sort(
                (word1, word2) ->
                        (int) ((getConfidence(word1) - getConfidence(word2)) * 100)
        );
        this.wordIndex = 0;
        this.wordsSinceReorder = 0;
    }

    private void growPool() {
        Random random = new Random();
        int foreignIndex = random.nextInt(foreignReserves.size());
        Foreign foreignWord = foreignReserves.get(foreignIndex);
        foreignReserves.remove(foreignIndex);
        int nativeIndex = random.nextInt(nativeReserves.size());
        Native nativeWord = nativeReserves.get(nativeIndex);
        nativeReserves.remove(nativeIndex);

        this.pool.add(new WordWrapper(foreignWord));
        this.pool.add(new WordWrapper(nativeWord));
        this.reorder();
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