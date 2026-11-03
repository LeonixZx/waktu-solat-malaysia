package com.example.waktusolatmalaysia.preferences;

import android.content.*;
import com.example.waktusolatmalaysia.widgets.PrayerWidgets;
import java.time.LocalDate;
import java.util.*;

/** Existing backup-compatible keys; labels never determine persistence identifiers. */
public final class PrayerTrackingStore {
  private static final Object LOCK = new Object();
  private static final Map<String, String> LEGACY;

  static {
    Map<String, String> m = new LinkedHashMap<>();
    m.put("fajr", "Subuh");
    m.put("dhuhr", "Zohor");
    m.put("asr", "Asar");
    m.put("maghrib", "Maghrib");
    m.put("isha", "Isyak");
    LEGACY = Collections.unmodifiableMap(m);
  }

  private final Context context;
  private final SharedPreferences data;

  public PrayerTrackingStore(Context c) {
    context = c.getApplicationContext();
    data = context.getSharedPreferences("personal_library", 0);
  }

  public static String key(LocalDate date, String prayer) {
    String old = LEGACY.get(prayer);
    if (old == null) throw new IllegalArgumentException("Not an obligatory prayer");
    return "prayed_" + date + "_" + old;
  }

  public Map<String, Boolean> read(LocalDate date) {
    synchronized (LOCK) {
      Map<String, Boolean> result = new LinkedHashMap<>();
      for (String p : LEGACY.keySet()) result.put(p, data.getBoolean(key(date, p), false));
      return Collections.unmodifiableMap(result);
    }
  }

  public boolean set(LocalDate date, String prayer, boolean completed) {
    boolean changed;
    synchronized (LOCK) {
      String key = key(date, prayer);
      if (data.getBoolean(key, false) == completed) return false;
      changed = data.edit().putBoolean(key, completed).commit();
    }
    if (changed) PrayerWidgets.refreshAsync(context);
    return changed;
  }
}
