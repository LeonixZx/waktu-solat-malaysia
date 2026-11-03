package com.example.waktusolatmalaysia.ui;

import android.app.*;
import android.content.*;
import android.media.AudioManager;
import android.net.Uri;
import android.os.*;
import android.provider.Settings;
import android.widget.*;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import com.example.waktusolatmalaysia.R;
import com.example.waktusolatmalaysia.services.*;
import com.example.waktusolatmalaysia.views.MainActivity;
import com.google.android.material.button.MaterialButton;

/** Live device checks and explicit user-triggered tests, using the real notification/audio paths. */
public final class ReminderDiagnostics extends LinearLayout {
  private final Fragment fragment;
  private final TextView status;
  private final Handler handler=new Handler(Looper.getMainLooper());
  private final Runnable tick=new Runnable(){public void run(){refresh();handler.postDelayed(this,1000);}};
  public ReminderDiagnostics(Fragment fragment) {
    super(fragment.requireContext());this.fragment=fragment;setOrientation(VERTICAL);
    status=Ui.text(getContext(),"",13,false,R.color.text_secondary);
    status.setId(R.id.alert_health);Ui.add(this,status,8);
    button(I18n.choose(getContext(),"Uji notifikasi","Test notification"),R.id.alert_test_notification,()->
      ((MainActivity)fragment.requireActivity()).notificationPermission(granted->{
        if(!fragment.isAdded())return;
        if(granted&&PrayerNotifications.allowed(getContext(),PrayerNotifications.PRAYER)){
          getContext().getSystemService(NotificationManager.class).notify(9001,
              PrayerNotifications.builder(getContext(),PrayerNotifications.PRAYER)
                  .setContentTitle(I18n.choose(getContext(),"Ujian notifikasi","Notification test"))
                  .setContentText(I18n.choose(getContext(),"Notifikasi Waktu Solat Malaysia berfungsi.","Waktu Solat Malaysia notifications are working."))
                  .setContentIntent(PrayerNotifications.open(getContext())).setAutoCancel(true).build());
          AlertStatus.set(getContext(),"notification","test_posted");
        }else Toast.makeText(getContext(),I18n.choose(getContext(),"Benarkan notifikasi dalam tetapan Android.","Allow notifications in Android settings."),Toast.LENGTH_LONG).show();
        refresh();
      }));
    button(I18n.choose(getContext(),"Uji azan · 5 saat","Test adhan · 5 seconds"),R.id.alert_test_azan,()->{
      try{ContextCompat.startForegroundService(getContext(),new Intent(getContext(),AzanPlaybackService.class)
          .setAction("test").putExtra("index",4));}
      catch(RuntimeException failure){AlertStatus.set(getContext(),"azan","background_start_blocked");}
      refresh();
    });
    button(I18n.choose(getContext(),"Hentikan azan","Stop adhan"),R.id.alert_stop_azan,()->
        getContext().startService(new Intent(getContext(),AzanPlaybackService.class).setAction("stop")));
    button(I18n.choose(getContext(),"Tetapan notifikasi Android","Android notification settings"),R.id.alert_notification_settings,()->
      open(Build.VERSION.SDK_INT>=26?new Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
          .putExtra(Settings.EXTRA_APP_PACKAGE,getContext().getPackageName()):appSettings()));
    if(Build.VERSION.SDK_INT>=31)button(I18n.choose(getContext(),"Benarkan penggera tepat","Allow exact alarms"),R.id.alert_exact_settings,()->
        open(new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,Uri.parse("package:"+getContext().getPackageName()))));
    button(I18n.choose(getContext(),"Kelantangan penggera & Jangan Ganggu","Alarm volume & Do Not Disturb"),R.id.alert_sound_settings,()->open(new Intent(Settings.ACTION_SOUND_SETTINGS)));
    refresh();
  }
  private Intent appSettings(){return new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,Uri.parse("package:"+getContext().getPackageName()));}
  private void open(Intent intent){try{fragment.startActivity(intent);}catch(ActivityNotFoundException e){fragment.startActivity(appSettings());}}
  private void button(String label,int id,Runnable action){MaterialButton b=Ui.quiet(getContext(),label,R.drawable.ic_bell);b.setId(id);b.setOnClickListener(v->action.run());Ui.add(this,b,4);}
  public void refresh(){
    Context c=getContext();PrayerNotifications.channels(c);
    AudioManager audio=(AudioManager)c.getSystemService(Context.AUDIO_SERVICE);
    int volume=audio.getStreamVolume(AudioManager.STREAM_ALARM),max=audio.getStreamMaxVolume(AudioManager.STREAM_ALARM);
    NotificationManager manager=c.getSystemService(NotificationManager.class);
    String text=I18n.choose(c,"Notifikasi solat: ","Prayer notifications: ")
        +I18n.choose(c,PrayerNotifications.allowed(c,PrayerNotifications.PRAYER)?"Dibenarkan":"Disekat",PrayerNotifications.allowed(c,PrayerNotifications.PRAYER)?"Allowed":"Blocked")
        +"\n"+I18n.choose(c,"Notifikasi azan: ","Adhan notifications: ")
        +I18n.choose(c,PrayerNotifications.allowed(c,PrayerNotifications.AZAN)?"Dibenarkan":"Disekat",PrayerNotifications.allowed(c,PrayerNotifications.AZAN)?"Allowed":"Blocked")
        +"\n"+I18n.choose(c,"Penggera: ","Alarms: ")+I18n.choose(c,ReminderScheduler.exact(c)?"Tepat":"Anggaran — mungkin lewat",ReminderScheduler.exact(c)?"Exact":"Approximate — may be delayed")
        +"\n"+I18n.choose(c,"Kelantangan azan (penggera): ","Adhan volume (alarm): ")+volume+"/"+max
        +"\n"+I18n.choose(c,"Jangan Ganggu: ","Do Not Disturb: ")
        +(manager.getCurrentInterruptionFilter()==NotificationManager.INTERRUPTION_FILTER_ALL?I18n.choose(c,"Tidak aktif","Off"):I18n.choose(c,"Semak pengecualian penggera","Check alarm exceptions"));
    String state=AlertStatus.get(c,"azan");
    if(!state.isEmpty())text+="\n"+I18n.choose(c,"Azan: ","Adhan: ")+describe(c,state);
    text+="\n"+I18n.choose(c,"Azan automatik memerlukan penggera tepat pada Android baharu. Kelantangan penggera, Jangan Ganggu dan sekatan bateri peranti boleh menjejaskan audio. Tetapan ini tidak diubah secara automatik.",
        "Automatic adhan needs exact alarms on newer Android. Alarm volume, Do Not Disturb and device battery restrictions can affect audio. These settings are not changed automatically.");
    if(!text.contentEquals(status.getText()))status.setText(text);
  }
  public static String describe(Context c,String state){
    switch(state){
      case "playing":return I18n.choose(c,"Sedang dimainkan","Playing");
      case "preparing":return I18n.choose(c,"Menyediakan audio","Preparing audio");
      case "alarm_volume_zero":return I18n.choose(c,"Kelantangan penggera ialah sifar","Alarm volume is zero");
      case "audio_focus_denied":case "audio_focus_lost":return I18n.choose(c,"Audio digunakan aplikasi lain; cuba semula","Audio focus unavailable; retry");
      case "background_start_blocked":return I18n.choose(c,"Android menyekat audio latar. Benarkan penggera tepat atau ketik Mainkan azan pada notifikasi.","Android blocked background playback. Allow exact alarms or tap Play adhan in the notification.");
      case "playback_error":return I18n.choose(c,"Audio gagal dimainkan; cuba ujian semula","Playback failed; retry the test");
      default:return I18n.choose(c,"Selesai / dihentikan","Finished / stopped");
    }
  }
  @Override protected void onAttachedToWindow(){super.onAttachedToWindow();handler.post(tick);}
  @Override protected void onDetachedFromWindow(){handler.removeCallbacksAndMessages(null);super.onDetachedFromWindow();}
}
