package com.example.waktusolatmalaysia.repo;

import android.content.*;
import android.util.AtomicFile;
import com.example.waktusolatmalaysia.model.*;
import com.example.waktusolatmalaysia.preferences.*;
import com.example.waktusolatmalaysia.services.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import org.json.*;

public final class BackupStore {
  public static JSONObject export(Context c) throws Exception {
    SharedPreferences p = c.getSharedPreferences("waktu_solat", 0),
        s = c.getSharedPreferences("personal_library", 0);
    JSONObject preferences = new JSONObject();
    for (String key :
        new String[] {
          "zone_code", "zone_state", "zone_name", "twelve_hour", "dark", "theme", "language"
        }) if (p.contains(key)) preferences.put(key, p.getAll().get(key));
    for (String key : new String[] {"arabic_size", "show_translation", "quran_language"})
      if (s.contains(key)) preferences.put(key, s.getAll().get(key));
    JSONObject tracker = new JSONObject();
    for (Map.Entry<String, ?> item : s.getAll().entrySet())
      if (item.getKey().startsWith("prayed_")
          || item.getKey().startsWith("fasted_")
          || item.getKey().equals("tasbih_count")
          || item.getKey().equals("tasbih_goal")) tracker.put(item.getKey(), item.getValue());
    JSONArray reminders = new JSONArray();
    for (PrayerRelativeReminder task : new TaskStore(c).load()) reminders.put(task.json());
    return new JSONObject()
        .put("schemaVersion", 1)
        .put("exportedAt", java.time.Instant.now().toString())
        .put("preferences", preferences)
        .put("bookmarks", new JSONArray(new TreeSet<>(new UserStore(c).savedIds())))
        .put(
            "reading",
            new JSONObject()
                .put("surah", s.getInt("last_surah", 1))
                .put("verse", s.getInt("last_verse", 1))
                .put("name", s.getString("last_name", "Al-Fatihah")))
        .put("tracker", tracker)
        .put("reminders", reminders);
  }

  public static void validate(JSONObject data) throws Exception {
    if (data.getInt("schemaVersion") != 1)
      throw new IllegalArgumentException("Unsupported backup version");
    JSONArray saved = data.getJSONArray("bookmarks"), tasks = data.getJSONArray("reminders");
    if (saved.length() > 20000 || tasks.length() > 1000)
      throw new IllegalArgumentException("Backup too large");
    for (int i = 0; i < saved.length(); i++) {
      String id = saved.getString(i);
      if (!id.matches(
          "(surah:[1-9][0-9]{0,2}|ayah:[1-9][0-9]{0,2}:[1-9][0-9]{0,2}|(doa|niat|wirid|hadith):[0-9]+|catalog:[a-zA-Z0-9_-]+)"))
        throw new IllegalArgumentException("Invalid bookmark");
      if (id.startsWith("surah:") || id.startsWith("ayah:")) {
        String[] pieces = id.split(":");
        if (Integer.parseInt(pieces[1]) > 114
            || pieces.length > 2 && Integer.parseInt(pieces[2]) > 286)
          throw new IllegalArgumentException("Invalid bookmark range");
      }
    }
    for (int i = 0; i < tasks.length(); i++) PrayerRelativeReminder.parse(tasks.getJSONObject(i));
    JSONObject reading = data.getJSONObject("reading");
    if (reading.getInt("surah") < 1
        || reading.getInt("surah") > 114
        || reading.getInt("verse") < 1
        || reading.getInt("verse") > 286)
      throw new IllegalArgumentException("Invalid reading position");
    JSONObject pref = data.getJSONObject("preferences");
    for (String key : new String[] {"twelve_hour", "dark", "show_translation"})
      if (pref.has(key) && !(pref.get(key) instanceof Boolean))
        throw new IllegalArgumentException("Invalid toggle");
    for (String key :
        new String[] {
          "zone_code", "zone_state", "zone_name", "language", "theme", "quran_language"
        })
      if (pref.has(key)
          && (!(pref.get(key) instanceof String) || pref.getString(key).length() > 300))
        throw new IllegalArgumentException("Invalid text preference");
    if (pref.has("quran_language")
        && !Arrays.asList("ms", "en").contains(pref.getString("quran_language")))
      throw new IllegalArgumentException("Invalid Quran language");
    if (pref.has("zone_code") && !pref.getString("zone_code").matches("[A-Z]{3}[0-9]{2}"))
      throw new IllegalArgumentException("Invalid zone");
    if (pref.has("language") && !Arrays.asList("ms", "en").contains(pref.getString("language")))
      throw new IllegalArgumentException("Invalid locale");
    if (pref.has("theme")
        && !Arrays.asList("system", "dark", "light").contains(pref.getString("theme")))
      throw new IllegalArgumentException("Invalid theme");
    if (pref.has("arabic_size")
        && (pref.getInt("arabic_size") < 20 || pref.getInt("arabic_size") > 60))
      throw new IllegalArgumentException("Invalid text size");
    JSONObject tracker = data.getJSONObject("tracker");
    Iterator<String> keys = tracker.keys();
    while (keys.hasNext()) {
      String key = keys.next();
      Object value = tracker.get(key);
      if (key.startsWith("prayed_") || key.startsWith("fasted_")) {
        if (!(value instanceof Boolean)) throw new IllegalArgumentException("Invalid tracker");
      } else if (!(value instanceof Integer) || ((Integer) value) < 0)
        throw new IllegalArgumentException("Invalid counter");
      if (!(key.matches("(prayed_\\d{4}-\\d{2}-\\d{2}_.+|fasted_\\d{4}-\\d{2}-\\d{2})")
          || key.equals("tasbih_count")
          || key.equals("tasbih_goal")))
        throw new IllegalArgumentException("Unexpected private field");
      if (key.equals("tasbih_count") && tracker.getInt(key) < 0
          || key.equals("tasbih_goal") && (tracker.getInt(key) < 1 || tracker.getInt(key) > 10000))
        throw new IllegalArgumentException("Invalid counter");
    }
  }

  public static synchronized void apply(Context c, JSONObject data, boolean merge)
      throws Exception {
    validate(data);
    AtomicFile old = new AtomicFile(new File(c.getFilesDir(), "backup-before-import.json"));
    FileOutputStream out = old.startWrite();
    try {
      out.write(export(c).toString().getBytes(StandardCharsets.UTF_8));
      old.finishWrite(out);
    } catch (Exception e) {
      old.failWrite(out);
      throw e;
    }
    Map<String, PrayerRelativeReminder> tasks = new LinkedHashMap<>();
    if (merge) for (PrayerRelativeReminder t : new TaskStore(c).load()) tasks.put(t.id, t);
    JSONArray incoming = data.getJSONArray("reminders");
    for (int i = 0; i < incoming.length(); i++) {
      PrayerRelativeReminder t = PrayerRelativeReminder.parse(incoming.getJSONObject(i));
      tasks.put(
          t.id,
          new PrayerRelativeReminder(t.id, t.title, t.anchor, t.offsetMinutes, t.weekdays, false));
    }
    new TaskStore(c).replace(new ArrayList<>(tasks.values()));
    SharedPreferences personal = c.getSharedPreferences("personal_library", 0);
    SharedPreferences.Editor p = personal.edit();
    if (!merge) p.clear();
    Set<String> saved =
        merge
            ? new HashSet<>(personal.getStringSet("saved", Collections.emptySet()))
            : new HashSet<>();
    JSONArray a = data.getJSONArray("bookmarks");
    for (int i = 0; i < a.length(); i++) saved.add(a.getString(i));
    p.putStringSet("saved", saved);
    JSONObject tracker = data.getJSONObject("tracker");
    Iterator<String> keys = tracker.keys();
    while (keys.hasNext()) {
      String key = keys.next();
      Object value = tracker.get(key);
      if (merge && personal.contains(key)) continue;
      if (value instanceof Boolean) p.putBoolean(key, (Boolean) value);
      else if (value instanceof Number) p.putInt(key, ((Number) value).intValue());
    }
    JSONObject reading = data.getJSONObject("reading");
    if (!merge || !personal.contains("last_surah")) {
      p.putInt("last_surah", reading.getInt("surah"))
          .putInt("last_verse", reading.getInt("verse"))
          .putString("last_name", reading.optString("name", "Al-Quran"));
    }
    JSONObject pref = data.getJSONObject("preferences");
    for (String key : new String[] {"arabic_size", "show_translation", "quran_language"})
      if (pref.has(key) && (!merge || !personal.contains(key))) {
        if (key.equals("arabic_size")) p.putInt(key, pref.getInt(key));
        else if (key.equals("quran_language")) p.putString(key, pref.getString(key));
        else p.putBoolean(key, pref.getBoolean(key));
      }
    p.commit();
    SharedPreferences app = c.getSharedPreferences("waktu_solat", 0);
    SharedPreferences.Editor settings = app.edit();
    for (String key : new String[] {"zone_code", "zone_state", "zone_name", "language", "theme"})
      if (pref.has(key) && (!merge || !app.contains(key)))
        settings.putString(key, pref.getString(key));
    for (String key : new String[] {"twelve_hour", "dark"})
      if (pref.has(key) && (!merge || !app.contains(key)))
        settings.putBoolean(key, pref.getBoolean(key));
    settings.commit();
    if (!merge)
      for (int index : PrayerDay.OBLIGATORY) {
        personal
            .edit()
            .putString("prayer_mode_" + index, "OFF")
            .putBoolean("reminder_" + index, false)
            .apply();
      }
    if (!merge) personal.edit().putBoolean("azan_mode", false).apply();
    ReminderScheduler.reschedule(c);
    com.example.waktusolatmalaysia.widgets.PrayerWidgets.refreshAsync(c);
    com.example.waktusolatmalaysia.services.WidgetScheduleRefreshWorker.enqueueAll(c);
    ScheduleRefreshWorker.enqueue(c, true);
  }
}
