package com.example.waktusolatmalaysia.fragments;

import android.os.Bundle;
import android.widget.*;
import androidx.activity.result.*;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import com.example.waktusolatmalaysia.R;
import com.example.waktusolatmalaysia.repo.*;
import com.example.waktusolatmalaysia.ui.*;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import java.io.*;
import java.nio.charset.StandardCharsets;
import org.json.*;

public final class BackupFragment extends FeatureFragment {
  private TextView status;
  private final ActivityResultLauncher<String> export =
      registerForActivityResult(
          new ActivityResultContracts.CreateDocument("application/json"),
          uri -> {
            if (uri == null) return;
            task(
                () -> {
                  byte[] data =
                      BackupStore.export(requireContext())
                          .toString(2)
                          .getBytes(StandardCharsets.UTF_8);
                  try (OutputStream out =
                      requireContext().getContentResolver().openOutputStream(uri, "wt")) {
                    out.write(data);
                  }
                  return true;
                },
                ok ->
                    status.setText(
                        I18n.choose(requireContext(), "Sandaran dieksport.", "Backup exported.")),
                e -> error());
          });
  private final ActivityResultLauncher<String[]> restore =
      registerForActivityResult(
          new ActivityResultContracts.OpenDocument(),
          uri -> {
            if (uri == null) return;
            task(
                () -> {
                  try (InputStream in = requireContext().getContentResolver().openInputStream(uri);
                      ByteArrayOutputStream out = new ByteArrayOutputStream()) {
                    byte[] b = new byte[4096];
                    int n;
                    while ((n = in.read(b)) != -1) {
                      out.write(b, 0, n);
                      if (out.size() > 5000000) throw new IOException("Backup too large");
                    }
                    JSONObject data = new JSONObject(out.toString("UTF-8"));
                    BackupStore.validate(data);
                    return data;
                  }
                },
                this::preview,
                e -> error());
          });

  @Override
  protected void build(@Nullable Bundle s) {
    heading(
        "Sandaran & pemulihan",
        I18n.choose(
            requireContext(),
            "Pindahkan simpanan dan amalan anda antara Android dan web tanpa akaun.",
            "Move saved items and practice records between Android and web without an account."));
    status =
        Ui.text(
            requireContext(),
            I18n.choose(
                requireContext(),
                "Fail mengandungi data peribadi tempatan anda. Kebenaran sistem dan konfigurasi"
                    + " widget tidak disertakan.",
                "The file contains your local personal data. System permissions and widget"
                    + " instances are excluded."),
            13,
            false,
            R.color.text_secondary);
    Ui.add(page, status, 20);
    android.view.View save = Ui.button(requireContext(), "Eksport sandaran", R.drawable.ic_backup);
    save.setId(R.id.backup_export);
    save.setOnClickListener(
        v -> export.launch("WaktuSolatMalaysia-" + java.time.LocalDate.now() + ".json"));
    Ui.add(page, save, 20);
    android.view.View load = Ui.quiet(requireContext(), "Import sandaran", R.drawable.ic_download);
    load.setId(R.id.backup_import);
    load.setOnClickListener(v -> restore.launch(new String[] {"application/json", "text/plain"}));
    Ui.add(page, load, 12);
    Ui.add(
        page,
        Ui.text(
            requireContext(),
            I18n.choose(
                requireContext(),
                "Gabungkan mengekalkan pilihan sedia ada. Gantikan memulihkan pilihan dalam fail."
                    + " Semua peringatan solat diimport dalam keadaan mati; semak sebelum"
                    + " mengaktifkannya.",
                "Merge keeps existing preferences. Replace restores the file’s preferences. Prayer"
                    + " alerts are switched off on import; review before enabling them."),
            12,
            false,
            R.color.text_secondary),
        20);
  }

  private void preview(JSONObject data) {
    try {
      String message =
          data.getJSONArray("bookmarks").length()
              + I18n.choose(requireContext(), " simpanan · ", " saved items · ")
              + data.getJSONArray("reminders").length()
              + I18n.choose(requireContext(), " peringatan", " reminders");
      new MaterialAlertDialogBuilder(requireContext())
          .setTitle(I18n.t(requireContext(), "Import sandaran"))
          .setMessage(message)
          .setNegativeButton(I18n.t(requireContext(), "Batal"), null)
          .setNeutralButton(I18n.t(requireContext(), "Gabungkan"), (d, w) -> apply(data, true))
          .setPositiveButton(I18n.t(requireContext(), "Gantikan"), (d, w) -> apply(data, false))
          .show();
    } catch (Exception e) {
      error();
    }
  }

  private void apply(JSONObject data, boolean merge) {
    task(
        () -> {
          BackupStore.apply(requireContext(), data, merge);
          return true;
        },
        ok -> {
          status.setText(
              I18n.choose(
                  requireContext(),
                  "Data diimport. Peringatan baharu kekal mati sehingga diaktifkan.",
                  "Data imported. New reminders remain off until enabled."));
          String theme =
              new com.example.waktusolatmalaysia.preferences.AppPreferences(requireContext())
                  .theme();
          androidx.appcompat.app.AppCompatDelegate.setDefaultNightMode(
              theme.equals("system")
                  ? androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
                  : theme.equals("dark")
                      ? androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_YES
                      : androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_NO);
          requireActivity().recreate();
        },
        e -> error());
  }

  private void error() {
    if (status != null)
      status.setText(
          I18n.choose(
              requireContext(),
              "Fail tidak sah atau operasi tidak dapat diselesaikan.",
              "The file is invalid or the operation could not be completed."));
  }

  @Override
  public void onDestroyView() {
    status = null;
    super.onDestroyView();
  }
}
