package com.example.portside.dictionary;

public enum Gender {
    NEUTER, MASCULINE, FEMININE;

    public String getShortHand() {
        return this.name().toLowerCase().charAt(0) + ".";
    }
}
