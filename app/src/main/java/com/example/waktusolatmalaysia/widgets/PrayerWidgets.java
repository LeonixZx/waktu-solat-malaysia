package com.example.waktusolatmalaysia.widgets;

import android.app.*;
import android.appwidget.*;
import android.content.*;
import android.content.res.Configuration;
import android.graphics.Color;
import android.net.Uri;
import android.os.*;
import android.util.*;
import android.view.*;
import android.widget.*;
import com.example.waktusolatmalaysia.R;
import com.example.waktusolatmalaysia.model.*;
import com.example.waktusolatmalaysia.preferences.*;
import com.example.waktusolatmalaysia.repo.*;
import com.example.waktusolatmalaysia.services.*;
import com.example.waktusolatmalaysia.ui.I18n;
import com.example.waktusolatmalaysia.utils.*;
import com.example.waktusolatmalaysia.views.MainActivity;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import org.json.*;

/**
 * Disk-only renderer. Candidate selection measures real platform text, including accessibility
 * scaling.
 */
public final class PrayerWidgets {
  private static final ExecutorService WORK = Executors.newSingleThreadExecutor();
  private static final AtomicBoolean QUEUED = new AtomicBoolean();
  // Compatibility adapter for legacy callers. Production discovery uses component descriptors.
  public static final Class<?>[] PROVIDERS = {
    NextPrayerWidget.class,
    DayPrayerWidget.class,
    ExpandedPrayerWidget.class,
    NextPrayerStripWidget.class
  };

  public static JSONObject config(Context c, int id) {
    try {
      return new JSONObject(c.getSharedPreferences("widget_config", 0).getString("" + id, "{}"));
    } catch (Exception e) {
      return new JSONObject();
    }
  }

  public static PrayerZone zone(Context c, int id) {
    WidgetProviderDescriptor d = WidgetProviderDescriptor.instance(c, id);
    return WidgetConfigV2.read(c, id, d == null ? WidgetProviderDescriptor.NEXT : d)
        .resolvedZone(c);
  }

  public static Set<String> zones(Context c) {
    Set<String> zs = new HashSet<>();
    for (int id : WidgetProviderDescriptor.instances(c).keySet()) {
      PrayerZone z = zone(c, id);
      if (z != null) zs.add(z.code);
    }
    return zs;
  }

  public static void refreshAsync(Context c) {
    Context app = c.getApplicationContext();
    if (QUEUED.compareAndSet(false, true))
      WORK.execute(
          () -> {
            QUEUED.set(false);
            refresh(app);
          });
  }

  public static void refresh(Context c) {
    for (Map.Entry<Integer, WidgetProviderDescriptor> e :
        WidgetProviderDescriptor.instances(c).entrySet()) update(c, e.getKey(), e.getValue());
    scheduleBoundary(c);
  }

  public static void update(Context c, int id, int kind) {
    update(c, id, WidgetProviderDescriptor.legacy(kind));
  }

  public static void update(Context c, int id, WidgetProviderDescriptor d) {
    if (WidgetProviderDescriptor.instance(c, id) != d) return;
    AppWidgetManager manager = AppWidgetManager.getInstance(c);
    Bundle opts = manager.getAppWidgetOptions(id);
    WidgetConfigV2 cfg = WidgetConfigV2.read(c, id, d);
    if (cfg.legacy) cfg.save(c, id);
    if (Build.VERSION.SDK_INT >= 31) {
      ArrayList<SizeF> sizes = opts.getParcelableArrayList(AppWidgetManager.OPTION_APPWIDGET_SIZES);
      if (sizes != null && !sizes.isEmpty()) {
        Map<SizeF, RemoteViews> variants = new LinkedHashMap<>();
        for (SizeF size : sizes) {
          if (variants.size() == 16) break;
          if (!Float.isFinite(size.getWidth())
              || !Float.isFinite(size.getHeight())
              || size.getWidth() <= 0
              || size.getHeight() <= 0) continue;
          variants.put(size, render(c, id, d, cfg, size.getWidth(), size.getHeight(), false));
        }
        if (!variants.isEmpty()) {
          manager.updateAppWidget(id, new RemoteViews(variants));
          return;
        }
      }
    }
    int w = opts.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, d.width),
        h =
            opts.getInt(
                AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT,
                opts.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, d.height));
    int landscapeW = opts.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH, w),
        landscapeH = opts.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, h);
    w = positive(w, d.width);
    h = positive(h, d.height);
    landscapeW = positive(landscapeW, w);
    landscapeH = positive(landscapeH, h);
    manager.updateAppWidget(
        id,
        new RemoteViews(
            render(c, id, d, cfg, landscapeW, landscapeH, false),
            render(c, id, d, cfg, w, h, false)));
  }

  public static SizeF presentationSize(Context c, int id, WidgetProviderDescriptor d) {
    if (id <= 0) return new SizeF(d.width, d.height);
    Bundle opts = AppWidgetManager.getInstance(c).getAppWidgetOptions(id);
    boolean landscape =
        c.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE;
    return presentationSize(opts, landscape, d);
  }

  /** Choose a real host size pair, never a width/height combination absent from its size list. */
  public static SizeF presentationSize(Bundle opts, boolean landscape, WidgetProviderDescriptor d) {
    int width =
        opts.getInt(
            landscape
                ? AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH
                : AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH,
            d.width);
    int height =
        opts.getInt(
            landscape
                ? AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT
                : AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT,
            opts.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, d.height));
    SizeF target = new SizeF(positive(width, d.width), positive(height, d.height));
    if (Build.VERSION.SDK_INT >= 31) {
      ArrayList<SizeF> sizes = opts.getParcelableArrayList(AppWidgetManager.OPTION_APPWIDGET_SIZES);
      SizeF chosen = null;
      double distance = Double.MAX_VALUE;
      if (sizes != null)
        for (SizeF candidate : sizes) {
          if (candidate == null
              || !Float.isFinite(candidate.getWidth())
              || !Float.isFinite(candidate.getHeight())
              || candidate.getWidth() <= 0
              || candidate.getHeight() <= 0) continue;
          double delta =
              Math.pow(candidate.getWidth() - target.getWidth(), 2)
                  + Math.pow(candidate.getHeight() - target.getHeight(), 2);
          if (delta < distance) {
            distance = delta;
            chosen = candidate;
          }
        }
      if (chosen != null) return chosen;
    }
    return target;
  }

  private static int positive(int value, int fallback) {
    return value > 0 ? Math.min(2000, value) : fallback;
  }

  public static RemoteViews render(Context c, int id, int kind) {
    WidgetProviderDescriptor d = WidgetProviderDescriptor.legacy(kind);
    return render(c, id, d, WidgetConfigV2.read(c, id, d), d.width, d.height, false);
  }

  public static RemoteViews render(
      Context c,
      int id,
      WidgetProviderDescriptor d,
      WidgetConfigV2 cfg,
      float w,
      float h,
      boolean preview) {
    return render(
        c,
        id,
        d,
        cfg,
        WidgetRenderModel.load(c, cfg, d, ZonedDateTime.now(PrayerClock.MALAYSIA)),
        w,
        h,
        preview);
  }

  public static RemoteViews render(
      Context c,
      int id,
      WidgetProviderDescriptor d,
      WidgetConfigV2 cfg,
      WidgetRenderModel m,
      float w,
      float h,
      boolean preview) {
    if (cfg.layoutStyle.equals("timeline")) {
      RemoteViews timeline = TimelineWidgetRenderer.render(c, id, d, cfg, m, w, h, preview);
      if (timeline != null) return timeline;
    }
    boolean dark =
        cfg.theme.equals("dark")
            || cfg.theme.equals("system")
                && (android.content.res.Resources.getSystem().getConfiguration().uiMode
                        & Configuration.UI_MODE_NIGHT_MASK)
                    == Configuration.UI_MODE_NIGHT_YES;
    List<Candidate> candidates = new ArrayList<>();
    boolean noData =
        m.day == null
            || m.zone == null
            || d.family == WidgetProviderDescriptor.Family.NEXT && m.next == null;
    if (noData) candidates.add(new Candidate(R.layout.widget_state, false, false, false, false));
    else if (d.family == WidgetProviderDescriptor.Family.NEXT) {
      for (int layout :
          (w > h * 1.8f
              ? new int[] {
                R.layout.widget_next_inline, R.layout.widget_next_overview, R.layout.widget_next
              }
              : new int[] {
                R.layout.widget_next_overview, R.layout.widget_next, R.layout.widget_next_inline
              }))
        for (boolean full : new boolean[] {true, false})
          candidates.add(new Candidate(layout, false, false, false, full));
    } else if (d.family == WidgetProviderDescriptor.Family.DAILY) {
      for (int layout :
          d.preferStrip
              ? new int[] {
                R.layout.widget_strip, R.layout.widget_day_grid, R.layout.widget_day_list
              }
              : new int[] {R.layout.widget_day, R.layout.widget_day_grid, R.layout.widget_day_list})
        candidates.add(new Candidate(layout, false, false, false, false));
    } else {
      if (cfg.trackingEnabled)
        for (int layout :
            (h >= w * .85f
                ? new int[] {
                  R.layout.widget_expanded_grid_tracking,
                  R.layout.widget_expanded_dashboard_tracking,
                  R.layout.widget_expanded_tracking
                }
                : new int[] {
                  R.layout.widget_expanded_dashboard_tracking,
                  R.layout.widget_expanded_grid_tracking,
                  R.layout.widget_expanded_tracking
                }))
          for (int level = 3; level >= 1; level--)
            for (boolean full : new boolean[] {true, false})
              candidates.add(
                  new Candidate(
                      layout,
                      true,
                      cfg.showHijri && level >= 2,
                      cfg.showAuxiliary && level == 3,
                      full));
      for (int layout :
          (h >= w * .85f
              ? new int[] {
                R.layout.widget_expanded_grid,
                R.layout.widget_expanded_dashboard,
                R.layout.widget_expanded
              }
              : new int[] {
                R.layout.widget_expanded_dashboard,
                R.layout.widget_expanded_grid,
                R.layout.widget_expanded
              }))
        for (int level = 3; level >= 1; level--)
          for (boolean full : new boolean[] {true, false})
            candidates.add(
                new Candidate(
                    layout,
                    false,
                    cfg.showHijri && level >= 2,
                    cfg.showAuxiliary && level == 3,
                    full));
    }
    // Tracking has priority over decoration; within each mode retain icons before optional
    // metadata.
    for (boolean trackingStage : new boolean[] {true, false})
      for (boolean plain : new boolean[] {false, true})
        for (Candidate candidate : candidates) {
          if (candidate.tracking != trackingStage
              || plain && candidate.layout == R.layout.widget_state) continue;
          RemoteViews v = build(c, id, d, cfg, m, candidate, dark, preview);
          if (plain)
            for (int i = 0; i < 8; i++) v.setViewVisibility(res(c, "widget_icon_" + i), View.GONE);
          if (fits(c, v, w, h)) return v;
        }
    // Keep the selected family's actual information at normal phone launcher allocations.
    int[] compact =
        noData
            ? new int[] {R.layout.widget_state_compact}
            : d.family == WidgetProviderDescriptor.Family.NEXT
                ? new int[] {R.layout.widget_next_compact}
                : new int[] {
                  R.layout.widget_timetable_compact,
                  R.layout.widget_timetable_grid_compact,
                  R.layout.widget_timetable_list_compact,
                  R.layout.widget_timetable_micro
                };
    for (int layout : compact) {
      RemoteViews v =
          build(c, id, d, cfg, m, new Candidate(layout, false, false, false, false), dark, preview);
      if (fits(c, v, w, h)) return v;
    }
    RemoteViews resize =
        build(
            c,
            id,
            d,
            cfg,
            m,
            new Candidate(R.layout.widget_state, false, false, false, false),
            dark,
            preview);
    resize.setViewVisibility(R.id.widget_zone, View.GONE);
    resize.setViewVisibility(R.id.widget_date, View.GONE);
    resize.setViewVisibility(R.id.widget_logo, View.GONE);
    resize.setTextViewText(R.id.widget_message, I18n.choose(c, "Besarkan widget", "Resize widget"));
    resize.setBoolean(R.id.widget_message, "setSingleLine", false);
    resize.setViewVisibility(R.id.widget_detail, View.GONE);
    if (!fits(c, resize, w, h)) {
      resize = new RemoteViews(c.getPackageName(), R.layout.widget_resize);
      resize.setInt(
          R.id.widget_surface,
          "setBackgroundResource",
          dark ? R.drawable.widget_bg_dark : R.drawable.widget_bg_light);
      resize.setContentDescription(
          R.id.widget_root,
          I18n.choose(
              c,
              "Besarkan widget untuk melihat waktu solat. Ketik untuk tetapan.",
              "Resize widget to see prayer times. Tap for settings."));
    }
    resize.setContentDescription(
        R.id.widget_root,
        I18n.choose(
            c,
            "Besarkan widget untuk melihat waktu solat. Ketik untuk tetapan.",
            "Resize widget to see prayer times. Tap for settings."));
    if (!preview) resize.setOnClickPendingIntent(R.id.widget_root, configuration(c, id));
    return resize;
  }

  private static final class Candidate {
    final int layout;
    final boolean tracking, hijri, aux, fullZone;

    Candidate(int l, boolean t, boolean h, boolean a, boolean z) {
      layout = l;
      tracking = t;
      hijri = h;
      aux = a;
      fullZone = z;
    }
  }

  private static RemoteViews build(
      Context c,
      int id,
      WidgetProviderDescriptor d,
      WidgetConfigV2 cfg,
      WidgetRenderModel m,
      Candidate q,
      boolean dark,
      boolean preview) {
    RemoteViews v = new RemoteViews(c.getPackageName(), q.layout);
    boolean state = q.layout == R.layout.widget_state || q.layout == R.layout.widget_state_compact;
    boolean compact =
        q.layout == R.layout.widget_next_compact
            || q.layout == R.layout.widget_timetable_compact
            || q.layout == R.layout.widget_timetable_grid_compact
            || q.layout == R.layout.widget_timetable_list_compact
            || q.layout == R.layout.widget_timetable_micro;
    int text = Color.parseColor(dark ? "#F7EEDD" : "#28231E"),
        muted = Color.parseColor(dark ? "#C2B5A3" : "#62574D"),
        gold = Color.parseColor(dark ? "#E8B86D" : "#7A491A");
    v.setInt(
        R.id.widget_surface,
        "setBackgroundResource",
        dark ? R.drawable.widget_bg_dark : R.drawable.widget_bg_light);
    String zone =
        m.zone == null
            ? I18n.choose(c, "Pilih lokasi", "Choose location")
            : WidgetLocations.name(m.zone, !q.fullZone);
    v.setTextViewText(R.id.widget_zone, zone);
    v.setTextColor(R.id.widget_zone, muted);
    if (!q.fullZone)
      v.setTextViewTextSize(R.id.widget_zone, TypedValue.COMPLEX_UNIT_SP, 10f);
    LocalDate display = m.displayDate;
    if (d.family == WidgetProviderDescriptor.Family.NEXT
        && m.availability == WidgetRenderModel.Availability.MISSING_TOMORROW)
      display = m.today.plusDays(1);
    v.setTextViewText(R.id.widget_date, display.format(DateTimeFormatter.ofPattern("d/M/uuuu")));
    if (compact)
      v.setTextViewText(R.id.widget_date, display.format(DateTimeFormatter.ofPattern("d/M/yy")));
    v.setTextColor(R.id.widget_date, muted);
    String fullDate =
        display.format(
            DateTimeFormatter.ofPattern(
                "d MMMM uuuu", I18n.english(c) ? Locale.ENGLISH : Locale.forLanguageTag("ms")));
    v.setContentDescription(
        R.id.widget_zone, m.zone == null ? zone : m.zone.name);
    v.setContentDescription(R.id.widget_date, fullDate);
    DateTimeFormatter time =
        DateTimeFormatter.ofPattern(
            new AppPreferences(c).isTwelveHour() ? "h:mm a" : "HH:mm", Locale.ENGLISH);
    if (state) {
      String message =
          m.zone == null
              ? I18n.choose(c, "Pilih zon widget", "Choose widget zone")
              : m.availability == WidgetRenderModel.Availability.MISSING_TOMORROW
                  ? I18n.choose(c, "Jadual esok belum tersedia", "Tomorrow's times unavailable")
                  : I18n.choose(c, "Jadual belum tersedia", "Schedule unavailable");
      v.setTextViewText(R.id.widget_message, message);
      v.setBoolean(R.id.widget_message, "setSingleLine", false);
      v.setTextColor(R.id.widget_message, text);
      v.setTextViewText(
          R.id.widget_detail, I18n.choose(c, "Buka untuk muat jadual", "Open to load times"));
      v.setBoolean(R.id.widget_detail, "setSingleLine", false);
      v.setTextColor(R.id.widget_detail, muted);
    } else if (d.family == WidgetProviderDescriptor.Family.NEXT) {
      v.setTextViewText(R.id.widget_next, I18n.prayer(c, m.next.index));
      v.setTextColor(R.id.widget_next, text);
      v.setContentDescription(
          R.id.widget_next,
          I18n.choose(c, "Solat seterusnya: ", "Next prayer: ")
              + I18n.prayer(c, m.next.index)
              + ", "
              + fullDate);
      v.setTextViewText(R.id.widget_time, m.next.time.format(time));
      v.setTextColor(R.id.widget_time, gold);
      if (q.layout != R.layout.widget_next_inline) {
        v.setTextViewText(R.id.widget_kicker, I18n.choose(c, "SETERUSNYA", "UP NEXT"));
        v.setTextColor(R.id.widget_kicker, muted);
      }
    }
    if (q.layout != R.layout.widget_state
        && (d.family != WidgetProviderDescriptor.Family.NEXT
            || q.layout == R.layout.widget_next_overview)) {
      PrayerDay shown =
          d.family == WidgetProviderDescriptor.Family.NEXT && m.next.tomorrow ? m.tomorrow : m.day;
      int[] indices =
          d.family == WidgetProviderDescriptor.Family.EXPANDED
              ? new int[] {0, 1, 2, 3, 4, 5, 6, 7}
              : PrayerDay.OBLIGATORY;
      int pos = 0;
      String[] canonical = {"fajr", "dhuhr", "asr", "maghrib", "isha"};
      for (int index : indices) {
        int name = res(c, "widget_name_" + index),
            value = res(c, "widget_value_" + index),
            cell = res(c, "widget_cell_" + index);
        boolean next =
            m.next != null
                && (d.family == WidgetProviderDescriptor.Family.NEXT || !m.next.tomorrow)
                && m.next.index == index;
        v.setTextViewText(name, I18n.prayer(c, index));
        v.setTextColor(name, text);
        v.setTextViewText(value, shown.times.get(index).format(time));
        if (q.layout == R.layout.widget_strip
            || q.layout == R.layout.widget_timetable_compact
            || q.layout == R.layout.widget_timetable_grid_compact
            || q.layout == R.layout.widget_timetable_micro) {
          boolean twelve = new AppPreferences(c).isTwelveHour();
          int period = res(c, "widget_period_" + index);
          v.setViewVisibility(period, twelve ? View.VISIBLE : View.GONE);
          v.setTextColor(period, muted);
          if (twelve) {
            v.setTextViewText(
                value,
                shown.times.get(index).format(DateTimeFormatter.ofPattern("h:mm", Locale.ENGLISH)));
            v.setTextViewText(
                period,
                shown.times.get(index).format(DateTimeFormatter.ofPattern("a", Locale.ENGLISH)));
          }
        }

        v.setTextColor(value, next ? (dark ? gold : Color.parseColor("#8C581E")) : text);
        if (next)
          v.setInt(
              cell,
              "setBackgroundResource",
              dark ? R.drawable.widget_highlight_dark : R.drawable.widget_highlight_light);
        v.setContentDescription(
            value,
            (next ? I18n.choose(c, "Solat seterusnya, ", "Next prayer, ") : "")
                + I18n.prayer(c, index)
                + ", "
                + shown.times.get(index).format(time));
        int icon = res(c, "widget_icon_" + index);
        v.setImageViewResource(
            icon, resDrawable(c, "widget_prayer_" + index + (dark ? "_dark" : "")));
        if (q.tracking && Arrays.binarySearch(PrayerDay.OBLIGATORY, index) >= 0) {
          String prayer = canonical[pos++];
          boolean done = Boolean.TRUE.equals(m.tracking.get(prayer));
          int check = res(c, "widget_check_" + index);
          v.setImageViewResource(
              check,
              dark
                  ? (done ? R.drawable.widget_check_on_dark : R.drawable.widget_check_off_dark)
                  : (done ? R.drawable.widget_check_on_light : R.drawable.widget_check_off_light));
          v.setInt(
              check,
              "setBackgroundResource",
              dark ? R.drawable.widget_action_dark : R.drawable.widget_action_light);
          v.setContentDescription(
              check,
              I18n.prayer(c, index)
                  + I18n.choose(
                      c,
                      done
                          ? ", direkod. Ketik untuk batal."
                          : ", belum direkod. Ketik untuk rekod.",
                      done ? ", recorded. Tap to unrecord." : ", unrecorded. Tap to record."));
          if (preview) v.setBoolean(check, "setEnabled", false);
          if (!preview)
            v.setOnClickPendingIntent(
                check, WidgetTrackingReceiver.pending(c, id, m.today, prayer, !done));
        }
      }
      if (d.family != WidgetProviderDescriptor.Family.NEXT && !q.tracking) {
        String label =
            m.next == null
                ? I18n.choose(c, "Jadual esok belum tersedia", "Tomorrow's times unavailable")
                : I18n.choose(c, "Seterusnya · ", "Next · ")
                    + I18n.prayer(c, m.next.index)
                    + "  "
                    + m.next.time.format(time)
                    + (m.next.tomorrow ? I18n.choose(c, " · esok", " · tomorrow") : "");
        v.setTextViewText(R.id.widget_status, label);
        v.setTextColor(R.id.widget_status, muted);
        if (d.preferStrip || compact) {
          v.setViewVisibility(R.id.widget_status, View.GONE);
          v.setTextViewText(
              R.id.widget_zone,
              zone
                  + (compact || m.next == null
                      ? ""
                      : I18n.choose(c, " · Seterusnya ", " · Next ")
                          + I18n.prayer(c, m.next.index)));
        }
      }
      if (d.family == WidgetProviderDescriptor.Family.EXPANDED) {
        String hijri = WidgetDates.hijri(m.day.hijri, I18n.english(c));
        v.setViewVisibility(
            R.id.widget_hijri, q.hijri && !hijri.isEmpty() ? View.VISIBLE : View.GONE);
        v.setTextViewText(R.id.widget_hijri, hijri);
        v.setTextColor(R.id.widget_hijri, muted);
        v.setContentDescription(R.id.widget_hijri, "Hijri (JAKIM): " + hijri);
        if (q.hijri && !hijri.isEmpty())
          v.setContentDescription(
              R.id.widget_date,
              I18n.choose(c, "Masihi: ", "Gregorian: ") + fullDate
                  + ", Hijri (JAKIM): " + WidgetDates.hijri(m.day.hijri, I18n.english(c)));
        v.setViewVisibility(R.id.widget_aux, q.aux ? View.VISIBLE : View.GONE);
        v.setViewVisibility(
            R.id.widget_hint, cfg.trackingEnabled && !q.tracking ? View.VISIBLE : View.GONE);
        v.setTextViewText(
            R.id.widget_hint,
            I18n.choose(c, "Besarkan untuk rekod solat", "Resize to track prayers"));
        v.setBoolean(R.id.widget_hint, "setSingleLine", false);
        v.setTextColor(R.id.widget_hint, muted);
        v.setContentDescription(
            R.id.widget_settings, I18n.choose(c, "Tetapan widget", "Widget settings"));
        v.setInt(
            R.id.widget_settings,
            "setBackgroundResource",
            dark ? R.drawable.widget_action_dark : R.drawable.widget_action_light);
        if (preview) v.setBoolean(R.id.widget_settings, "setEnabled", false);
        if (!preview) v.setOnClickPendingIntent(R.id.widget_settings, configuration(c, id));
      }
    }
    StringBuilder spoken =
        new StringBuilder(m.zone == null ? zone : m.zone.name)
            .append(", ")
            .append(fullDate);
    if (state)
      spoken
          .append(", ")
          .append(
              I18n.choose(
                  c,
                  "Jadual belum tersedia. Buka untuk muat jadual.",
                  "Schedule unavailable. Open to load times."));
    else if (d.family == WidgetProviderDescriptor.Family.NEXT)
      spoken
          .append(", ")
          .append(I18n.choose(c, "Solat seterusnya: ", "Next prayer: "))
          .append(I18n.prayer(c, m.next.index))
          .append(" ")
          .append(m.next.time.format(time));
    else
      for (int index : PrayerDay.OBLIGATORY)
        spoken
            .append(", ")
            .append(I18n.prayer(c, index))
            .append(" ")
            .append(m.day.times.get(index).format(time));
    if (q.layout == R.layout.widget_next_overview) {
      PrayerDay table = m.next.tomorrow ? m.tomorrow : m.day;
      for (int index : PrayerDay.OBLIGATORY)
        spoken
            .append(", ")
            .append(I18n.prayer(c, index))
            .append(" ")
            .append(table.times.get(index).format(time));
    }
    if (d.family != WidgetProviderDescriptor.Family.NEXT && m.next != null)
      spoken
          .append(", ")
          .append(I18n.choose(c, "Solat seterusnya: ", "Next prayer: "))
          .append(I18n.prayer(c, m.next.index))
          .append(" ")
          .append(m.next.time.format(time))
          .append(m.next.tomorrow ? I18n.choose(c, ", esok", ", tomorrow") : "");
    v.setContentDescription(R.id.widget_root, spoken.toString());
    if (!preview) {
      Intent open =
          new Intent(c, MainActivity.class)
              .putExtra("open_schedule", true)
              .putExtra("date", display.toString())
              .putExtra("zone", m.zone == null ? "" : m.zone.code)
              .putExtra("zone_name", m.zone == null ? "" : m.zone.name)
              .setData(Uri.parse("wsm://widget/" + id + "/" + display));
      v.setOnClickPendingIntent(
          R.id.widget_root,
          m.zone == null
              ? configuration(c, id)
              : PendingIntent.getActivity(
                  c, id, open, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE));
    }
    return v;
  }

  private static int resDrawable(Context c, String name) {
    return c.getResources().getIdentifier(name, "drawable", c.getPackageName());
  }

  private static int res(Context c, String name) {
    return c.getResources().getIdentifier(name, "id", c.getPackageName());
  }

  public static PendingIntent configuration(Context c, int id) {
    return PendingIntent.getActivity(
        c,
        id,
        new Intent(c, WidgetConfigActivity.class)
            .putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, id)
            .setData(Uri.parse("wsm://widget-config/" + id)),
        PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
  }

  /** Measure framework font layout, not a fontScale threshold or a guessed character count. */
  public static boolean fits(Context c, RemoteViews remote, float width, float height) {
    try {
      FrameLayout parent = new FrameLayout(c);
      View view = remote.apply(c, parent);
      int w =
          Math.round(
              TypedValue.applyDimension(
                  TypedValue.COMPLEX_UNIT_DIP, width, c.getResources().getDisplayMetrics()));
      int h =
          Math.round(
              TypedValue.applyDimension(
                  TypedValue.COMPLEX_UNIT_DIP, height, c.getResources().getDisplayMetrics()));
      view.measure(
          View.MeasureSpec.makeMeasureSpec(w, View.MeasureSpec.EXACTLY),
          View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED));
      view.layout(0, 0, w, view.getMeasuredHeight());
      return view.getMeasuredHeight() <= h && textFits(view);
    } catch (RuntimeException error) {
      android.util.Log.w("WidgetRenderer", "Candidate unavailable", error);
      return false;
    }
  }

  private static boolean textFits(View view) {
    if (view.getVisibility() == View.GONE) return true;
    if (view instanceof TextView) {
      TextView t = (TextView) view;
      int width = t.getWidth() - t.getCompoundPaddingLeft() - t.getCompoundPaddingRight();
      if (t.getMaxLines() == 1 && t.getPaint().measureText(t.getText().toString()) > width + .5f)
        return false;
      if (t.getLayout() != null
          && t.getLayout().getHeight()
              > t.getHeight() - t.getCompoundPaddingTop() - t.getCompoundPaddingBottom() + 1)
        return false;
    }
    if (view instanceof ViewGroup)
      for (int i = 0; i < ((ViewGroup) view).getChildCount(); i++)
        if (!textFits(((ViewGroup) view).getChildAt(i))) return false;
    return true;
  }

  private static void scheduleBoundary(Context c) {
    PendingIntent pi =
        PendingIntent.getBroadcast(
            c,
            700,
            new Intent(c, WidgetRefreshReceiver.class).setAction("wsm.WIDGET_BOUNDARY"),
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    Set<String> zs = zones(c);
    if (zs.isEmpty()) {
      ((AlarmManager) c.getSystemService(Context.ALARM_SERVICE)).cancel(pi);
      return;
    }
    ZonedDateTime now = ZonedDateTime.now(PrayerClock.MALAYSIA),
        boundary = now.toLocalDate().plusDays(1).atStartOfDay(PrayerClock.MALAYSIA);
    for (String z : zs)
      for (PrayerDay day :
          new PrayerRepository(c).cachedDays(z, now.toLocalDate(), now.toLocalDate().plusDays(1)))
        for (int i : PrayerDay.OBLIGATORY) {
          ZonedDateTime t = day.date.atTime(day.times.get(i)).atZone(PrayerClock.MALAYSIA);
          if (t.isAfter(now) && t.isBefore(boundary)) boundary = t;
        }
    ReminderScheduler.schedule(c, pi, boundary.plusSeconds(1));
  }
}
