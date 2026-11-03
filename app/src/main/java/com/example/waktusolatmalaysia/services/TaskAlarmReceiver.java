package com.example.waktusolatmalaysia.services;

import android.Manifest;
import android.app.*;
import android.content.*;
import android.content.pm.PackageManager;
import android.os.Build;
import androidx.core.app.*;
import androidx.core.content.ContextCompat;
import com.example.waktusolatmalaysia.R;
import com.example.waktusolatmalaysia.model.*;
import com.example.waktusolatmalaysia.preferences.*;
import com.example.waktusolatmalaysia.repo.*;
import com.example.waktusolatmalaysia.ui.I18n;
import com.example.waktusolatmalaysia.views.MainActivity;

public final class TaskAlarmReceiver extends BroadcastReceiver {
  @Override
  public void onReceive(Context c, Intent intent) {
    if (!ReminderScheduler.current(c, intent)) return;
    String id = intent.getStringExtra("id"), anchor = intent.getStringExtra("anchor_date");
    PrayerRelativeReminder task = null;
    for (PrayerRelativeReminder t : new TaskStore(c).safeLoad())
      if (t.id.equals(id) && t.enabled) task = t;
    if (task == null) return;
    UserStore s = new UserStore(c);
    if (s.text("delivered_task_" + id, "").equals(anchor)) return;
    s.set("delivered_task_" + id, anchor);
    PrayerNotifications.channels(c);
    PendingIntent open =
        PendingIntent.getActivity(
            c,
            id.hashCode(),
            new Intent(c, MainActivity.class).putExtra("open_tasks", true),
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    if (PrayerNotifications.allowed(c,PrayerNotifications.TASK))
      NotificationManagerCompat.from(c)
          .notify(
              "task:" + id,
              1,
              PrayerNotifications.builder(c,PrayerNotifications.TASK)
                  .setContentTitle(task.title)
                  .setContentText(
                      new AppPreferences(c).zone().name + " · " + I18n.prayer(c, task.anchor))
                  .setContentIntent(open)
                  .setAutoCancel(true)
                  .build());
    PendingResult done = goAsync();
    new Thread(
            () -> {
              try {
                ReminderScheduler.scheduleNow(c);
              } finally {
                done.finish();
              }
            },
            "task-alarm")
        .start();
  }
}
