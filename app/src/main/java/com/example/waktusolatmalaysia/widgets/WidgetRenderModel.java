package com.example.waktusolatmalaysia.widgets;

import android.content.Context;
import com.example.waktusolatmalaysia.model.*;
import com.example.waktusolatmalaysia.preferences.PrayerTrackingStore;
import com.example.waktusolatmalaysia.repo.PrayerRepository;
import com.example.waktusolatmalaysia.utils.PrayerClock;
import java.time.*;
import java.util.*;

/** Cached, immutable widget inputs. Loading never performs network I/O. */
public final class WidgetRenderModel {
  public enum Availability {
    READY,
    MISSING_TODAY,
    MISSING_TOMORROW,
    INVALID_ZONE
  }

  public final PrayerZone zone;
  public final LocalDate today, displayDate;
  public final PrayerDay day, tomorrow;
  public final PrayerClock.Next next;
  public final Map<String, Boolean> tracking;
  public final Availability availability;

  public WidgetRenderModel(
      PrayerZone zone,
      PrayerDay day,
      PrayerDay tomorrow,
      ZonedDateTime now,
      WidgetProviderDescriptor d,
      Map<String, Boolean> tracking) {
    this.zone = zone;
    this.day = day;
    this.tomorrow = tomorrow;
    today = now.withZoneSameInstant(PrayerClock.MALAYSIA).toLocalDate();
    next = PrayerClock.next(day, tomorrow, now.withZoneSameInstant(PrayerClock.MALAYSIA));
    displayDate =
        d.family == WidgetProviderDescriptor.Family.NEXT
                && day != null
                && (next == null || next.tomorrow)
            ? today.plusDays(1)
            : today;
    this.tracking = Collections.unmodifiableMap(new LinkedHashMap<>(tracking));
    availability =
        zone == null
            ? Availability.INVALID_ZONE
            : day == null
                ? Availability.MISSING_TODAY
                : next == null ? Availability.MISSING_TOMORROW : Availability.READY;
  }

  public static WidgetRenderModel load(
      Context c, WidgetConfigV2 cfg, WidgetProviderDescriptor d, ZonedDateTime now) {
    PrayerZone zone = cfg.resolvedZone(c);
    LocalDate date = now.withZoneSameInstant(PrayerClock.MALAYSIA).toLocalDate();
    PrayerDay today = null, tomorrow = null;
    if (zone != null)
      for (PrayerDay p : new PrayerRepository(c).cachedDays(zone.code, date, date.plusDays(1)))
        if (p.date.equals(date)) today = p;
        else if (p.date.equals(date.plusDays(1))) tomorrow = p;
    Map<String, Boolean> tracking =
        cfg.trackingEnabled ? new PrayerTrackingStore(c).read(date) : Collections.emptyMap();
    return new WidgetRenderModel(zone, today, tomorrow, now, d, tracking);
  }
}
