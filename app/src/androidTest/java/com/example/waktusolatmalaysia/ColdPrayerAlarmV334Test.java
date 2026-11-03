package com.example.waktusolatmalaysia;

import static org.junit.Assert.*;
import android.app.*;
import android.content.*;
import android.net.Uri;
import android.os.*;
import androidx.test.platform.app.InstrumentationRegistry;
import com.example.waktusolatmalaysia.preferences.*;
import com.example.waktusolatmalaysia.services.*;
import org.junit.Test;

/** Harness seeds a future real alarm, exits instrumentation, then kills only the emulator app process. */
public class ColdPrayerAlarmV334Test {
  @Test public void seedFutureAlarmForProcessRestart()throws Exception{
    org.junit.Assume.assumeTrue(Build.FINGERPRINT.contains("generic")||Build.MODEL.contains("sdk")||Build.MODEL.contains("Android SDK"));
    Context c=InstrumentationRegistry.getInstrumentation().getTargetContext();
    boolean azan=InstrumentationRegistry.getArguments().getString("mode","notification").equals("azan");
    boolean verify=InstrumentationRegistry.getArguments().getString("phase","seed").equals("verify");
    java.io.File backup=new java.io.File(c.getCacheDir(),"cold-alert-before.ser");
    if(verify){
      try{
        android.app.Notification delivered=null;
        for(var n:c.getSystemService(NotificationManager.class).getActiveNotifications())if(n.getId()==104)delivered=n.getNotification();
        assertNotNull("Cold-start alarm must deliver",delivered);
        assertEquals(R.drawable.ic_notification_brand,delivered.getSmallIcon().getResId());
        if(azan)assertEquals("playing",AlertStatus.get(c,"azan"));
      }finally{
        c.stopService(new Intent(c,AzanPlaybackService.class));
        c.getSystemService(NotificationManager.class).cancel(104);
        try(var input=new java.io.ObjectInputStream(new java.io.FileInputStream(backup))){
          V3FeaturesTest.restorePrefs(c.getSharedPreferences("personal_library",0),(java.util.Map<String,?>)input.readObject());
        }
        backup.delete();ReminderScheduler.scheduleNow(c);
      }
      return;
    }
    c.stopService(new Intent(c,AzanPlaybackService.class));
    c.getSystemService(NotificationManager.class).cancel(104);
    try(var out=new java.io.ObjectOutputStream(new java.io.FileOutputStream(backup))){
      out.writeObject(new java.util.HashMap<>(c.getSharedPreferences("personal_library",0).getAll()));
    }
    new UserStore(c).set("prayer_mode_4",azan?"AZAN":"NOTIFICATION");
    ReminderScheduler.scheduleNow(c);
    long when=System.currentTimeMillis()+20_000;
    Intent i=new Intent(c,PrayerAlarmReceiver.class).setData(Uri.parse("wsm://test/cold-v334"))
        .putExtra("index",4).putExtra("zone",new AppPreferences(c).zone().code)
        .putExtra("generation",ReminderScheduler.generation(c)).putExtra("when",when);
    PendingIntent p=PendingIntent.getBroadcast(c,33499,i,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
    assertTrue(ReminderScheduler.schedule(c,p,java.time.Instant.ofEpochMilli(when).atZone(java.time.ZoneId.of("Asia/Kuala_Lumpur"))));
    Bundle result=new Bundle();result.putString("coldAlarmWhen",Long.toString(when));
    InstrumentationRegistry.getInstrumentation().sendStatus(0,result);
  }
}
