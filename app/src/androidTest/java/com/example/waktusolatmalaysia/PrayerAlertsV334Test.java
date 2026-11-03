package com.example.waktusolatmalaysia;

import static org.junit.Assert.*;
import android.app.*;
import android.content.*;
import android.media.AudioManager;
import android.net.Uri;
import android.os.*;
import android.widget.*;
import androidx.test.core.app.ActivityScenario;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.uiautomator.UiDevice;
import com.example.waktusolatmalaysia.preferences.*;
import com.example.waktusolatmalaysia.services.*;
import com.example.waktusolatmalaysia.views.MainActivity;
import java.io.*;
import java.time.*;
import java.util.*;
import org.junit.*;

/** Explicit emulator-only alert tests; never changes a real phone's permissions or audio volume. */
public class PrayerAlertsV334Test {
  private Context c;
  private Map<String,?> before;
  private final List<PendingIntent> alarms=new ArrayList<>();
  @Before public void snapshot(){
    org.junit.Assume.assumeTrue(Build.FINGERPRINT.contains("generic")||Build.MODEL.contains("sdk")||Build.MODEL.contains("Android SDK"));
    c=InstrumentationRegistry.getInstrumentation().getTargetContext();
    before=new HashMap<>(c.getSharedPreferences("personal_library",0).getAll());
    PrayerNotifications.channels(c);
  }
  @After public void restore(){
    if(c==null)return;
    c.stopService(new Intent(c,AzanPlaybackService.class));
    for(PendingIntent p:alarms){c.getSystemService(AlarmManager.class).cancel(p);p.cancel();}
    c.getSystemService(NotificationManager.class).cancel(104);
    c.getSystemService(NotificationManager.class).cancel(9001);
    V3FeaturesTest.restorePrefs(c.getSharedPreferences("personal_library",0),before);
    ReminderScheduler.scheduleNow(c);
  }
  private Intent due(long when){return new Intent(c,PrayerAlarmReceiver.class)
      .setData(Uri.parse("wsm://test/v334/"+when)).putExtra("index",4).putExtra("when",when)
      .putExtra("zone",new AppPreferences(c).zone().code)
      .putExtra("generation",ReminderScheduler.generation(c));}
  private void mode(PrayerMode m){new UserStore(c).set("prayer_mode_4",m.name());ReminderScheduler.scheduleNow(c);}
  private void schedule(Intent i,long when){
    PendingIntent p=PendingIntent.getBroadcast(c,33404,i,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
    alarms.add(p);assertTrue(ReminderScheduler.schedule(c,p,Instant.ofEpochMilli(when).atZone(ZoneId.of("Asia/Kuala_Lumpur"))));
  }
  private void await(java.util.function.BooleanSupplier condition,String message)throws Exception{
    long end=SystemClock.elapsedRealtime()+15000;
    while(!condition.getAsBoolean()&&SystemClock.elapsedRealtime()<end)SystemClock.sleep(100);
    assertTrue(message,condition.getAsBoolean());
  }
  private Notification notification(int id){
    for(var n:c.getSystemService(NotificationManager.class).getActiveNotifications())if(n.getId()==id)return n.getNotification();return null;
  }
  private void capture(String name)throws Exception{
    UiDevice.getInstance(InstrumentationRegistry.getInstrumentation()).openNotification();SystemClock.sleep(500);
    var b=InstrumentationRegistry.getInstrumentation().getUiAutomation().takeScreenshot();assertNotNull(b);
    File dir=new File(c.getExternalFilesDir(null),"alerts-v334");dir.mkdirs();
    try(var out=new FileOutputStream(new File(dir,name+".png"))){b.compress(android.graphics.Bitmap.CompressFormat.PNG,100,out);}b.recycle();
    UiDevice.getInstance(InstrumentationRegistry.getInstrumentation()).pressBack();
  }

  @Test public void routineRefreshKeepsDueAlarmValidAndChangesInvalidateIt(){
    mode(PrayerMode.NOTIFICATION);Intent intent=due(System.currentTimeMillis());
    String generation=ReminderScheduler.generation(c);
    for(int i=0;i<3;i++)ReminderScheduler.scheduleNow(c);
    assertEquals(generation,ReminderScheduler.generation(c));assertTrue(ReminderScheduler.current(c,intent));
    intent.putExtra("when",System.currentTimeMillis()-6*60_000L);assertFalse(ReminderScheduler.current(c,intent));
    intent.putExtra("when",System.currentTimeMillis());
    new UserStore(c).set("prayer_mode_4",PrayerMode.OFF.name());
    assertFalse(ReminderScheduler.current(c,intent));
  }
  @Test public void actualAlarmPostsOurLogoAndDoesNotRepeat()throws Exception{
    mode(PrayerMode.NOTIFICATION);assertTrue(PrayerNotifications.allowed(c,PrayerNotifications.PRAYER));
    long when=System.currentTimeMillis()+2500;Intent i=due(when);schedule(i,when);
    await(()->notification(104)!=null,"AlarmManager must deliver the prayer notification");
    Notification n=notification(104);assertEquals(R.drawable.ic_notification_brand,n.getSmallIcon().getResId());
    assertNotNull(n.getLargeIcon());assertNotNull(n.contentIntent);
    capture("prayer-notification");
    c.getSystemService(NotificationManager.class).cancel(104);c.sendBroadcast(i);SystemClock.sleep(350);
    assertNull(notification(104));
  }
  @Test public void actualAzanAlarmPlaysOnAlarmStreamAndStopActionWorks()throws Exception{
    mode(PrayerMode.AZAN);long when=System.currentTimeMillis()+2500;schedule(due(when),when);
    await(()->AlertStatus.get(c,"azan").equals("playing"),"Scheduled azan must reach MediaPlayer.start");
    Notification n=notification(500);assertNotNull(n);
    assertEquals(R.drawable.ic_notification_brand,n.getSmallIcon().getResId());assertNotNull(n.getLargeIcon());
    assertNotNull(n.actions);assertTrue(n.actions.length>0);
    assertNotNull(notification(104).actions);
    capture("azan-playing");n.actions[0].actionIntent.send();
    await(()->notification(500)==null,"Stop must remove foreground playback");
    assertEquals("stopped",AlertStatus.get(c,"azan"));
  }
  @Test public void bothBundledRecordingsPlayAndFiveSecondTestEnds()throws Exception{
    try(var scenario=ActivityScenario.launch(MainActivity.class)){
      for(int index:new int[]{1,4}){
        AlertStatus.set(c,"azan","");
        scenario.onActivity(a->androidx.core.content.ContextCompat.startForegroundService(a,
            new Intent(a,AzanPlaybackService.class).setAction("test").putExtra("index",index)));
        await(()->AlertStatus.get(c,"azan").equals("playing"),"Bundled recording must play");
        await(()->AlertStatus.get(c,"azan").equals("test_completed"),"Test must end after five seconds");
        await(()->notification(500)==null,"Completed test must remove playback notification");
      }
    }
  }
  @Test public void systemSelectedAlarmVolumeIsRespected()throws Exception{
    AudioManager audio=(AudioManager)c.getSystemService(Context.AUDIO_SERVICE);int old=audio.getStreamVolume(AudioManager.STREAM_ALARM);
    try(var scenario=ActivityScenario.launch(MainActivity.class)){
      audio.setStreamVolume(AudioManager.STREAM_ALARM,0,0);
      int selected=audio.getStreamVolume(AudioManager.STREAM_ALARM);
      scenario.onActivity(a->androidx.core.content.ContextCompat.startForegroundService(a,new Intent(a,AzanPlaybackService.class).setAction("test").putExtra("index",4)));
      if(selected==0)await(()->AlertStatus.get(c,"azan").equals("alarm_volume_zero"),"Muted alarm volume must be explained");
      else await(()->AlertStatus.get(c,"azan").equals("playing"),"Android's minimum permitted alarm volume must remain usable");
      assertEquals(selected,audio.getStreamVolume(AudioManager.STREAM_ALARM));
    }finally{audio.setStreamVolume(AudioManager.STREAM_ALARM,old,0);}
  }
  @Test public void scheduleOffersRealNotificationAndAdhanTests()throws Exception{
    try(var scenario=ActivityScenario.launch(MainActivity.class)){
      scenario.onActivity(a->a.openDestination(R.id.scheduleFragment,null));
      InstrumentationRegistry.getInstrumentation().waitForIdleSync();
      scenario.onActivity(a->{
        assertNotNull(a.findViewById(R.id.alert_health));
        assertNotNull(a.findViewById(R.id.alert_test_azan));
        a.findViewById(R.id.alert_test_notification).performClick();
      });
      await(()->notification(9001)!=null,"The user-facing test must post a real branded notification");
      assertEquals(R.drawable.ic_notification_brand,notification(9001).getSmallIcon().getResId());
    }
  }
}
