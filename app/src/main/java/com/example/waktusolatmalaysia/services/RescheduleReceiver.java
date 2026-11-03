package com.example.waktusolatmalaysia.services;

import android.content.*;

public final class RescheduleReceiver extends BroadcastReceiver {
  @Override
  public void onReceive(Context c, Intent intent) {
    PendingResult result = goAsync();
    new Thread(
            () -> {
              try {
                ReminderScheduler.scheduleNow(c.getApplicationContext());
                com.example.waktusolatmalaysia.widgets.PrayerWidgets.refresh(c);
                ScheduleRefreshWorker.enqueue(c, true);
              } finally {
                result.finish();
              }
            },
            "restore-reminders")
        .start();
  }
}
