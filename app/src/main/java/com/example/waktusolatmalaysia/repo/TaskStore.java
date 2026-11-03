package com.example.waktusolatmalaysia.repo;

import android.content.Context;
import android.util.AtomicFile;
import com.example.waktusolatmalaysia.model.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import org.json.*;

public final class TaskStore {
  private final AtomicFile file;

  public TaskStore(Context c) {
    file = new AtomicFile(new File(c.getFilesDir(), "personal_tasks.json"));
  }

  public List<PrayerRelativeReminder> load() throws Exception {
    synchronized (TaskStore.class) {
      List<PrayerRelativeReminder> result = new ArrayList<>();
      if (!file.getBaseFile().exists()) return result;
      JSONObject root = new JSONObject(new String(file.readFully(), StandardCharsets.UTF_8));
      if (root.getInt("schemaVersion") != 1) throw new IOException("Unsupported task version");
      JSONArray data = root.getJSONArray("data");
      for (int i = 0; i < data.length(); i++)
        result.add(PrayerRelativeReminder.parse(data.getJSONObject(i)));
      return result;
    }
  }

  public List<PrayerRelativeReminder> safeLoad() {
    try {
      return load();
    } catch (Exception e) {
      return Collections.emptyList();
    }
  }

  public void put(PrayerRelativeReminder task) throws Exception {
    synchronized (TaskStore.class) {
      List<PrayerRelativeReminder> tasks = load();
      tasks.removeIf(t -> t.id.equals(task.id));
      tasks.add(task);
      replace(tasks);
    }
  }

  public void remove(String id) throws Exception {
    synchronized (TaskStore.class) {
      List<PrayerRelativeReminder> tasks = load();
      tasks.removeIf(t -> t.id.equals(id));
      replace(tasks);
    }
  }

  public void replace(List<PrayerRelativeReminder> tasks) throws Exception {
    synchronized (TaskStore.class) {
      JSONArray data = new JSONArray();
      for (PrayerRelativeReminder t : tasks) data.put(t.json());
      FileOutputStream out = null;
      try {
        out = file.startWrite();
        out.write(
            new JSONObject()
                .put("schemaVersion", 1)
                .put("data", data)
                .toString()
                .getBytes(StandardCharsets.UTF_8));
        file.finishWrite(out);
      } catch (Exception e) {
        if (out != null) file.failWrite(out);
        throw e;
      }
    }
  }
}
