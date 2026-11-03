package com.example.waktusolatmalaysia.widgets;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Color;
import android.net.Uri;
import android.view.View;
import android.widget.RemoteViews;
import com.example.waktusolatmalaysia.R;
import com.example.waktusolatmalaysia.model.PrayerDay;
import com.example.waktusolatmalaysia.preferences.AppPreferences;
import com.example.waktusolatmalaysia.ui.I18n;
import com.example.waktusolatmalaysia.views.MainActivity;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Original framework-only presentation; schedule, configuration and tracking contracts stay shared.
 */
final class TimelineWidgetRenderer {
  private static final int[] SIX = {1, 2, 4, 5, 6, 7};
  private static final String[] CANONICAL = {"fajr", "dhuhr", "asr", "maghrib", "isha"};

  static RemoteViews render(
      Context c,
      int id,
      WidgetProviderDescriptor d,
      WidgetConfigV2 cfg,
      WidgetRenderModel m,
      float width,
      float height,
      boolean preview) {
    if (m.zone == null || m.day == null || d == WidgetProviderDescriptor.NEXT && m.next == null)
      return null;
    boolean dark =
        cfg.theme.equals("dark")
            || cfg.theme.equals("system")
                && (Resources.getSystem().getConfiguration().uiMode
                        & Configuration.UI_MODE_NIGHT_MASK)
                    == Configuration.UI_MODE_NIGHT_YES;
    List<Shape> shapes = new ArrayList<>();
    if (d == WidgetProviderDescriptor.NEXT) {
      if (width >= 280)
        shapes.add(new Shape(R.layout.widget_timeline_next_arc, false, false, false));
      shapes.add(new Shape(R.layout.widget_timeline_next, false, false, false));
    } else if (d.preferStrip) {
      shapes.add(new Shape(R.layout.widget_timeline_band, false, false, false));
      shapes.add(new Shape(R.layout.widget_timeline_compact, false, false, false));
      shapes.add(new Shape(R.layout.widget_timeline_micro, false, false, false));
    } else if (d == WidgetProviderDescriptor.DAILY) {
      shapes.add(new Shape(R.layout.widget_timeline_rectangle, false, false, false));
      shapes.add(new Shape(R.layout.widget_timeline_rectangle_compact, false, false, false));
      shapes.add(new Shape(R.layout.widget_timeline_band, false, false, false));
      shapes.add(new Shape(R.layout.widget_timeline_compact, false, false, false));
      shapes.add(new Shape(R.layout.widget_timeline_micro, false, false, false));
    } else {
      for (boolean tracking :
          cfg.trackingEnabled ? new boolean[] {true, false} : new boolean[] {false})
        for (int level = 2; level >= 0; level--) {
          if (width >= 280)
            shapes.add(
                new Shape(
                    tracking
                        ? R.layout.widget_timeline_square_tracking
                        : R.layout.widget_timeline_square,
                    tracking,
                    cfg.showHijri && level > 0,
                    cfg.showAuxiliary && level == 2));
          shapes.add(
              new Shape(
                  tracking ? R.layout.widget_timeline_grid_tracking : R.layout.widget_timeline_grid,
                  tracking,
                  cfg.showHijri && level > 0,
                  cfg.showAuxiliary && level == 2));
        }
    }
    for (Shape shape : shapes)
      for (boolean fullLocation : new boolean[] {true, false}) {
        RemoteViews result = bind(c, id, d, cfg, m, shape, dark, fullLocation, preview);
        if (PrayerWidgets.fits(c, result, width, height)) return result;
      }
    // The verified classic candidates retain the same family's data at constrained bounds.
    return null;
  }

  private static final class Shape {
    final int layout;
    final boolean tracking, hijri, auxiliary;

    Shape(int layout, boolean tracking, boolean hijri, boolean auxiliary) {
      this.layout = layout;
      this.tracking = tracking;
      this.hijri = hijri;
      this.auxiliary = auxiliary;
    }

    boolean compact() {
      return layout == R.layout.widget_timeline_compact
          || layout == R.layout.widget_timeline_micro
          || layout == R.layout.widget_timeline_rectangle_compact;
    }

    boolean band() {
      return layout == R.layout.widget_timeline_compact
          || layout == R.layout.widget_timeline_micro
          || layout == R.layout.widget_timeline_band;
    }
  }

  private static RemoteViews bind(
      Context c,
      int id,
      WidgetProviderDescriptor d,
      WidgetConfigV2 cfg,
      WidgetRenderModel m,
      Shape shape,
      boolean dark,
      boolean fullLocation,
      boolean preview) {
    RemoteViews v = new RemoteViews(c.getPackageName(), shape.layout);
    boolean nextFamily = d == WidgetProviderDescriptor.NEXT;
    PrayerDay shown = nextFamily && m.next.tomorrow ? m.tomorrow : m.day;
    int ink = Color.parseColor(dark ? "#F7EEDD" : "#28231E");
    int muted = Color.parseColor(dark ? "#C2B5A3" : "#62574D");
    int accent = Color.parseColor(dark ? "#E8B86D" : "#7A491A");
    String suffix = dark ? "dark" : "light";
    Locale locale = I18n.english(c) ? Locale.ENGLISH : Locale.forLanguageTag("ms");
    DateTimeFormatter fullTime =
        DateTimeFormatter.ofPattern(
            new AppPreferences(c).isTwelveHour() ? "h:mm a" : "HH:mm", Locale.ENGLISH);
    boolean twelve = new AppPreferences(c).isTwelveHour();
    String location = WidgetLocations.name(m.zone, !fullLocation);
    v.setInt(
        R.id.widget_surface,
        "setBackgroundResource",
        dark ? R.drawable.widget_timeline_plate_dark : R.drawable.widget_timeline_plate_light);
    v.setTextViewText(
        R.id.widget_zone,
        location);
    v.setTextColor(R.id.widget_zone, ink);
    if (shape.compact())
      v.setTextViewTextSize(R.id.widget_zone, android.util.TypedValue.COMPLEX_UNIT_SP, 9f);
    v.setTextViewText(R.id.widget_brand, fullLocation ? "Waktu Solat Malaysia" : "Waktu Solat");
    v.setTextColor(R.id.widget_brand, accent);
    v.setTextViewText(
        R.id.widget_date,
        shown.date.format(
            DateTimeFormatter.ofPattern(
                shape.compact() || shape.layout == R.layout.widget_timeline_next
                    ? "d/M/yy"
                    : "d MMM uuuu",
                locale)));
    v.setTextColor(R.id.widget_date, muted);
    String hijri = WidgetDates.hijri(shown.hijri, I18n.english(c));
    boolean compactDaily =
        d == WidgetProviderDescriptor.DAILY
            && shape.layout == R.layout.widget_timeline_rectangle_compact;
    if (compactDaily && !hijri.isEmpty()) {
      v.setTextViewText(
          R.id.widget_date, shown.date.format(DateTimeFormatter.ofPattern("d MMM uuuu", locale)));
    }
    v.setTextViewText(R.id.widget_hijri, hijri);
    v.setTextColor(R.id.widget_hijri, muted);
    if (d == WidgetProviderDescriptor.EXPANDED)
      v.setViewVisibility(
          R.id.widget_hijri, shape.hijri && !hijri.isEmpty() ? View.VISIBLE : View.GONE);
    else if (d == WidgetProviderDescriptor.DAILY && !shape.band())
      v.setViewVisibility(
          R.id.widget_hijri, hijri.isEmpty() ? View.GONE : View.VISIBLE);
    String dateDescription =
        I18n.choose(c, "Masihi: ", "Gregorian: ")
            + shown.date.format(DateTimeFormatter.ofPattern("d MMMM uuuu", locale));
    if (!hijri.isEmpty())
      dateDescription +=
          ", Hijri (JAKIM): " + WidgetDates.hijri(shown.hijri, I18n.english(c));
    v.setContentDescription(R.id.widget_date, dateDescription);
    v.setContentDescription(R.id.widget_hijri, "Hijri (JAKIM): " + hijri);
    v.setTextViewText(R.id.widget_location, location);
    v.setTextColor(R.id.widget_location, muted);
    int[] entries =
        shape.band()
            ? SIX
            : d == WidgetProviderDescriptor.EXPANDED
                ? new int[] {0, 1, 2, 3, 4, 5, 6, 7}
                : PrayerDay.OBLIGATORY;
    StringBuilder spoken =
        new StringBuilder(m.zone.name)
            .append(", ")
            .append(dateDescription);
    for (int index : entries) {
      boolean next = m.next != null && (nextFamily || !m.next.tomorrow) && m.next.index == index;
      String name = I18n.prayer(c, index);
      int value = resource(c, "widget_value_" + index);
      v.setTextViewText(resource(c, "widget_name_" + index), name);
      v.setTextColor(resource(c, "widget_name_" + index), ink);
      v.setTextViewText(
          value,
          shown
              .times
              .get(index)
              .format(DateTimeFormatter.ofPattern(twelve ? "h:mm" : "HH:mm", Locale.ENGLISH)));
      v.setTextColor(value, next ? accent : ink);
      int period = resource(c, "widget_period_" + index);
      v.setViewVisibility(period, twelve ? View.VISIBLE : View.GONE);
      v.setTextViewText(
          period, shown.times.get(index).format(DateTimeFormatter.ofPattern("a", Locale.ENGLISH)));
      v.setTextColor(period, muted);
      String description =
          (next ? I18n.choose(c, "Seterusnya, ", "Next, ") : "")
              + name
              + ", "
              + shown.times.get(index).format(fullTime);
      v.setContentDescription(value, description);
      spoken.append(", ").append(description);
      int icon = resource(c, "widget_icon_" + index);
      v.setImageViewResource(
          icon, drawable(c, "widget_prayer_" + index + (dark || next ? "_dark" : "")));
      v.setInt(
          icon,
          "setBackgroundResource",
          drawable(c, "widget_timeline_ring_" + suffix + (next ? "_active" : "")));
      if (next && shape.band())
        v.setInt(
            resource(c, "widget_cell_" + index),
            "setBackgroundResource",
            dark ? R.drawable.widget_highlight_dark : R.drawable.widget_highlight_light);
      int position = Arrays.binarySearch(PrayerDay.OBLIGATORY, index);
      if (shape.tracking && position >= 0) {
        String canonical = CANONICAL[position];
        boolean recorded = Boolean.TRUE.equals(m.tracking.get(canonical));
        int check = resource(c, "widget_check_" + index);
        v.setImageViewResource(
            check, drawable(c, "widget_check_" + (recorded ? "on_" : "off_") + suffix));
        v.setInt(
            check,
            "setBackgroundResource",
            dark ? R.drawable.widget_action_dark : R.drawable.widget_action_light);
        v.setContentDescription(
            check,
            name
                + I18n.choose(
                    c,
                    recorded
                        ? ", direkod. Ketik untuk batal."
                        : ", belum direkod. Ketik untuk rekod.",
                    recorded ? ", recorded. Tap to unrecord." : ", unrecorded. Tap to record."));
        if (preview) v.setBoolean(check, "setEnabled", false);
        else
          v.setOnClickPendingIntent(
              check, WidgetTrackingReceiver.pending(c, id, m.today, canonical, !recorded));
      }
    }
    String nextLabel =
        m.next == null
            ? I18n.choose(c, "Jadual esok belum tersedia", "Tomorrow's times unavailable")
            : I18n.choose(c, "Seterusnya · ", "Next · ")
                + I18n.prayer(c, m.next.index)
                + "  "
                + m.next.time.format(fullTime)
                + (m.next.tomorrow ? I18n.choose(c, " · esok", " · tomorrow") : "");
    if (nextFamily) {
      v.setTextViewText(R.id.widget_kicker, I18n.choose(c, "SOLAT SETERUSNYA", "NEXT PRAYER"));
      v.setTextColor(R.id.widget_kicker, muted);
      v.setTextViewText(R.id.widget_next, I18n.prayer(c, m.next.index));
      v.setTextColor(R.id.widget_next, ink);
      v.setTextViewText(R.id.widget_time, m.next.time.format(fullTime));
      v.setTextColor(R.id.widget_time, accent);
    } else {
      v.setTextViewText(R.id.widget_status, nextLabel);
      v.setTextColor(R.id.widget_status, accent);
      spoken.append(", ").append(nextLabel);
    }
    if (d == WidgetProviderDescriptor.EXPANDED) {
      v.setViewVisibility(R.id.widget_aux, shape.auxiliary ? View.VISIBLE : View.GONE);
      v.setViewVisibility(
          R.id.widget_hint, cfg.trackingEnabled && !shape.tracking ? View.VISIBLE : View.GONE);
      v.setTextViewText(
          R.id.widget_hint,
          I18n.choose(c, "Besarkan untuk rekod solat", "Resize to track prayers"));
      v.setTextColor(R.id.widget_hint, muted);
      v.setInt(
          R.id.widget_settings,
          "setBackgroundResource",
          dark ? R.drawable.widget_action_dark : R.drawable.widget_action_light);
      v.setContentDescription(
          R.id.widget_settings, I18n.choose(c, "Tetapan widget", "Widget settings"));
      if (preview) v.setBoolean(R.id.widget_settings, "setEnabled", false);
      else v.setOnClickPendingIntent(R.id.widget_settings, PrayerWidgets.configuration(c, id));
    }
    v.setContentDescription(R.id.widget_root, spoken.toString());
    if (!preview) {
      Intent open =
          new Intent(c, MainActivity.class)
              .putExtra("open_schedule", true)
              .putExtra("date", shown.date.toString())
              .putExtra("zone", m.zone.code)
              .putExtra("zone_name", m.zone.name)
              .setData(Uri.parse("wsm://widget/" + id + "/" + shown.date));
      v.setOnClickPendingIntent(
          R.id.widget_root,
          PendingIntent.getActivity(
              c, id, open, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE));
    }
    return v;
  }

  private static int resource(Context c, String name) {
    return c.getResources().getIdentifier(name, "id", c.getPackageName());
  }

  private static int drawable(Context c, String name) {
    return c.getResources().getIdentifier(name, "drawable", c.getPackageName());
  }
}
