package com.example.waktusolatmalaysia;

import static org.junit.Assert.*;

import android.appwidget.*;
import android.content.*;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import org.json.*;
import org.junit.*;
import org.junit.runner.RunWith;

/** Run before under 3.1.1 then after under 3.2.0; no references to new production APIs. */
@RunWith(AndroidJUnit4.class)
public class WidgetUpgradeV32Test {
  @Test
  public void persistentHostIdsSurvivePackageReplacement() throws Exception {
    String stage = InstrumentationRegistry.getArguments().getString("upgrade", "");
    org.junit.Assume.assumeTrue(stage.equals("before") || stage.equals("after"));
    Context c = InstrumentationRegistry.getInstrumentation().getTargetContext();
    AppWidgetManager manager = AppWidgetManager.getInstance(c);
    AppWidgetHost host = new AppWidgetHost(c, 32010);
    File fixture = new File(c.getFilesDir(), "widget-upgrade-fixture.json");
    String[] names = {"NextPrayerWidget", "DayPrayerWidget", "ExpandedPrayerWidget"};
    if (stage.equals("before")) {
      assertEquals(
          "3.1.1", c.getPackageManager().getPackageInfo(c.getPackageName(), 0).versionName);
      JSONArray ids = new JSONArray();
      for (int n = 0; n < 3; n++) {
        int id = host.allocateAppWidgetId();
        ids.put(id);
        assertTrue(
            manager.bindAppWidgetIdIfAllowed(
                id,
                new ComponentName(
                    c.getPackageName(), c.getPackageName() + ".widgets." + names[n])));
        JSONObject cfg =
            new JSONObject()
                .put("followApp", false)
                .put("zone", "SGR03")
                .put("name", "Preserved zone")
                .put("state", "Selangor")
                .put("theme", n == 1 ? "dark" : "light");
        c.getSharedPreferences("widget_config", 0)
            .edit()
            .putString("" + id, cfg.toString())
            .commit();
      }
      JSONObject saved =
          new JSONObject()
              .put("ids", ids)
              .put("app", new JSONObject(c.getSharedPreferences("waktu_solat", 0).getAll()))
              .put(
                  "personal",
                  new JSONObject(c.getSharedPreferences("personal_library", 0).getAll()));
      try (FileOutputStream out = new FileOutputStream(fixture)) {
        out.write(saved.toString().getBytes(StandardCharsets.UTF_8));
      }
    } else {
      try {
        assertEquals(
            "3.2.0", c.getPackageManager().getPackageInfo(c.getPackageName(), 0).versionName);
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        try (FileInputStream in = new FileInputStream(fixture)) {
          byte[] bytes = new byte[4096];
          int n;
          while ((n = in.read(bytes)) != -1) buffer.write(bytes, 0, n);
        }
        JSONObject saved = new JSONObject(buffer.toString("UTF-8"));
        JSONArray ids = saved.getJSONArray("ids");
        // Package-replaced delivery is asynchronous; exercise the same cached refresh entry point.
        Class.forName(c.getPackageName() + ".widgets.PrayerWidgets")
            .getMethod("refresh", Context.class)
            .invoke(null, c);
        for (int n = 0; n < 3; n++) {
          int id = ids.getInt(n);
          assertNotNull(manager.getAppWidgetInfo(id));
          assertEquals(
              c.getPackageName() + ".widgets." + names[n],
              manager.getAppWidgetInfo(id).provider.getClassName());
          JSONObject config =
              new JSONObject(c.getSharedPreferences("widget_config", 0).getString("" + id, "{}"));
          assertEquals(2, config.getInt("schemaVersion"));
          assertEquals("SGR03", config.getString("zone"));
          assertFalse(config.getBoolean("followApp"));
          assertEquals(n == 1 ? "dark" : "light", config.getString("theme"));
          assertFalse(config.getBoolean("trackingEnabled"));
          assertEquals(n == 2, config.getBoolean("showHijri"));
          assertEquals(n == 2, config.getBoolean("showAuxiliary"));
        }
        for (String pref : new String[] {"app", "personal"}) {
          String file = pref.equals("app") ? "waktu_solat" : "personal_library";
          JSONObject baseline = saved.getJSONObject(pref);
          for (Iterator<String> it = baseline.keys(); it.hasNext(); ) {
            String key = it.next();
            if (key.startsWith("alarm_") || key.startsWith("prefetch_")) continue;
            Object actual = c.getSharedPreferences(file, 0).getAll().get(key);
            if (actual instanceof java.util.Set) {
              java.util.Set<String> expected = new java.util.HashSet<>();
              org.json.JSONArray list = baseline.getJSONArray(key);
              for (int n = 0; n < list.length(); n++) expected.add(list.getString(n));
              assertEquals(key, expected, actual);
            } else assertEquals(key, String.valueOf(baseline.get(key)), String.valueOf(actual));
          }
        }
      } finally {
        host.deleteHost();
        fixture.delete();
      }
    }
  }
}
