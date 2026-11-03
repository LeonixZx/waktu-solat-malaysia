package com.example.waktusolatmalaysia.ui;

import android.content.*;
import android.content.res.Configuration;
import android.view.*;
import android.widget.*;
import java.io.*;
import java.util.*;
import org.json.*;

public final class I18n {
  private static JSONObject words;
  private static final Map<String, String> memo = new HashMap<>();

  public static boolean english(Context c) {
    return c.getSharedPreferences("waktu_solat", 0).getString("language", "ms").equals("en");
  }

  public static String choose(Context c, String ms, String en) {
    return english(c) ? en : ms;
  }

  public static Context wrap(Context c) {
    Configuration cfg = new Configuration(c.getResources().getConfiguration());
    cfg.setLocale(Locale.forLanguageTag(english(c) ? "en" : "ms"));
    String theme = new com.example.waktusolatmalaysia.preferences.AppPreferences(c).theme();
    if (!theme.equals("system")) cfg.uiMode = (cfg.uiMode & ~Configuration.UI_MODE_NIGHT_MASK)
        | (theme.equals("dark") ? Configuration.UI_MODE_NIGHT_YES : Configuration.UI_MODE_NIGHT_NO);
    else cfg.uiMode=(cfg.uiMode & ~Configuration.UI_MODE_NIGHT_MASK)
        | (android.content.res.Resources.getSystem().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK);
    return c.createConfigurationContext(cfg);
  }

  /** Application resources must follow Android; only activities apply a manual night override. */
  public static Context wrapApplication(Context c) {
    Configuration localeOnly=new Configuration();localeOnly.fontScale=0;
    localeOnly.setLocale(Locale.forLanguageTag(english(c)?"en":"ms"));
    return c.createConfigurationContext(localeOnly);
  }

  public static void language(Context c, String lang) {
    c.getSharedPreferences("waktu_solat", 0)
        .edit()
        .putString("language", lang.equals("en") ? "en" : "ms")
        .commit();
  }

  public static String t(Context c, String value) {
    if (value == null || !english(c)) return value;
    try {
      if (words == null) {
        try (InputStream in = c.getAssets().open("ui_translations.json");
            ByteArrayOutputStream out = new ByteArrayOutputStream()) {
          byte[] b = new byte[4096];
          int n;
          while ((n = in.read(b)) != -1) out.write(b, 0, n);
          words = new JSONObject(out.toString("UTF-8"));
        }
      }
      if (words.has(value)) return words.getString(value);
      if (memo.containsKey(value)) return memo.get(value);
      String s = value;
      String[][] pairs = {
        {" ayat", " ayahs"},
        {"Sasaran ", "Goal "},
        {"pusingan lengkap", "completed rounds"},
        {"Saiz tulisan Arab ·", "Arabic text size ·"},
        {"Putar ", "Turn "},
        {"° ke kiri", "° to the left"},
        {"° ke kanan", "° to the right"},
        {"Bacaan terakhir:", "Last reading:"},
        {"Subuh esok", "Tomorrow’s Fajr"},
        {" lagi", " remaining"},
        {"Versi ", "Version "},
        {"AYAT ", "AYAH "},
        {"Tarikh penting ·", "Important dates ·"},
        {"Terjemahan Abdullah Muhammad Basmeih", "Abdullah Muhammad Basmeih translation"},
        {"Terjemahan Bahasa Indonesia", "Bahasa Indonesia translation"}
      };
      for (String[] p : pairs)
        if (s.startsWith(p[0])
            || s.matches("^\\d+.*")
            || value.startsWith("Putar ")
            || value.startsWith("Sasaran ")
            || value.contains(" · ")) s = s.replace(p[0], p[1]);
      if (memo.size() > 300) memo.clear();
      memo.put(value, s);
      return s;
    } catch (Exception e) {
      return value;
    }
  }

  public static String prayer(Context c, int i) {
    return english(c)
        ? new String[] {"Imsak", "Fajr", "Sunrise", "Duha", "Dhuhr", "Asr", "Maghrib", "Isha"}[i]
        : new String[] {"Imsak", "Subuh", "Syuruk", "Dhuha", "Zohor", "Asar", "Maghrib", "Isyak"}
            [i];
  }

  public static void localize(View root) {
    if (!english(root.getContext())) return;
    if (root instanceof TextView) {
      TextView text = (TextView) root;
      if (!text.isTextSelectable()) {
        String old = text.getText().toString(), next = t(root.getContext(), old);
        if (!next.equals(old)) text.setText(next);
      }
    }
    if (root instanceof ViewGroup) {
      ViewGroup group = (ViewGroup) root;
      for (int i = 0; i < group.getChildCount(); i++) localize(group.getChildAt(i));
    }
  }
}
