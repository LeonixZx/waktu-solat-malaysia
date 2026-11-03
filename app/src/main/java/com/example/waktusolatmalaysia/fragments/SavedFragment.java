package com.example.waktusolatmalaysia.fragments;

import android.os.Bundle;
import android.widget.*;
import androidx.annotation.Nullable;
import com.example.waktusolatmalaysia.R;
import com.example.waktusolatmalaysia.model.*;
import com.example.waktusolatmalaysia.preferences.*;
import com.example.waktusolatmalaysia.repo.*;
import com.example.waktusolatmalaysia.ui.*;
import java.util.*;

public final class SavedFragment extends FeatureFragment {
  @Override
  protected void build(@Nullable Bundle s) {
    heading("Simpanan", "Tambah bacaan atau video ke Simpanan.");
    LinearLayout list = Ui.column(requireContext());
    list.setId(R.id.saved_list);
    Ui.add(page, list, 16);
    task(
        () -> {
          ContentRepository repo = new ContentRepository(requireContext());
          Map<String, Reading> readings = new HashMap<>();
          for (String kind : new String[] {"doa", "wirid", "niat", "hadith"})
            for (Reading r : repo.readings(kind)) readings.put(r.id, r);
          return readings;
        },
        readings -> {
          UserStore store = new UserStore(requireContext());
          try {
            List<Surah> surahs = new ContentRepository(requireContext()).surahs();
            List<CatalogEntry> catalog = CatalogRepository.load(requireContext());
            for (String id : new TreeSet<>(store.savedIds())) {
              if (id.startsWith("surah:") || id.startsWith("ayah:")) {
                String[] parts = id.split(":");
                int n = Integer.parseInt(parts[1]),
                    v = parts.length > 2 ? Integer.parseInt(parts[2]) : 1;
                Surah found = surahs.get(n - 1);
                Ui.add(
                    list,
                    Ui.line(
                        requireContext(),
                        found.name + (parts.length > 2 ? " · Ayat " + v : ""),
                        "Al-Quran",
                        R.drawable.ic_quran,
                        () -> {
                          Bundle b = new Bundle();
                          b.putInt("surah", n);
                          b.putInt("verse", v);
                          open(R.id.quranReaderFragment, b);
                        }),
                    8);
              } else if (id.startsWith("catalog:")) {
                for (CatalogEntry entry : catalog)
                  if (id.equals("catalog:" + entry.id))
                    Ui.add(
                        list,
                        Ui.line(
                            requireContext(),
                            entry.title,
                            entry.provider,
                            entry.type.equals("article")
                                ? R.drawable.ic_article
                                : R.drawable.ic_video,
                            () -> {
                              if (entry.type.equals("article") || entry.youtubeId.isEmpty())
                                CatalogRepository.external(requireContext(), entry.url);
                              else {
                                Bundle b = new Bundle();
                                b.putString("id", entry.id);
                                open(R.id.videoFragment, b);
                              }
                            }),
                        8);
              } else {
                Reading r = readings.get(id);
                if (r != null)
                  Ui.add(
                      list,
                      Ui.line(
                          requireContext(),
                          r.title,
                          r.reference,
                          R.drawable.ic_bookmark,
                          () -> {
                            Bundle b = new Bundle();
                            b.putString("kind", r.kind);
                            b.putInt("index", Integer.parseInt(r.id.split(":")[1]));
                            open(R.id.readingFragment, b);
                          }),
                      8);
              }
            }
            if (list.getChildCount() == 0)
              Ui.add(
                  list,
                  Ui.text(
                      requireContext(), "Belum ada simpanan.", 15, false, R.color.text_secondary),
                  24);
          } catch (Exception e) {
            Ui.add(
                list,
                Ui.text(
                    requireContext(),
                    I18n.choose(
                        requireContext(),
                        "Simpanan belum dapat dibaca.",
                        "Saved items could not be read."),
                    14,
                    false,
                    R.color.text_secondary),
                12);
          }
        },
        e -> {});
  }
}
