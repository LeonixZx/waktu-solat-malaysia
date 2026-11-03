package com.example.waktusolatmalaysia.core;

import android.app.Application;
import androidx.appcompat.app.AppCompatDelegate;
import com.example.waktusolatmalaysia.preferences.AppPreferences;

public final class WaktuSolatApp extends Application {
  private final android.content.SharedPreferences.OnSharedPreferenceChangeListener widgetSettings =
      (p, key) -> {
        if (key == null
            || key.equals("language")
            || key.equals("twelve_hour")
            || key.startsWith("zone_")) {
          com.example.waktusolatmalaysia.widgets.PrayerWidgets.refreshAsync(this);
          if (key == null || key.startsWith("zone_")) {
            com.example.waktusolatmalaysia.services.WidgetScheduleRefreshWorker.enqueueAll(this);
            com.example.waktusolatmalaysia.services.WidgetScheduleRefreshWorker.cleanup(this);
          }
        }
      };
  private final android.content.SharedPreferences.OnSharedPreferenceChangeListener widgetRecords =
      (p, key) -> {
        if (key == null || key.startsWith("prayed_"))
          com.example.waktusolatmalaysia.widgets.PrayerWidgets.refreshAsync(this);
      };

  @Override
  protected void attachBaseContext(android.content.Context base) {
    super.attachBaseContext(com.example.waktusolatmalaysia.ui.I18n.wrapApplication(base));
  }

  @Override
  public void onCreate() {
    super.onCreate();
    getSharedPreferences("waktu_solat", 0).registerOnSharedPreferenceChangeListener(widgetSettings);
    getSharedPreferences("personal_library", 0)
        .registerOnSharedPreferenceChangeListener(widgetRecords);
    // Restore alarms from validated disk data even when the next network job is constrained.
    com.example.waktusolatmalaysia.services.ReminderScheduler.reschedule(this);
    AppCompatDelegate.setDefaultNightMode(
        new AppPreferences(this).theme().equals("system")
            ? AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
            : new AppPreferences(this).isDark()
                ? AppCompatDelegate.MODE_NIGHT_YES
                : AppCompatDelegate.MODE_NIGHT_NO);
  }

  @Override
  public void onConfigurationChanged(android.content.res.Configuration configuration) {
    super.onConfigurationChanged(configuration);
    com.example.waktusolatmalaysia.widgets.PrayerWidgets.refreshAsync(this);
  }
}
