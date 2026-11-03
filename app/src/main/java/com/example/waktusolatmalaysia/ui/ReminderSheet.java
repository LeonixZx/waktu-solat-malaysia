package com.example.waktusolatmalaysia.ui;

import androidx.fragment.app.Fragment;
import com.example.waktusolatmalaysia.preferences.*;
import com.example.waktusolatmalaysia.services.*;
import com.example.waktusolatmalaysia.views.MainActivity;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

public final class ReminderSheet {
  public static void show(Fragment fragment, int index, Runnable changed) {
    android.content.Context c = fragment.requireContext();
    String[] labels = {I18n.t(c, "Matikan"), I18n.t(c, "Pemberitahuan"), I18n.t(c, "Azan")};
    new MaterialAlertDialogBuilder(c)
        .setTitle(I18n.prayer(c, index))
        .setSingleChoiceItems(
            labels,
            PrayerMode.get(c, index).ordinal(),
            (dialog, which) -> {
              dialog.dismiss();
              if (which == 0) {
                PrayerMode.set(c, index, PrayerMode.OFF);
                changed.run();
              } else
                ((MainActivity) fragment.requireActivity())
                    .notificationPermission(
                        granted -> {
                          if (!fragment.isAdded()) return;
                          if (granted) {
                            PrayerMode.set(c, index, PrayerMode.values()[which]);
                            ScheduleRefreshWorker.enqueue(c, false);
                            changed.run();
                            if(which==PrayerMode.AZAN.ordinal()&&!ReminderScheduler.exact(c)
                                &&android.os.Build.VERSION.SDK_INT>=31)
                              new MaterialAlertDialogBuilder(c)
                                  .setTitle(I18n.choose(c,"Benarkan penggera tepat","Allow exact alarms"))
                                  .setMessage(I18n.choose(c,
                                      "Android memerlukan izin ini untuk azan automatik tepat pada waktunya. Tanpanya, notifikasi mungkin lewat dan anda mungkin perlu mengetik Mainkan azan.",
                                      "Android needs this permission for automatic adhan at the correct time. Otherwise notifications may be delayed and you may need to tap Play adhan."))
                                  .setPositiveButton(I18n.choose(c,"Buka tetapan","Open settings"),(d,w)->{
                                    try{fragment.startActivity(new android.content.Intent(android.provider.Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,
                                        android.net.Uri.parse("package:"+c.getPackageName())));}
                                    catch(android.content.ActivityNotFoundException unavailable){
                                      android.widget.Toast.makeText(c,I18n.choose(c,"Buka kebenaran penggera dalam tetapan Android.","Open alarm permissions in Android settings."),android.widget.Toast.LENGTH_LONG).show();
                                    }
                                  }).setNegativeButton(I18n.choose(c,"Kemudian","Later"),null).show();
                          } else
                            android.widget.Toast.makeText(
                                    c,
                                    I18n.t(c, "Kebenaran pemberitahuan diperlukan."),
                                    android.widget.Toast.LENGTH_LONG)
                                .show();
                        });
            })
        .setNegativeButton(I18n.t(c, "Tutup"), null)
        .show();
  }
}
