package com.example.waktusolatmalaysia.model;

public final class Surah {
  public final int number, verses;
  public final String name, arabicName, translation, revelation;

  public Surah(
      int number,
      int verses,
      String name,
      String arabicName,
      String translation,
      String revelation) {
    this.number = number;
    this.verses = verses;
    this.name = name;
    this.arabicName = arabicName;
    this.translation = translation;
    this.revelation = revelation;
  }
}
