package com.example.waktusolatmalaysia.repo;

import com.example.waktusolatmalaysia.model.PrayerDay;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public final class PrayerParser {
  private static final DateTimeFormatter API_DATE =
      DateTimeFormatter.ofPattern("dd-MMM-uuuu", Locale.ENGLISH)
          .withResolverStyle(java.time.format.ResolverStyle.STRICT);

  private PrayerParser() {}

  public static Map<LocalDate, PrayerDay> parse(String json, YearMonth month, String zone)
      throws JSONException {
    JSONObject root = new JSONObject(json);
    if (!zone.equals(root.optString("zone", zone)))
      throw new JSONException("Unexpected prayer zone");
    JSONArray rows = root.getJSONArray("prayerTime");
    Map<LocalDate, PrayerDay> days = new LinkedHashMap<>();
    for (int i = 0; i < rows.length(); i++) {
      JSONObject row = rows.getJSONObject(i);
      LocalDate date = LocalDate.parse(row.getString("date"), API_DATE);
      if (!YearMonth.from(date).equals(month)) throw new JSONException("Unexpected prayer month");
      if (days.containsKey(date)) throw new JSONException("Duplicate prayer date");
      List<LocalTime> times = new ArrayList<>();
      for (String key : PrayerDay.KEYS) times.add(LocalTime.parse(row.getString(key)));
      for (int n = 1; n < times.size(); n++)
        if (!times.get(n).isAfter(times.get(n - 1)))
          throw new JSONException("Invalid prayer order");
      days.put(date, new PrayerDay(date, row.optString("hijri", ""), times));
    }
    if (days.isEmpty()) throw new JSONException("Empty prayer timetable");
    return days;
  }
}
