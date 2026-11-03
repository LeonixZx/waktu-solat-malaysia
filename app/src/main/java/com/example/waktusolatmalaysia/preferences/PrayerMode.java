package com.example.waktusolatmalaysia.preferences;

import android.content.Context;
import com.example.waktusolatmalaysia.services.ReminderScheduler;

public enum PrayerMode {
  OFF,
  NOTIFICATION,
  AZAN;

  public static PrayerMode get(Context c, int index) {
    UserStore s = new UserStore(c);
    String value = s.text("prayer_mode_" + index, "");
    if (value.isEmpty()) {
      PrayerMode mode =
          s.flag("reminder_" + index, false)
              ? s.flag("azan_mode", false) ? AZAN : NOTIFICATION
              : OFF;
      s.set("prayer_mode_" + index, mode.name());
      return mode;
    }
    try {
      return valueOf(value);
    } catch (Exception e) {
      return OFF;
    }
  }

  public static void set(Context c, int index, PrayerMode value) {
    UserStore s = new UserStore(c);
    s.set("prayer_mode_" + index, value.name());
    s.set("reminder_" + index, value != OFF);
    ReminderScheduler.reschedule(c);
  }
}
