package com.example.waktusolatmalaysia.widgets;

/** Formats supplied timetable dates only. Never converts or estimates a Hijri date. */
public final class WidgetDates {
  private static final String[] MALAY = {
    "Muharam", "Safar", "Rabiul Awal", "Rabiul Akhir", "Jamadil Awal", "Jamadil Akhir",
    "Rejab", "Syaaban", "Ramadan", "Syawal", "Zulkaedah", "Zulhijah"
  };
  private static final String[] ENGLISH = {
    "Muharram", "Safar", "Rabi al-Awwal", "Rabi al-Thani", "Jumada al-Ula",
    "Jumada al-Akhirah", "Rajab", "Sha'ban", "Ramadan", "Shawwal",
    "Dhu al-Qidah", "Dhu al-Hijjah"
  };

  private WidgetDates() {}

  public static String hijri(String supplied, boolean english) {
    if (supplied == null || !supplied.matches("[0-9]{4}-[0-9]{2}-[0-9]{2}")) return "";
    int year = Integer.parseInt(supplied.substring(0, 4));
    int month = Integer.parseInt(supplied.substring(5, 7));
    int day = Integer.parseInt(supplied.substring(8, 10));
    if (year < 1 || month < 1 || month > 12 || day < 1 || day > 30) return "";
    return day + " " + (english ? ENGLISH : MALAY)[month - 1] + " " + year;
  }
}
