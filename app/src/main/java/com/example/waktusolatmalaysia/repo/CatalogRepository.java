package com.example.waktusolatmalaysia.repo;

import android.content.*;
import android.net.Uri;
import androidx.browser.customtabs.CustomTabsIntent;
import com.example.waktusolatmalaysia.model.*;
import java.util.*;
import org.json.*;

public final class CatalogRepository {
  public static List<CatalogEntry> load(Context c) throws Exception {
    JSONArray entries =
        new JSONObject(new ContentRepository(c).asset("catalog.json")).getJSONArray("entries");
    List<CatalogEntry> result = new ArrayList<>();
    for (int i = 0; i < entries.length(); i++) {
      CatalogEntry e = new CatalogEntry(entries.getJSONObject(i));
      if (!allowed(e.url)) throw new IllegalArgumentException("Unexpected catalog provider");
      result.add(e);
    }
    return result;
  }

  public static boolean allowed(String url) {
    try {
      Uri u = Uri.parse(url);
      String h = u.getHost();
      return "https".equals(u.getScheme())
          && Arrays.asList(
                  "www.youtube.com",
                  "youtube.com",
                  "yaqeeninstitute.org",
                  "muftiwp.gov.my",
                  "www.islam.gov.my")
              .contains(h);
    } catch (Exception e) {
      return false;
    }
  }

  public static void external(Context c, String url) {
    if (!allowed(url)) return;
    try {
      new CustomTabsIntent.Builder().setShowTitle(true).build().launchUrl(c, Uri.parse(url));
    } catch (ActivityNotFoundException e) {
      c.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
    }
  }
}
