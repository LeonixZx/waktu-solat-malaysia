package com.example.waktusolatmalaysia.services;

import android.app.*;
import android.content.*;
import android.content.pm.ServiceInfo;
import android.media.*;
import android.os.*;
import androidx.core.app.*;
import com.example.waktusolatmalaysia.R;

public final class AzanPlaybackService extends Service {
  private final AudioManager.OnAudioFocusChangeListener focusListener =
      change -> {
        if (change < 0) stopSelf();
      };
  private MediaPlayer player;
  private AudioManager audio;
  private AudioFocusRequest focus;

  @Override
  public int onStartCommand(Intent intent, int flags, int id) {
    if (intent == null || "stop".equals(intent.getAction())) {
      stopSelf();
      return START_NOT_STICKY;
    }
    NotificationManager manager = getSystemService(NotificationManager.class);
    if (Build.VERSION.SDK_INT >= 26) {
      NotificationChannel channel =
          new NotificationChannel(
              "azan_playback",
              getString(R.string.azan_channel),
              NotificationManager.IMPORTANCE_LOW);
      channel.setSound(null, null);
      manager.createNotificationChannel(channel);
    }
    PendingIntent stop =
        PendingIntent.getService(
            this,
            0,
            new Intent(this, AzanPlaybackService.class).setAction("stop"),
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    Notification notification =
        new NotificationCompat.Builder(this, "azan_playback")
            .setSmallIcon(R.drawable.ic_prayer)
            .setContentTitle("Azan · " + PrayerAlarmReceiver.NAMES[intent.getIntExtra("index", 1)])
            .setOngoing(true)
            .addAction(R.drawable.ic_pause, getString(R.string.stop_azan), stop)
            .build();
    ServiceCompat.startForeground(
        this,
        500,
        notification,
        Build.VERSION.SDK_INT >= 29 ? ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK : 0);
    release();
    audio = (AudioManager) getSystemService(AUDIO_SERVICE);
    AudioAttributes attributes =
        new AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_ALARM)
            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
            .build();
    int granted;
    if (Build.VERSION.SDK_INT >= 26) {
      focus =
          new AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT)
              .setAudioAttributes(attributes)
              .setOnAudioFocusChangeListener(focusListener)
              .build();
      granted = audio.requestAudioFocus(focus);
    } else
      granted =
          audio.requestAudioFocus(
              focusListener, AudioManager.STREAM_ALARM, AudioManager.AUDIOFOCUS_GAIN_TRANSIENT);
    if (granted != AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
      stopSelf();
      return START_NOT_STICKY;
    }
    try {
      player = new MediaPlayer();
      player.setAudioAttributes(attributes);
      android.content.res.AssetFileDescriptor file =
          getResources()
              .openRawResourceFd(
                  intent.getIntExtra("index", 1) == 1 ? R.raw.adhan_fajr : R.raw.adhan_standard);
      player.setDataSource(file.getFileDescriptor(), file.getStartOffset(), file.getLength());
      file.close();
      player.setOnCompletionListener(p -> stopSelf());
      player.setOnErrorListener(
          (p, a, b) -> {
            stopSelf();
            return true;
          });
      player.prepare();
      player.start();
    } catch (Exception e) {
      stopSelf();
    }
    return START_NOT_STICKY;
  }

  private void release() {
    if (player != null) {
      player.release();
      player = null;
    }
    if (audio != null && Build.VERSION.SDK_INT >= 26 && focus != null) {
      audio.abandonAudioFocusRequest(focus);
      focus = null;
    } else if (audio != null && Build.VERSION.SDK_INT < 26) audio.abandonAudioFocus(focusListener);
  }

  @Override
  public void onDestroy() {
    release();
    super.onDestroy();
  }

  @Override
  public IBinder onBind(Intent intent) {
    return null;
  }
}
