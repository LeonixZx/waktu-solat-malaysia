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
    PrayerNotifications.channels(c);
    String channel = mode == PrayerMode.AZAN ? PrayerNotifications.AZAN : PrayerNotifications.PRAYER;
    if (PrayerNotifications.allowed(c,channel)) {
      NotificationCompat.Builder alert=PrayerNotifications.builder(c,channel)
          .setContentTitle(I18n.choose(c,"Waktu ","Time for ")+I18n.prayer(c,index))
          .setContentText(new AppPreferences(c).zone().name)
          .setContentIntent(PrayerNotifications.open(c)).setAutoCancel(true);
      if(mode==PrayerMode.AZAN)alert.addAction(R.drawable.ic_play,
          I18n.choose(c,"Mainkan azan","Play adhan"),PrayerNotifications.play(c,index));
      try {
        NotificationManagerCompat.from(c).notify(100+index,alert.build());
        AlertStatus.set(c,"notification","posted");
      }catch(SecurityException denied){AlertStatus.set(c,"notification","blocked");}
    }else AlertStatus.set(c,"notification","blocked");
    if(mode==PrayerMode.AZAN)try {
      ContextCompat.startForegroundService(c,new Intent(c,AzanPlaybackService.class)
          .setAction("alarm").putExtra("index",index));
    }catch(RuntimeException denied){
      AlertStatus.set(c,"azan","background_start_blocked");
      android.util.Log.w("PrayerAlerts","Adhan start blocked",denied);
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
