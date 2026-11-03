package com.example.waktusolatmalaysia;

import android.app.*;
import android.content.*;
import android.media.AudioManager;
import android.os.*;
import androidx.test.platform.app.InstrumentationRegistry;
import org.json.*;
import org.junit.Test;

/** Read-only framework inspection, also usable against the previous installed app version. */
public class AlertDeviceDiagnosticTest {
  @Test public void reportDeviceAlertState()throws Exception{
    var inst=InstrumentationRegistry.getInstrumentation();Context c=inst.getTargetContext();
    NotificationManager notifications=c.getSystemService(NotificationManager.class);
    AudioManager audio=(AudioManager)c.getSystemService(Context.AUDIO_SERVICE);
    JSONObject j=new JSONObject().put("version",c.getPackageManager().getPackageInfo(c.getPackageName(),0).versionName)
        .put("api",Build.VERSION.SDK_INT).put("notificationsEnabled",notifications.areNotificationsEnabled())
        .put("exactAlarms",Build.VERSION.SDK_INT<31||c.getSystemService(AlarmManager.class).canScheduleExactAlarms())
        .put("alarmVolume",audio.getStreamVolume(AudioManager.STREAM_ALARM))
        .put("alarmMaximum",audio.getStreamMaxVolume(AudioManager.STREAM_ALARM))
        .put("interruptionFilter",notifications.getCurrentInterruptionFilter())
        .put("batteryOptimizationExempt",c.getSystemService(PowerManager.class).isIgnoringBatteryOptimizations(c.getPackageName()));
    JSONArray channels=new JSONArray();if(Build.VERSION.SDK_INT>=26)for(NotificationChannel ch:notifications.getNotificationChannels())
      channels.put(new JSONObject().put("id",ch.getId()).put("importance",ch.getImportance()).put("sound",String.valueOf(ch.getSound())));
    j.put("channels",channels);
    try(var out=new java.io.FileOutputStream(new java.io.File(c.getExternalFilesDir(null),"alert-device-state.json"))){out.write(j.toString(2).getBytes(java.nio.charset.StandardCharsets.UTF_8));}
    Bundle result=new Bundle();result.putString("deviceAlertState",j.toString());inst.sendStatus(0,result);
  }
}
