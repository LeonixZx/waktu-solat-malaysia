package com.example.waktusolatmalaysia;

import static org.junit.Assert.*;
import android.app.*;
import android.content.*;
import android.net.Uri;
import android.os.*;
import androidx.test.core.app.ActivityScenario;
import androidx.test.platform.app.InstrumentationRegistry;
import com.example.waktusolatmalaysia.preferences.*;
import com.example.waktusolatmalaysia.services.*;
import com.example.waktusolatmalaysia.views.MainActivity;
import java.time.*;
import java.util.*;
import org.junit.*;

/** Owner-authorized real-phone test. Never grants permissions or changes device audio/settings. */
public class PhysicalAlertsV334Test {
  private Context c;
  private Map<String,?> before;
  private PendingIntent pending;
  @Before public void snapshot(){
    org.junit.Assume.assumeTrue(InstrumentationRegistry.getArguments().getString("phoneAlerts","").equals("true"));
    c=InstrumentationRegistry.getInstrumentation().getTargetContext();
    before=new HashMap<>(c.getSharedPreferences("personal_library",0).getAll());
    assertTrue("Allow notification permission before the device test",PrayerNotifications.allowed(c,PrayerNotifications.PRAYER));
    assertTrue("Allow exact alarms before the device test",ReminderScheduler.exact(c));
  }
  @After public void restore(){
    if(c==null)return;
    c.stopService(new Intent(c,AzanPlaybackService.class));
    if(pending!=null){c.getSystemService(AlarmManager.class).cancel(pending);pending.cancel();}
    c.getSystemService(NotificationManager.class).cancel(104);
    V3FeaturesTest.restorePrefs(c.getSharedPreferences("personal_library",0),before);
    ReminderScheduler.scheduleNow(c);
  }
  private Notification notification(int id){for(var n:c.getSystemService(NotificationManager.class).getActiveNotifications())if(n.getId()==id)return n.getNotification();return null;}
  private void await(java.util.function.BooleanSupplier condition,String message){long end=SystemClock.elapsedRealtime()+15000;while(!condition.getAsBoolean()&&SystemClock.elapsedRealtime()<end)SystemClock.sleep(100);assertTrue(message,condition.getAsBoolean());}
  private void alarm(PrayerMode mode){
    new UserStore(c).set("prayer_mode_4",mode.name());ReminderScheduler.scheduleNow(c);
    long when=System.currentTimeMillis()+3000;
    Intent i=new Intent(c,PrayerAlarmReceiver.class).setData(Uri.parse("wsm://test/physical-alert-v334"))
        .putExtra("index",4).putExtra("when",when).putExtra("zone",new AppPreferences(c).zone().code)
        .putExtra("generation",ReminderScheduler.generation(c));
    pending=PendingIntent.getBroadcast(c,33490,i,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
    assertTrue(ReminderScheduler.schedule(c,pending,Instant.ofEpochMilli(when).atZone(ZoneId.of("Asia/Kuala_Lumpur"))));
  }
  @Test public void phoneReceivesBrandedNotificationFromAlarmManager(){
    try(var scenario=ActivityScenario.launch(MainActivity.class)){
      alarm(PrayerMode.NOTIFICATION);await(()->notification(104)!=null,"Phone must receive prayer notification");
      assertEquals(R.drawable.ic_notification_brand,notification(104).getSmallIcon().getResId());
      assertNotNull(notification(104).getLargeIcon());
    }
  }
  @Test public void phoneReceivesAzanAndStopActionStopsAudio()throws Exception{
    try(var scenario=ActivityScenario.launch(MainActivity.class)){
      AlertStatus.set(c,"azan","");alarm(PrayerMode.AZAN);
      await(()->AlertStatus.get(c,"azan").equals("playing"),"Phone must start scheduled adhan");
      assertEquals(R.drawable.ic_notification_brand,notification(500).getSmallIcon().getResId());
      SystemClock.sleep(2000);notification(500).actions[0].actionIntent.send();
      await(()->notification(500)==null,"Stop action must stop the phone's adhan");
    }
  }
}
