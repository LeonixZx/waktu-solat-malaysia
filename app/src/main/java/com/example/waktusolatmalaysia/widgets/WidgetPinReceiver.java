package com.example.waktusolatmalaysia.widgets;

import android.content.*;
import android.widget.Toast;
import com.example.waktusolatmalaysia.ui.I18n;
import java.util.*;

/** Immutable callback: confirm a newly bound component using the pre-request ID set. */
public final class WidgetPinReceiver extends BroadcastReceiver {
  @Override
  public void onReceive(Context c, Intent intent) {
    WidgetProviderDescriptor d =
        WidgetProviderDescriptor.component(
            new ComponentName(c.getPackageName(), intent.getStringExtra("component")));
    if (d == null) return;
    Set<Integer> before = new HashSet<>();
    int[] prior = intent.getIntArrayExtra("before");
    if (prior != null) for (int id : prior) before.add(id);
    for (java.util.Map.Entry<Integer, WidgetProviderDescriptor> e :
        WidgetProviderDescriptor.instances(c).entrySet())
      if (e.getValue() == d && !before.contains(e.getKey())) {
        PrayerWidgets.refreshAsync(c);
        Toast.makeText(
                c,
                I18n.choose(
                    c,
                    "Widget ditambah. Tetapan tersedia dalam Widget saya.",
                    "Widget added. Configure it in My widgets."),
                Toast.LENGTH_LONG)
            .show();
        return;
      }
  }
}
