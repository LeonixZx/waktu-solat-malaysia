package com.example.waktusolatmalaysia.widgets;

import android.appwidget.*;
import android.content.*;
import java.util.*;

/** Component identity is stable; presentation never depends on array position. */
public final class WidgetProviderDescriptor {
  public enum Family {
    NEXT,
    DAILY,
    EXPANDED
  }

  public final Class<? extends BasePrayerWidget> component;
  public final Family family;
  public final boolean preferStrip;
  public final int width, height;
  public final String ms, en, size;

  private WidgetProviderDescriptor(
      Class<? extends BasePrayerWidget> c,
      Family f,
      boolean strip,
      int w,
      int h,
      String ms,
      String en,
      String size) {
    component = c;
    family = f;
    preferStrip = strip;
    width = w;
    height = h;
    this.ms = ms;
    this.en = en;
    this.size = size;
  }

  public static final WidgetProviderDescriptor NEXT =
      new WidgetProviderDescriptor(
          NextPrayerWidget.class,
          Family.NEXT,
          false,
          180,
          180,
          "Solat seterusnya",
          "Next prayer",
          "2×2");
  public static final WidgetProviderDescriptor STRIP =
      new WidgetProviderDescriptor(
          NextPrayerStripWidget.class,
          Family.DAILY,
          true,
          360,
          120,
          "Bar waktu solat",
          "Prayer strip",
          "4×1");
  public static final WidgetProviderDescriptor DAILY =
      new WidgetProviderDescriptor(
          DayPrayerWidget.class,
          Family.DAILY,
          false,
          360,
          180,
          "Waktu hari ini",
          "Today's prayers",
          "4×2");
  public static final WidgetProviderDescriptor EXPANDED =
      new WidgetProviderDescriptor(
          ExpandedPrayerWidget.class,
          Family.EXPANDED,
          false,
          320,
          400,
          "Harian Plus",
          "Daily Plus",
          "4×4");
  public static final List<WidgetProviderDescriptor> ALL =
      Collections.unmodifiableList(Arrays.asList(NEXT, STRIP, DAILY, EXPANDED));

  public static WidgetProviderDescriptor legacy(int kind) {
    switch (kind) {
      case 1:
        return DAILY;
      case 2:
        return EXPANDED;
      case 3:
        return STRIP;
      default:
        return NEXT;
    }
  }

  public static WidgetProviderDescriptor component(ComponentName name) {
    if (name != null)
      for (WidgetProviderDescriptor d : ALL)
        if (d.component.getName().equals(name.getClassName())) return d;
    return null;
  }

  public static WidgetProviderDescriptor instance(Context c, int id) {
    AppWidgetProviderInfo info = AppWidgetManager.getInstance(c).getAppWidgetInfo(id);
    return info == null ? null : component(info.provider);
  }

  public static Map<Integer, WidgetProviderDescriptor> instances(Context c) {
    Map<Integer, WidgetProviderDescriptor> ids = new LinkedHashMap<>();
    AppWidgetManager m = AppWidgetManager.getInstance(c);
    for (WidgetProviderDescriptor d : ALL)
      for (int id : m.getAppWidgetIds(new ComponentName(c, d.component))) ids.put(id, d);
    return ids;
  }
}
