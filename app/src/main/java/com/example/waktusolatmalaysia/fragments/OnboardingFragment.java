package com.example.waktusolatmalaysia.fragments;

import android.os.Bundle;
import androidx.annotation.Nullable;
import com.example.waktusolatmalaysia.R;
import com.example.waktusolatmalaysia.ui.*;

public final class OnboardingFragment extends FeatureFragment {
  @Override
  protected void build(@Nullable Bundle s) {
    heading(
        "Panduan ringkas",
        I18n.choose(
            requireContext(),
            "Waktu, bacaan dan amalan pada rentak anda.",
            "Prayer times, reading and practice at your pace."));
    Ui.add(
        page,
        Ui.banner(
            requireContext(),
            R.drawable.onboarding_v3,
            "Waktu Solat Malaysia",
            I18n.choose(requireContext(), "Ruang untuk yang bermakna.", "Time for what matters."),
            null),
        20);
    add(
        "1 · " + I18n.t(requireContext(), "Pilih zon solat"),
        R.drawable.ic_location,
        R.id.locationFragment);
    add(
        "2 · " + I18n.t(requireContext(), "Peringatan & azan"),
        R.drawable.ic_bell,
        R.id.scheduleFragment);
    add(
        "3 · " + I18n.t(requireContext(), "Widget waktu solat"),
        R.drawable.ic_widget,
        R.id.widgetsFragment);
    add(
        "4 · " + I18n.t(requireContext(), "Muat turun teks"),
        R.drawable.ic_download,
        R.id.downloadsFragment);
    add(
        "5 · " + I18n.t(requireContext(), "Sandaran & pemulihan"),
        R.drawable.ic_backup,
        R.id.backupFragment);
  }

  private void add(String text, int icon, int id) {
    Ui.add(page, Ui.line(requireContext(), text, "", icon, () -> open(id)), 12);
  }
}
