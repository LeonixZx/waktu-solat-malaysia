package com.example.waktusolatmalaysia.model;

public final class Reading {
  public final String id, title, arabic, meaning, reference, kind;
  public final java.util.List<Section> sections;
  public final boolean english;

  public Reading(String id, String title, String reference, String kind,
      java.util.List<Section> sections, boolean english) {
    this.id = id;
    this.title = title;
    this.sections = java.util.List.copyOf(sections);
    this.english = english;
    this.arabic = String.join("\n\n", sections.stream().map(s -> s.arabic).toArray(String[]::new));
    this.meaning = String.join("\n\n", sections.stream().map(s -> s.translation(english)).toArray(String[]::new));
    this.reference = reference;
    this.kind = kind;
  }

  public static final class Section {
    public final String id, title, arabic, transliteration, localTranslation, englishTranslation, localLanguage;
    public Section(String id, String title, String arabic, String transliteration,
        String localTranslation, String englishTranslation, String localLanguage) {
      this.id = id; this.title = title; this.arabic = arabic; this.transliteration = transliteration;
      this.localTranslation = localTranslation; this.englishTranslation = englishTranslation;
      this.localLanguage = localLanguage;
    }
    public String translation(boolean english) {
      return english && !englishTranslation.isEmpty() ? englishTranslation : localTranslation;
    }
    public String language(boolean english) {
      return english && !englishTranslation.isEmpty() ? "en" : localLanguage;
    }
  }
}
