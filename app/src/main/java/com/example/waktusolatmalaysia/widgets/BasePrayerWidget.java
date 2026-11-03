package com.example.waktusolatmalaysia.widgets;

import android.appwidget.*;
import android.content.*;
import android.os.Bundle;
import com.example.waktusolatmalaysia.services.*;

public abstract class BasePrayerWidget extends AppWidgetProvider {
  @Override
  public void onUpdate(Context c, AppWidgetManager m, int[] ids) {
    PrayerWidgets.refreshAsync(c);
    WidgetScheduleRefreshWorker.enqueueAll(c);
    ScheduleRefreshWorker.enqueue(c, false);
    WidgetScheduleRefreshWorker.cleanup(c);
  }

  @Override
  public void onAppWidgetOptionsChanged(Context c, AppWidgetManager m, int id, Bundle opts) {
    PrayerWidgets.refreshAsync(c);
  }

  @Override
  public void onDeleted(Context c, int[] ids) {
    for (int id : ids) WidgetConfigV2.remove(c, id);
    PrayerWidgets.refreshAsync(c);
    WidgetScheduleRefreshWorker.cleanup(c);
  }

  @Override
  public void onDisabled(Context c) {
    PrayerWidgets.refreshAsync(c);
    WidgetScheduleRefreshWorker.cleanup(c);
  }

  @Override
  public void onRestored(Context c, int[] oldIds, int[] newIds) {
    android.content.SharedPreferences p = c.getSharedPreferences("widget_config", 0);
    android.content.SharedPreferences.Editor e = p.edit();
    for (int n = 0; n < Math.min(oldIds.length, newIds.length); n++) {
      String cfg = p.getString("" + oldIds[n], null);
      if (cfg != null) e.putString("" + newIds[n], cfg).remove("" + oldIds[n]);
    }
    e.commit();
    PrayerWidgets.refreshAsync(c);
    WidgetScheduleRefreshWorker.enqueueAll(c);
  }
}
