package com.example.waktusolatmalaysia.fragments;

import android.os.Bundle;
import android.widget.*;
import androidx.annotation.Nullable;
import com.example.waktusolatmalaysia.*;
import com.example.waktusolatmalaysia.repo.ContentRepository;
import com.example.waktusolatmalaysia.ui.Ui;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

public final class AboutFragment extends FeatureFragment {
  @Override
  protected void build(@Nullable Bundle saved) {
    heading("Waktu Solat Malaysia", "Versi " + BuildConfig.VERSION_NAME);
    Ui.add(
        page,
        Ui.banner(
            requireContext(),
            R.drawable.hero_mosque,
            "Ruang untuk ibadah.",
            "Waktu, bacaan dan rutin harian dalam satu aplikasi.",
            null),
        24);
    Ui.add(
        page,
        Ui.text(requireContext(), "Sumber & penghargaan", 20, true, R.color.text_primary),
        28);
    info(
        "Jadual solat",
        "e-Solat JAKIM · Jadual rasmi mengikut zon. Bulan yang dimuat turun tersedia luar talian.");
    info(
        "Al-Quran & audio",
        "AlQuran Cloud · Mushaf Uthmani, terjemahan Abdullah Muhammad Basmeih dan bacaan Mishary"
            + " Rashid Alafasy. Surah yang dibuka disimpan; audio memerlukan internet.");
    info(
        "Hadis",
        "Sahih al-Bukhari melalui Hadith API (fawazahmed0). 12 hadis tersedia luar talian."
            + " Terjemahan dalam Bahasa Indonesia.");
    info(
        "Bacaan & kalendar",
        "Doa, wirid, niat dan rujukan peristiwa tersedia pada peranti. Tarikh penting tertakluk"
            + " kepada pengisytiharan pihak berkuasa Malaysia.");
    info(
        "Privasi",
        "Tiada akaun diperlukan. Pilihan, tanda bacaan dan penjejak ibadah disimpan pada peranti."
            + " Lokasi digunakan bagi pengiraan Kiblat.");
    Ui.add(
        page,
        Ui.line(
            requireContext(),
            "Lesen tipografi & data",
            "WSM Sans · Amiri · Hadith API",
            R.drawable.ic_info,
            () -> {
              try {
                ContentRepository repo = new ContentRepository(requireContext());
                String licenses =
                    repo.asset("licenses/jakarta-ofl.txt")
                        + "\n\n"
                        + repo.asset("licenses/amiri-ofl.txt")
                        + "\n\n"
                        + repo.asset("licenses/hadith-api.txt");
                TextView text =
                    Ui.text(requireContext(), licenses, 12, false, R.color.text_primary);
                text.setPadding(24, 16, 24, 16);
                ScrollView scroll = new ScrollView(requireContext());
                scroll.addView(text);
                new MaterialAlertDialogBuilder(requireContext())
                    .setTitle("Lesen & penghargaan")
                    .setView(scroll)
                    .setPositiveButton("Tutup", null)
                    .show();
              } catch (Exception ignored) {
              }
            }),
        16);
  }

  private void info(String title, String value) {
    Ui.add(page, Ui.text(requireContext(), title, 14, true, R.color.text_primary), 20);
    Ui.add(page, Ui.text(requireContext(), value, 13, false, R.color.text_secondary), 6);
  }
}
