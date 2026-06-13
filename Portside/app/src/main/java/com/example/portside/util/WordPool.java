package com.example.portside.util;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

public class WordPool implements Iterable<WordWrapper> {
    private final List<WordWrapper> pool;
    private final Set<String> natives;
    private final Set<String> foreigns;

    public WordPool() {
        this.pool = new ArrayList<>();
        this.natives = new HashSet<>();
        this.foreigns = new HashSet<>();
    }

    public boolean containsNative(String word) {
        return this.natives.contains(word);
    }

    public boolean containsForeign(String word) {
        return this.foreigns.contains(word);
    }

    public boolean contains(WordWrapper wordWrapper) {
        return wordWrapper.isForeign() ?
                containsForeign(wordWrapper.getWord()) :
                containsNative(wordWrapper.getWord());
    }

    public WordWrapper get(int index) {
        return this.pool.get(index);
    }

    public void add(WordWrapper wordWrapper) {
        this.add(0, wordWrapper);
    }

    public void add(int index, WordWrapper wordWrapper) {
        Set<String> logSet = wordWrapper.isForeign() ? this.foreigns : this.natives;
        String word = wordWrapper.getWord();
        if (!logSet.contains(word)) {
            this.pool.add(index, wordWrapper);
            logSet.add(word);
        }
    }

    public void move(int index, int destination) {
        WordWrapper wordWrapper = this.pool.remove(index);
        this.pool.add(destination, wordWrapper);
    }

    public void remove(WordWrapper wordWrapper) {
        this.pool.remove(wordWrapper);
        (wordWrapper.isForeign() ? this.foreigns : this.natives).remove(wordWrapper.getWord());
    }

    public WordWrapper remove(int index) {
        WordWrapper wordWrapper = this.pool.remove(index);
        (wordWrapper.isForeign() ? this.foreigns : this.natives).remove(wordWrapper.getWord());
        return wordWrapper;
    }

    public int size() {
        return this.pool.size();
    }

    public boolean isEmpty() {
        return this.pool.isEmpty();
    }

    public void sort(@Nullable Comparator<? super WordWrapper> c) {
        this.pool.sort(c);
    }

    @Override
    public void forEach(@NonNull Consumer<? super WordWrapper> action) {
        this.pool.forEach(action);
    }

    @NonNull
    @Override
    public Iterator<WordWrapper> iterator() {
        return this.pool.iterator();
    }
}
