package com.example.waktusolatmalaysia.repo;

import android.content.Context;
import com.example.waktusolatmalaysia.model.PrayerZone;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.json.JSONArray;
import org.json.JSONObject;

public final class ZoneRepository {
  private ZoneRepository() {}

  public static Map<String, List<PrayerZone>> load(Context context) {
    Map<String, List<PrayerZone>> states = new LinkedHashMap<>();
    try (InputStream stream = context.getAssets().open("prayer_zones.json")) {
      java.io.ByteArrayOutputStream bytes = new java.io.ByteArrayOutputStream();
      byte[] buffer = new byte[4096];
      int count;
      while ((count = stream.read(buffer)) != -1) bytes.write(buffer, 0, count);
      JSONArray data =
          new JSONObject(new String(bytes.toByteArray(), StandardCharsets.UTF_8))
              .getJSONArray("zones");
      for (int i = 0; i < data.length(); i++) {
        JSONObject row = data.getJSONObject(i);
        String stateName = row.getString("state");
        String code = row.getString("code");
        String name = row.getString("districts");
        if (!code.matches("[A-Z]{3}[0-9]{2}") || name.isEmpty())
          throw new IllegalStateException("Invalid official prayer zone");
        List<PrayerZone> zones = states.get(stateName);
        if (zones == null) {
          zones = new ArrayList<>();
          states.put(stateName, zones);
        }
        zones.add(new PrayerZone(stateName, name, code));
      }
    } catch (Exception exception) {
      throw new IllegalStateException("Cannot read bundled prayer zones", exception);
    }
    return states;
  }
}
