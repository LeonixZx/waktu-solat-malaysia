package com.example.waktusolatmalaysia.model;

public final class QuranVerse {
  public final int number, globalNumber;
  public final String arabic, meaning, audio;

  public QuranVerse(int number, int globalNumber, String arabic, String meaning, String audio) {
    this.number = number;
    this.globalNumber = globalNumber;
    this.arabic = arabic;
    this.meaning = meaning;
    this.audio = audio;
  }
}
