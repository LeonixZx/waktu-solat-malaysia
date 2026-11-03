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
