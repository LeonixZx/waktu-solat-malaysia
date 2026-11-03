package com.example.waktusolatmalaysia;

import static org.junit.Assert.*;
import static org.junit.Assume.assumeTrue;

import android.app.*;
import android.content.*;
import android.net.Uri;
import android.os.*;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import com.example.waktusolatmalaysia.model.*;
import com.example.waktusolatmalaysia.preferences.*;
import com.example.waktusolatmalaysia.repo.*;
import com.example.waktusolatmalaysia.services.*;
import java.time.*;
import java.util.*;
import org.junit.*;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class AlarmDeliveryTest {
  @Test
  public void exactTaskDeliversOnceAndDeletedTaskDoesNotNotify() throws Exception {
    assumeTrue(
        "Opt-in emulator notification check",
        InstrumentationRegistry.getArguments().getString("alarms", "").equals("true"));
    Context c = InstrumentationRegistry.getInstrumentation().getTargetContext();
    List<PrayerRelativeReminder> before = new TaskStore(c).load();
    NotificationManager notifications = c.getSystemService(NotificationManager.class);
    AlarmManager alarms = c.getSystemService(AlarmManager.class);
    String id = "delivery-test";
    PendingIntent pending = null;
    try {
      new TaskStore(c)
          .put(
              new PrayerRelativeReminder(
                  id, "Delivery verification", 1, 0, Set.of(1, 2, 3, 4, 5, 6, 7), true));
      ReminderScheduler.scheduleNow(c);
      UserStore store = new UserStore(c);
      long when = System.currentTimeMillis() + 3000;
      Intent intent =
          new Intent(c, TaskAlarmReceiver.class)
              .setData(Uri.parse("wsm://verification/delivery"))
              .putExtra("id", id)
              .putExtra("anchor_date", LocalDate.now().toString())
              .putExtra("when", when)
              .putExtra("zone", new AppPreferences(c).zone().code)
              .putExtra("generation", store.text("alarm_generation", ""));
      pending =
          PendingIntent.getBroadcast(
              c, 30003, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
      ReminderScheduler.schedule(
          c, pending, Instant.ofEpochMilli(when).atZone(ZoneId.of("Asia/Kuala_Lumpur")));
      long end = SystemClock.elapsedRealtime() + 15000;
      boolean delivered = false;
      do {
        for (var n : notifications.getActiveNotifications())
          if (("task:" + id).equals(n.getTag())) delivered = true;
        if (delivered) break;
        SystemClock.sleep(100);
      } while (SystemClock.elapsedRealtime() < end);
      assertTrue("Task notification must be delivered through AlarmManager", delivered);
      notifications.cancel("task:" + id, 1);
      new TaskStore(c).remove(id);
      intent
          .putExtra("when", System.currentTimeMillis())
          .putExtra("generation", store.text("alarm_generation", ""));
      c.sendBroadcast(intent);
      SystemClock.sleep(300);
      for (var n : notifications.getActiveNotifications())
        assertNotEquals("task:" + id, n.getTag());
    } finally {
      if (pending != null) {
        alarms.cancel(pending);
        pending.cancel();
      }
      notifications.cancel("task:" + id, 1);
      new TaskStore(c).replace(before);
      c.getSharedPreferences("personal_library", 0).edit().remove("delivered_task_" + id).commit();
      ReminderScheduler.scheduleNow(c);
    }
  }
}
