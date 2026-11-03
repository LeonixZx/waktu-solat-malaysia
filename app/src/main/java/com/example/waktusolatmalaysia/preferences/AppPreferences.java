package com.example.waktusolatmalaysia.preferences;

import android.content.Context;
import android.content.SharedPreferences;
import com.example.waktusolatmalaysia.model.PrayerZone;

public final class AppPreferences {
  private final SharedPreferences preferences;
  private final Context context;

  public AppPreferences(Context context) {
    this.context = context.getApplicationContext();
    preferences = context.getSharedPreferences("waktu_solat", Context.MODE_PRIVATE);
  }

  public PrayerZone zone() {
    return new PrayerZone(
        preferences.getString("zone_state", "Wilayah Persekutuan"),
        preferences.getString("zone_name", "Kuala Lumpur, Putrajaya"),
        preferences.getString("zone_code", "WLY01"));
  }

  public void setZone(PrayerZone zone) {
    preferences
        .edit()
        .putString("zone_state", zone.state)
        .putString("zone_name", zone.name)
        .putString("zone_code", zone.code)
        .apply();
    com.example.waktusolatmalaysia.services.ReminderScheduler.reschedule(context);
    com.example.waktusolatmalaysia.widgets.PrayerWidgets.refreshAsync(context);
    com.example.waktusolatmalaysia.services.ScheduleRefreshWorker.enqueue(context, true);
  }

  public boolean isTwelveHour() {
    return preferences.getBoolean("twelve_hour", false);
  }

  public void setTwelveHour(boolean enabled) {
    preferences.edit().putBoolean("twelve_hour", enabled).apply();
  }

  public String theme() {
    return preferences.getString("theme", preferences.getBoolean("dark", false) ? "dark" : "light");
  }

  public void setTheme(String theme) {
    preferences.edit().putString("theme", theme).putBoolean("dark", theme.equals("dark")).apply();
  }

  public boolean isDark() {
    return theme().equals("dark") || theme().equals("system")
        && (android.content.res.Resources.getSystem().getConfiguration().uiMode & android.content.res.Configuration.UI_MODE_NIGHT_MASK)
            == android.content.res.Configuration.UI_MODE_NIGHT_YES;
  }

  public void setDark(boolean enabled) {
    preferences
        .edit()
        .putBoolean("dark", enabled)
        .putString("theme", enabled ? "dark" : "light")
        .apply();
  }
}
