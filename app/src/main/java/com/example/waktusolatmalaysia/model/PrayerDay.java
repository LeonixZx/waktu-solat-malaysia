package com.example.waktusolatmalaysia.model;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class PrayerDay {
  public static final String[] KEYS = {
    "imsak", "fajr", "syuruk", "dhuha", "dhuhr", "asr", "maghrib", "isha"
  };
  public static final int[] OBLIGATORY = {1, 4, 5, 6, 7};
  public final LocalDate date;
  public final String hijri;
  public final List<LocalTime> times;

  public PrayerDay(LocalDate date, String hijri, List<LocalTime> times) {
    if (times.size() != KEYS.length || times.contains(null)) {
      throw new IllegalArgumentException("Eight prayer times are required");
    }
    this.date = date;
    this.hijri = hijri;
    this.times = Collections.unmodifiableList(new ArrayList<>(times));
  }
}
