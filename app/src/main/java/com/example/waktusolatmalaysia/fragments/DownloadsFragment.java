package com.example.waktusolatmalaysia.fragments;

import android.os.Bundle;
import android.widget.*;
import androidx.annotation.Nullable;
import androidx.work.WorkManager;
import com.example.waktusolatmalaysia.R;
import com.example.waktusolatmalaysia.model.*;
import com.example.waktusolatmalaysia.repo.*;
import com.example.waktusolatmalaysia.services.*;
import com.example.waktusolatmalaysia.ui.*;
import com.google.android.material.checkbox.MaterialCheckBox;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import java.io.File;
import java.util.*;

public final class DownloadsFragment extends FeatureFragment {
  private TextView status;
  private Spinner surahs;
  private MaterialCheckBox cellular;

  @Override
  protected void build(@Nullable Bundle s) {
    heading(
        "Muat turun",
        I18n.choose(
            requireContext(),
            "Teks Arab dan terjemahan untuk bacaan luar talian. Audio distrim apabila dimainkan.",
            "Arabic and translation text for offline reading. Audio is streamed when played."));
    status = Ui.text(requireContext(), "", 14, false, R.color.prayer_text);
    status.setId(R.id.download_status);
    Ui.add(page, status, 20);
    cellular = new MaterialCheckBox(requireContext());
    cellular.setText(
        I18n.choose(requireContext(), "Benarkan data mudah alih", "Allow mobile data"));
    Ui.add(page, cellular, 12);
    android.view.View all =
        Ui.button(requireContext(), "Semua teks Al-Quran", R.drawable.ic_download);
    all.setId(R.id.download_all);
    all.setOnClickListener(
        v -> {
          List<Integer> values = new ArrayList<>();
          for (int i = 1; i <= 114; i++) values.add(i);
          QuranDownloadWorker.start(requireContext(), values, cellular.isChecked());
          update();
        });
    Ui.add(page, all, 12);
    surahs = new Spinner(requireContext());
    try {
      List<Surah> data = new ContentRepository(requireContext()).surahs();
      List<String> names = new ArrayList<>();
      for (Surah surah : data) names.add(surah.number + " · " + surah.name);
      surahs.setAdapter(
          new ArrayAdapter<>(
              requireContext(), android.R.layout.simple_spinner_dropdown_item, names));
    } catch (Exception ignored) {
    }
    Ui.add(page, surahs, 16);
    android.view.View selected =
        Ui.quiet(requireContext(), "Muat turun teks", R.drawable.ic_download);
    selected.setOnClickListener(
        v -> {
          QuranDownloadWorker.start(
              requireContext(),
              Collections.singletonList(surahs.getSelectedItemPosition() + 1),
              cellular.isChecked());
          update();
        });
    Ui.add(page, selected, 8);
    android.view.View pause =
        Ui.quiet(
            requireContext(),
            I18n.choose(requireContext(), "Jeda / batal giliran", "Pause download queue"),
            R.drawable.ic_pause);
    pause.setOnClickListener(
        v -> {
          QuranDownloadWorker.cancel(requireContext());
          update();
        });
    Ui.add(page, pause, 8);
    android.view.View resume =
        Ui.quiet(
            requireContext(),
            I18n.choose(requireContext(), "Sambung muat turun", "Resume downloads"),
            R.drawable.ic_play);
    resume.setOnClickListener(
        v -> {
          QuranDownloadWorker.start(
              requireContext(), Collections.emptyList(), cellular.isChecked());
          update();
        });
    Ui.add(page, resume, 8);
    android.view.View delete =
        Ui.quiet(
            requireContext(),
            I18n.choose(requireContext(), "Padam teks surah pilihan", "Delete selected surah text"),
            R.drawable.ic_delete);
    delete.setOnClickListener(
        v ->
            new MaterialAlertDialogBuilder(requireContext())
                .setMessage(
                    I18n.choose(
                        requireContext(),
                        "Teks boleh dimuat turun semula. Penanda bacaan dikekalkan.",
                        "Text can be downloaded again. Bookmarks are preserved."))
                .setNegativeButton(I18n.t(requireContext(), "Batal"), null)
                .setPositiveButton(
                    I18n.t(requireContext(), "Padam"),
                    (d, w) -> {
                      int n = surahs.getSelectedItemPosition() + 1;
                      QuranDownloadWorker.delete(requireContext(), n);
                      update();
                    })
                .show());
    Ui.add(page, delete, 16);
    WorkManager.getInstance(requireContext())
        .getWorkInfosForUniqueWorkLiveData("quran_text_download")
        .observe(getViewLifecycleOwner(), work -> update());
    update();
  }

  private void update() {
    if (status == null) return;
    File[] files =
        new File(requireContext().getFilesDir(), "quran")
            .listFiles((dir, name) -> name.matches("surah_\\d+\\.json"));
    long bytes = 0;
    int count = files == null ? 0 : files.length;
    if (files != null) for (File f : files) bytes += f.length();
    int queued = QuranDownloadWorker.pending(requireContext()).size();
    status.setText(
        String.format(
            Locale.ROOT,
            I18n.choose(
                requireContext(),
                "%d / 114 surah tersimpan · %.1f MB\n%d dalam giliran",
                "%d / 114 surahs stored · %.1f MB\n%d queued"),
            count,
            bytes / 1048576d,
            queued));
  }

  @Override
  public void onDestroyView() {
    status = null;
    super.onDestroyView();
  }
}
