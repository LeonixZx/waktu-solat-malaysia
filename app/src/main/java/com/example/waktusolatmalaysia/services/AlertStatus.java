package com.example.waktusolatmalaysia.services;

import android.content.Context;

/** Local operational status; no coordinates, tracking records or content are logged. */
public final class AlertStatus {
  private AlertStatus() {}
  public static void set(Context c,String key,String value) {
    c.getSharedPreferences("notification_status",0).edit().putString(key,value)
        .putLong(key+"_at",System.currentTimeMillis()).apply();
    android.util.Log.i("PrayerAlerts",key+"="+value);
  }
  public static String get(Context c,String key) {
    return c.getSharedPreferences("notification_status",0).getString(key,"");
  }
}
