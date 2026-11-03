package com.example.waktusolatmalaysia;

import static org.junit.Assert.*;
import static org.junit.Assume.assumeTrue;

import android.content.*;
import android.os.SystemClock;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import com.example.waktusolatmalaysia.repo.ContentRepository;
import com.example.waktusolatmalaysia.services.QuranDownloadWorker;
import java.io.*;
import java.util.*;
import org.junit.*;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class DownloadQueueTest {
  @Test
  public void queueDownloadsValidatedTextAndDeletionKeepsBookmarks() throws Exception {
    assumeTrue(
        "Opt-in emulator download check",
        InstrumentationRegistry.getArguments().getString("downloads", "").equals("true"));
    Context c = InstrumentationRegistry.getInstrumentation().getTargetContext();
    var prefs = c.getSharedPreferences("quran_downloads", 0);
    var before = new HashMap<>(prefs.getAll());
    File file = new File(c.getFilesDir(), "quran/surah_114.json");
    byte[] old = file.exists() ? java.nio.file.Files.readAllBytes(file.toPath()) : null;
    var bookmarks = new HashMap<>(c.getSharedPreferences("personal_library", 0).getAll());
    try {
      prefs.edit().putString("pending", "[114]").commit();
      QuranDownloadWorker.start(c, Collections.emptyList(), true);
      long end = SystemClock.elapsedRealtime() + 45000;
      while (!QuranDownloadWorker.pending(c).isEmpty() && SystemClock.elapsedRealtime() < end)
        SystemClock.sleep(100);
      assertTrue("Resumed queue must complete", QuranDownloadWorker.pending(c).isEmpty());
      assertEquals(6, new ContentRepository(c).verses(114, "en").size());
      assertEquals(6, new ContentRepository(c).verses(114, "ms").size());
      QuranDownloadWorker.delete(c, 114);
      assertFalse(file.exists());
      assertEquals(
          bookmarks.get("saved"),
          c.getSharedPreferences("personal_library", 0).getAll().get("saved"));
    } finally {
      QuranDownloadWorker.cancel(c);
      V3FeaturesTest.restorePrefs(prefs, before);
      if (old != null)
        try (var out = new FileOutputStream(file)) {
          out.write(old);
        }
    }
  }
}
