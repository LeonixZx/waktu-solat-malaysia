package com.example.waktusolatmalaysia.model;

import org.json.*;

public final class CatalogEntry {
  public final String id, type, provider, language, title, summary, url, thumbnail, youtubeId;

  public CatalogEntry(JSONObject j) throws JSONException {
    id = j.getString("id");
    type = j.getString("type");
    provider = j.getString("provider");
    language = j.getString("language");
    title = j.getString("title");
    summary = j.getString("summary");
    url = j.getString("url");
    thumbnail = j.optString("thumbnail");
    youtubeId = j.optString("youtubeId");
  }
}
