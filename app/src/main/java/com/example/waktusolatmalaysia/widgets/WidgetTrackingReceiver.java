package com.example.waktusolatmalaysia.widgets;

import android.app.*;
import android.content.*;
import android.net.Uri;
import com.example.waktusolatmalaysia.preferences.PrayerTrackingStore;
import com.example.waktusolatmalaysia.utils.PrayerClock;
import java.time.*;
import java.util.concurrent.*;

/** Explicit, idempotent actions; all validation and writes are serialized. */
public final class WidgetTrackingReceiver extends BroadcastReceiver {
  private static final ExecutorService WRITES = Executors.newSingleThreadExecutor();

  public static PendingIntent pending(
      Context c, int id, LocalDate date, String prayer, boolean done) {
    Intent i =
        new Intent(c, WidgetTrackingReceiver.class)
            .setData(Uri.parse("wsm://widget-track/" + id + "/" + date + "/" + prayer + "/" + done))
            .putExtra("widgetId", id)
            .putExtra("date", date.toString())
            .putExtra("prayer", prayer)
            .putExtra("completed", done);
    return PendingIntent.getBroadcast(
        c, id, i, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
  }

  @Override
  public void onReceive(Context c, Intent i) {
    PendingResult p = goAsync();
    WRITES.execute(
        () -> {
          try {
            apply(c, i, LocalDate.now(PrayerClock.MALAYSIA));
          } finally {
            p.finish();
          }
        });
  }

  public static boolean apply(Context c, Intent i, LocalDate today) {
    int id = i.getIntExtra("widgetId", -1);
    WidgetProviderDescriptor d = WidgetProviderDescriptor.instance(c, id);
    if (d != WidgetProviderDescriptor.EXPANDED || !WidgetConfigV2.read(c, id, d).trackingEnabled)
      return false;
    try {
      LocalDate date = LocalDate.parse(i.getStringExtra("date"));
      if (!today.equals(date)) {
        PrayerWidgets.refreshAsync(c);
        return false;
      }
      if (!i.hasExtra("completed")) return false;
      return new PrayerTrackingStore(c)
          .set(date, i.getStringExtra("prayer"), i.getBooleanExtra("completed", false));
    } catch (RuntimeException bad) {
      return false;
    }
  }
}
