package com.example.waktusolatmalaysia.widgets;

import android.content.*;
import com.example.waktusolatmalaysia.model.PrayerZone;
import com.example.waktusolatmalaysia.preferences.AppPreferences;
import com.example.waktusolatmalaysia.repo.ZoneRepository;
import java.util.*;
import org.json.*;

/** Immutable draft/configuration. Parsing and previews never persist changes. */
public final class WidgetConfigV2 {
  private final JSONObject original;
  private final WidgetProviderDescriptor descriptor;
  static final Object CONFIG_LOCK = new Object();
  public final boolean followApp, showHijri, showAuxiliary, trackingEnabled;
  public final String zone, name, state, theme, layoutStyle;
  public final boolean legacy;

  public WidgetConfigV2(JSONObject json, WidgetProviderDescriptor d) {
    descriptor = d;
    try {
      original = new JSONObject(json.toString());
    } catch (JSONException impossible) {
      throw new IllegalArgumentException(impossible);
    }
    legacy = json.optInt("schemaVersion", 1) < 2;
    followApp = json.optBoolean("followApp", true);
    zone = json.optString("zone", "");
    name = json.optString("name", "");
    state = json.optString("state", "");
    String chosen = json.optString("theme", "system");
    theme = Arrays.asList("system", "light", "dark").contains(chosen) ? chosen : "system";
    // Existing instances retain their verified presentation. Empty configurations are new widgets.
    layoutStyle =
        json.optString("layoutStyle", json.length() == 0 ? "timeline" : "classic")
                .equals("timeline")
            ? "timeline"
            : "classic";
    boolean expanded = d.family == WidgetProviderDescriptor.Family.EXPANDED;
    showHijri = json.optBoolean("showHijri", expanded);
    showAuxiliary = json.optBoolean("showAuxiliary", expanded);
    trackingEnabled = expanded && !legacy && json.optBoolean("trackingEnabled", false);
  }

  public static WidgetConfigV2 read(Context c, int id, WidgetProviderDescriptor d) {
    JSONObject data;
    try {
      data =
          new JSONObject(
              c.getSharedPreferences("widget_config", 0).getString(String.valueOf(id), "{}"));
    } catch (Exception e) {
      data = new JSONObject();
    }
    return new WidgetConfigV2(data, d);
  }

  public JSONObject json() {
    try {
      JSONObject j = new JSONObject(original.toString());
      j.put("schemaVersion", 2)
          .put("layoutStyle", layoutStyle)
          .put("followApp", followApp)
          .put("zone", zone)
          .put("name", name)
          .put("state", state)
          .put("theme", theme)
          .put("showHijri", showHijri)
          .put("showAuxiliary", showAuxiliary)
          .put("trackingEnabled", trackingEnabled);
      return j;
    } catch (JSONException impossible) {
      throw new IllegalStateException(impossible);
    }
  }

  public void save(Context c, int id) {
    synchronized (CONFIG_LOCK) {
      // A queued renderer must not recreate configuration after launcher deletion.
      if (WidgetProviderDescriptor.instance(c, id) != descriptor) return;
      c.getSharedPreferences("widget_config", 0)
          .edit()
          .putString(String.valueOf(id), json().toString())
          .commit();
    }
  }

  public static void remove(Context c, int id) {
    synchronized (CONFIG_LOCK) {
      c.getSharedPreferences("widget_config", 0).edit().remove(String.valueOf(id)).commit();
    }
  }

  public PrayerZone resolvedZone(Context c) {
    if (followApp) return new AppPreferences(c).zone();
    for (List<PrayerZone> group : ZoneRepository.load(c).values())
      for (PrayerZone z : group)
        if (z.code.equals(zone))
          return new PrayerZone(
              state.isEmpty() ? z.state : state, name.isEmpty() ? z.name : name, z.code);
    return null;
  }
}
