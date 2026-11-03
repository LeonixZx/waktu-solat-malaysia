package com.example.waktusolatmalaysia.model;

import java.util.*;
import org.json.*;

public final class PrayerRelativeReminder {
  public final String id, title;
  public final int anchor, offsetMinutes;
  public final Set<Integer> weekdays;
  public final boolean enabled;

  public PrayerRelativeReminder(
      String id, String title, int anchor, int offset, Set<Integer> weekdays, boolean enabled) {
    if (id == null
        || !id.matches("[A-Za-z0-9_-]{1,80}")
        || title == null
        || title.trim().isEmpty()
        || title.trim().length() > 80
        || anchor < 0
        || anchor > 7
        || offset < -360
        || offset > 360
        || weekdays.isEmpty()
        || weekdays.stream().anyMatch(d -> d < 1 || d > 7))
      throw new IllegalArgumentException("Invalid prayer reminder");
    this.id = id;
    this.title = title.trim();
    this.anchor = anchor;
    this.offsetMinutes = offset;
    this.weekdays = Collections.unmodifiableSet(new HashSet<>(weekdays));
    this.enabled = enabled;
  }

  public JSONObject json() throws JSONException {
    return new JSONObject()
        .put("id", id)
        .put("title", title)
        .put("anchor", PrayerDay.KEYS[anchor])
        .put("offsetMinutes", offsetMinutes)
        .put("weekdays", new JSONArray(new TreeSet<>(weekdays)))
        .put("enabled", enabled);
  }

  public static PrayerRelativeReminder parse(JSONObject j) throws JSONException {
    Set<Integer> days = new HashSet<>();
    JSONArray a = j.getJSONArray("weekdays");
    for (int i = 0; i < a.length(); i++) days.add(a.getInt(i));
    return new PrayerRelativeReminder(
        j.getString("id"),
        j.getString("title"),
        Arrays.asList(PrayerDay.KEYS).indexOf(j.getString("anchor")),
        j.getInt("offsetMinutes"),
        days,
        j.optBoolean("enabled", false));
  }
}
