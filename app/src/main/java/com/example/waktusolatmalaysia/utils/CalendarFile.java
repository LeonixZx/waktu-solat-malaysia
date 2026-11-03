package com.example.waktusolatmalaysia.utils;

import com.example.waktusolatmalaysia.model.*;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;

public final class CalendarFile {
  private static final DateTimeFormatter FORMAT =
      DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss'Z'").withZone(ZoneOffset.UTC);

  public static String prayers(Collection<PrayerDay> days, String zone, boolean english) {
    StringBuilder out =
        new StringBuilder(
            "BEGIN:VCALENDAR\r\n"
                + "VERSION:2.0\r\n"
                + "PRODID:-//Waktu Solat Malaysia//3.0//EN\r\n"
                + "CALSCALE:GREGORIAN\r\n");
    String[] names =
        english
            ? new String[] {"Imsak", "Fajr", "Sunrise", "Duha", "Dhuhr", "Asr", "Maghrib", "Isha"}
            : new String[] {
              "Imsak", "Subuh", "Syuruk", "Dhuha", "Zohor", "Asar", "Maghrib", "Isyak"
            };
    for (PrayerDay d : days)
      for (int i : PrayerDay.OBLIGATORY) {
        Instant start = d.date.atTime(d.times.get(i)).atZone(PrayerClock.MALAYSIA).toInstant();
        out.append("BEGIN:VEVENT\r\nUID:")
            .append(zone)
            .append('-')
            .append(d.date)
            .append('-')
            .append(PrayerDay.KEYS[i])
            .append("@waktusolatmalaysia\r\nDTSTAMP:")
            .append(FORMAT.format(Instant.now()))
            .append("\r\nDTSTART:")
            .append(FORMAT.format(start))
            .append("\r\nDTEND:")
            .append(FORMAT.format(start.plusSeconds(60)))
            .append("\r\nSUMMARY:")
            .append(names[i])
            .append("\r\nDESCRIPTION:JAKIM e-Solat ")
            .append(zone)
            .append("\r\nEND:VEVENT\r\n");
      }
    return out.append("END:VCALENDAR\r\n").toString();
  }
}
