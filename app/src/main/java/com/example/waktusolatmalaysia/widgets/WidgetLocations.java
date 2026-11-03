package com.example.waktusolatmalaysia.widgets;

import com.example.waktusolatmalaysia.model.PrayerZone;

/** Human-readable widget labels; official zone codes remain internal schedule identifiers. */
public final class WidgetLocations {
  private WidgetLocations() {}

  public static String name(PrayerZone zone, boolean compact) {
    if (zone == null) return "";
    String name = zone.name == null ? "" : zone.name.split(",", 2)[0].trim();
    if (name.isEmpty()) name = zone.state == null ? "" : zone.state.trim();
    if (!compact) return name;
    name = name.split("(?i)\\s+dan\\s+|\\s+&\\s+", 2)[0].trim();
    if (name.length() > 14)
      name = name.replaceFirst("(?i)^Kuala\\s+", "K. ")
          .replaceFirst("(?i)^Tanjung\\s+", "Tg. ")
          .replaceFirst("(?i)^Sungai\\s+", "Sg. ")
          .replaceFirst("(?i)^Kampung\\s+", "Kg. ")
          .replaceFirst("(?i)^Pulau\\s+", "P. ")
          .replaceFirst("(?i)^Bukit\\s+", "Bkt. ");
    return name;
  }
}
