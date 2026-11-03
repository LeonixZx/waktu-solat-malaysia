package com.example.waktusolatmalaysia.services;

import android.Manifest;
import android.app.*;
import android.content.*;
import android.content.pm.PackageManager;
import android.os.Build;
import androidx.core.app.*;
import androidx.core.content.ContextCompat;
import com.example.waktusolatmalaysia.R;
import com.example.waktusolatmalaysia.preferences.*;
import com.example.waktusolatmalaysia.ui.I18n;
import com.example.waktusolatmalaysia.views.MainActivity;

public final class PrayerAlarmReceiver extends BroadcastReceiver {
  public static final String[] NAMES = {
    "Imsak", "Subuh", "Syuruk", "Dhuha", "Zohor", "Asar", "Maghrib", "Isyak"
  };

  @Override
  public void onReceive(Context c, Intent intent) {
    int index = intent.getIntExtra("index", -1);
    if (index < 0
        || index > 7
        || !ReminderScheduler.current(c, intent)
        || PrayerMode.get(c, index) == PrayerMode.OFF) return;
    UserStore store = new UserStore(c);
    String delivered = "prayer_delivered_" + index;
    String key = String.valueOf(intent.getLongExtra("when", 0));
    if (store.text(delivered, "").equals(key)) return;
    store.set(delivered, key);
    PrayerMode mode = PrayerMode.get(c, index);
    NotificationManager manager = c.getSystemService(NotificationManager.class);
    String channel = mode == PrayerMode.AZAN ? "prayer_azan_alerts" : "prayer_reminders";
    if (Build.VERSION.SDK_INT >= 26) {
      NotificationChannel ch =
          new NotificationChannel(
              channel,
              I18n.choose(c, "Peringatan solat", "Prayer reminders"),
              NotificationManager.IMPORTANCE_HIGH);
      if (mode == PrayerMode.AZAN) ch.setSound(null, null);
      manager.createNotificationChannel(ch);
    }
    PendingIntent open =
        PendingIntent.getActivity(
            c,
            0,
            new Intent(c, MainActivity.class),
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    if (Build.VERSION.SDK_INT < 33
        || ContextCompat.checkSelfPermission(c, Manifest.permission.POST_NOTIFICATIONS)
            == PackageManager.PERMISSION_GRANTED) {
      NotificationManagerCompat.from(c)
          .notify(
              100 + index,
              new NotificationCompat.Builder(c, channel)
                  .setSmallIcon(R.drawable.ic_bell)
                  .setContentTitle(I18n.choose(c, "Waktu ", "Time for ") + I18n.prayer(c, index))
                  .setContentText(new AppPreferences(c).zone().name)
                  .setContentIntent(open)
                  .setAutoCancel(true)
                  .build());
      if (mode == PrayerMode.AZAN)
        try {
          ContextCompat.startForegroundService(
              c, new Intent(c, AzanPlaybackService.class).putExtra("index", index));
        } catch (RuntimeException ignored) {
        }
    }
    PendingResult result = goAsync();
    new Thread(
            () -> {
              try {
                ReminderScheduler.scheduleNow(c);
                com.example.waktusolatmalaysia.widgets.PrayerWidgets.refresh(c);
              } finally {
                result.finish();
              }
            },
            "prayer-alarm")
        .start();
  }
}
