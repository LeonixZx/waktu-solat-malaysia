package com.example.waktusolatmalaysia.services;

import android.Manifest;
import android.app.*;
import android.content.*;
import android.content.pm.PackageManager;
import android.graphics.*;
import android.os.Build;
import androidx.core.app.*;
import androidx.core.content.ContextCompat;
import com.example.waktusolatmalaysia.R;
import com.example.waktusolatmalaysia.ui.I18n;
import com.example.waktusolatmalaysia.views.MainActivity;

/** Shared branding and existing channel IDs. Never overrides a user's channel settings. */
public final class PrayerNotifications {
  public static final String PRAYER="prayer_reminders",AZAN="prayer_azan_alerts",
      PLAYBACK="azan_playback",TASK="personal_reminders";
  private PrayerNotifications() {}

  public static void channels(Context c) {
    if(Build.VERSION.SDK_INT<26)return;
    NotificationManager m=c.getSystemService(NotificationManager.class);
    for(String id:new String[]{PRAYER,AZAN,PLAYBACK,TASK}) {
      String name=id.equals(PLAYBACK)?I18n.choose(c,"Main balik azan","Adhan playback")
          :id.equals(TASK)?I18n.choose(c,"Peringatan peribadi","Personal reminders")
          :id.equals(AZAN)?I18n.choose(c,"Waktu azan","Adhan alerts")
          :I18n.choose(c,"Peringatan solat","Prayer reminders");
      NotificationChannel ch=new NotificationChannel(id,name,id.equals(PLAYBACK)
          ?NotificationManager.IMPORTANCE_LOW:id.equals(TASK)
          ?NotificationManager.IMPORTANCE_DEFAULT:NotificationManager.IMPORTANCE_HIGH);
      if(id.equals(AZAN)||id.equals(PLAYBACK))ch.setSound(null,null);
      m.createNotificationChannel(ch);
    }
  }
  public static boolean allowed(Context c,String channel) {
    if(Build.VERSION.SDK_INT>=33&&ContextCompat.checkSelfPermission(c,Manifest.permission.POST_NOTIFICATIONS)
        !=PackageManager.PERMISSION_GRANTED)return false;
    if(!NotificationManagerCompat.from(c).areNotificationsEnabled())return false;
    if(Build.VERSION.SDK_INT>=26) {
      NotificationChannel ch=c.getSystemService(NotificationManager.class).getNotificationChannel(channel);
      if(ch!=null&&ch.getImportance()==NotificationManager.IMPORTANCE_NONE)return false;
    }return true;
  }
  public static NotificationCompat.Builder builder(Context c,String channel) {
    channels(c);
    BitmapFactory.Options opts=new BitmapFactory.Options();opts.inSampleSize=4;
    Bitmap logo=BitmapFactory.decodeResource(c.getResources(),R.drawable.brand_mark_v322,opts);
    return new NotificationCompat.Builder(c,channel).setSmallIcon(R.drawable.ic_notification_brand)
        .setLargeIcon(logo).setColor(Color.rgb(150,96,37)).setOnlyAlertOnce(true)
        .setDefaults(channel.equals(PRAYER)||channel.equals(TASK)?Notification.DEFAULT_SOUND:0)
        .setPriority(NotificationCompat.PRIORITY_HIGH).setCategory(NotificationCompat.CATEGORY_REMINDER);
  }
  public static PendingIntent open(Context c) {
    return PendingIntent.getActivity(c,0,new Intent(c,MainActivity.class)
        .putExtra("open_schedule",true)
        .putExtra("date",java.time.LocalDate.now(com.example.waktusolatmalaysia.utils.PrayerClock.MALAYSIA).toString()),
        PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
  }
  public static PendingIntent play(Context c,int index) {
    Intent i=new Intent(c,AzanPlaybackService.class).setAction("play").putExtra("index",index);
    return Build.VERSION.SDK_INT>=26?PendingIntent.getForegroundService(c,600+index,i,
        PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE)
        :PendingIntent.getService(c,600+index,i,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
  }
}
