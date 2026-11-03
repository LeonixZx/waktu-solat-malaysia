package com.example.waktusolatmalaysia.widgets;

import android.content.*;
import com.example.waktusolatmalaysia.services.*;

public final class WidgetRefreshReceiver extends BroadcastReceiver {
  @Override
  public void onReceive(Context c, Intent i) {
    PendingResult done = goAsync();
    new Thread(
            () -> {
              try {
                PrayerWidgets.refresh(c);
                if (i.getBooleanExtra("network", false))
                  com.example.waktusolatmalaysia.services.WidgetScheduleRefreshWorker.enqueueAll(c);
              } finally {
                done.finish();
              }
            },
            "widget-refresh")
        .start();
  }
}
