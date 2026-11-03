package com.example.waktusolatmalaysia.services;

import android.app.*;
import android.content.*;
import android.content.pm.ServiceInfo;
import android.media.*;
import android.os.*;
import androidx.core.app.*;
import com.example.waktusolatmalaysia.R;
import com.example.waktusolatmalaysia.preferences.PrayerMode;
import com.example.waktusolatmalaysia.ui.I18n;

/** Local alarm-stream playback with bounded CPU wake lock, focus and explicit status. */
public final class AzanPlaybackService extends Service {
  private MediaPlayer player;
  private AudioManager audio;
  private AudioFocusRequest focus;
  private final Handler handler=new Handler(Looper.getMainLooper());
  private final AudioManager.OnAudioFocusChangeListener focusListener=change->{
    if(change<0)finish("audio_focus_lost");
  };

  @Override public int onStartCommand(Intent intent,int flags,int startId) {
    if(intent==null||"stop".equals(intent.getAction())) {
      finish("stopped");return START_NOT_STICKY;
    }
    int index=intent.getIntExtra("index",-1);
    boolean test="test".equals(intent.getAction());
    if(index<0||index>7||(!test&&PrayerMode.get(this,index)!=PrayerMode.AZAN)) {
      finish("disabled");return START_NOT_STICKY;
    }
    release();
    PendingIntent stop=PendingIntent.getService(this,0,
        new Intent(this,AzanPlaybackService.class).setAction("stop"),
        PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
    Notification notification=PrayerNotifications.builder(this,PrayerNotifications.PLAYBACK)
        .setContentTitle((test?I18n.choose(this,"Ujian azan · ","Adhan test · "):"Azan · ")+I18n.prayer(this,index))
        .setContentText(I18n.choose(this,"Kelantangan penggera digunakan","Uses alarm volume"))
        .setContentIntent(PrayerNotifications.open(this)).setOngoing(true)
        .setCategory(NotificationCompat.CATEGORY_ALARM).setPriority(NotificationCompat.PRIORITY_LOW)
        .addAction(R.drawable.ic_pause,getString(R.string.stop_azan),stop).build();
    try {
      ServiceCompat.startForeground(this,500,notification,Build.VERSION.SDK_INT>=29
          ?ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK:0);
      audio=(AudioManager)getSystemService(AUDIO_SERVICE);
      if(audio.getStreamVolume(AudioManager.STREAM_ALARM)==0){finish("alarm_volume_zero");return START_NOT_STICKY;}
      AudioAttributes attributes=new AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_ALARM)
          .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH).build();
      int granted;
      if(Build.VERSION.SDK_INT>=26){
        focus=new AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT)
            .setAudioAttributes(attributes).setOnAudioFocusChangeListener(focusListener,handler).build();
        granted=audio.requestAudioFocus(focus);
      }else granted=audio.requestAudioFocus(focusListener,AudioManager.STREAM_ALARM,AudioManager.AUDIOFOCUS_GAIN_TRANSIENT);
      if(granted!=AudioManager.AUDIOFOCUS_REQUEST_GRANTED){finish("audio_focus_denied");return START_NOT_STICKY;}
      player=new MediaPlayer();
      player.setAudioAttributes(attributes);
      player.setWakeMode(getApplicationContext(),PowerManager.PARTIAL_WAKE_LOCK);
      try(var file=getResources().openRawResourceFd(index==1?R.raw.adhan_fajr:R.raw.adhan_standard)){
        player.setDataSource(file.getFileDescriptor(),file.getStartOffset(),file.getLength());
      }
      player.setOnCompletionListener(p->finish("completed"));
      player.setOnErrorListener((p,a,b)->{finish("playback_error");return true;});
      player.setOnPreparedListener(p->{
        p.start();AlertStatus.set(this,"azan","playing");
        if(test)handler.postDelayed(()->finish("test_completed"),5000);
      });
      AlertStatus.set(this,"azan","preparing");
      player.prepareAsync();
    }catch(RuntimeException|java.io.IOException error){
      android.util.Log.w("PrayerAlerts","Adhan playback unavailable",error);
      finish("playback_error");
    }
    return START_NOT_STICKY;
  }
  private void finish(String state){
    AlertStatus.set(this,"azan",state);release();stopForeground(true);stopSelf();
  }
  private void release(){
    handler.removeCallbacksAndMessages(null);
    if(player!=null){player.release();player=null;}
    if(audio!=null&&Build.VERSION.SDK_INT>=26&&focus!=null){audio.abandonAudioFocusRequest(focus);focus=null;}
    else if(audio!=null&&Build.VERSION.SDK_INT<26)audio.abandonAudioFocus(focusListener);
  }
  @Override public void onDestroy(){release();super.onDestroy();}
  @Override public IBinder onBind(Intent intent){return null;}
}
