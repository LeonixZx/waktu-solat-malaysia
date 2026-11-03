package com.example.waktusolatmalaysia.preferences;

import android.content.Context;
import android.content.SharedPreferences;
import java.util.*;

public final class UserStore {
  private final SharedPreferences data;

  public UserStore(Context context) {
    data = context.getSharedPreferences("personal_library", Context.MODE_PRIVATE);
  }

  public String text(String key, String fallback) {
    return data.getString(key, fallback);
  }

  public int number(String key, int fallback) {
    return data.getInt(key, fallback);
  }

  public boolean flag(String key, boolean fallback) {
    return data.getBoolean(key, fallback);
  }

  public void set(String key, String value) {
    data.edit().putString(key, value).apply();
  }

  public void set(String key, int value) {
    data.edit().putInt(key, value).apply();
  }

  public void set(String key, boolean value) {
    data.edit().putBoolean(key, value).apply();
  }

  public Set<String> savedIds() {
    return new HashSet<>(data.getStringSet("saved", Collections.emptySet()));
  }

  public boolean saved(String id) {
    return data.getStringSet("saved", Collections.emptySet()).contains(id);
  }

  public boolean toggle(String id) {
    Set<String> saved = new HashSet<>(data.getStringSet("saved", Collections.emptySet()));
    boolean added = !saved.remove(id);
    if (added) saved.add(id);
    data.edit().putStringSet("saved", saved).apply();
    return added;
  }

  public void bookmark(int surah, int verse, String name) {
    data.edit()
        .putInt("last_surah", surah)
        .putInt("last_verse", verse)
        .putString("last_name", name)
        .apply();
  }
}
