package com.example.waktusolatmalaysia.fragments;

import android.os.Bundle;
import android.view.*;
import android.widget.*;
import androidx.appcompat.app.AppCompatDelegate;
import com.example.waktusolatmalaysia.R;
import com.example.waktusolatmalaysia.preferences.*;
import com.example.waktusolatmalaysia.ui.*;
import com.example.waktusolatmalaysia.views.MainActivity;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.switchmaterial.SwitchMaterial;

/** Shared grouped content for the floating sheet and existing settings route. */
public final class SettingsFragment extends FeatureFragment {
  private AppPreferences prefs;
  private UserStore store;

  @Override
  protected void build(Bundle state) {
    prefs = new AppPreferences(requireContext());
    store = new UserStore(requireContext());
    if (!(getParentFragment() instanceof SettingsPanel))
      heading("Pilihan anda", "Sesuaikan paparan, bacaan dan peringatan.");
    LinearLayout appearance = group("Paparan", "Appearance");
    appearance.addView(
        Ui.text(
            requireContext(),
            I18n.choose(requireContext(), "Tema aplikasi", "App theme"),
            14,
            true,
            R.color.text_primary));
    ChoiceBar themes =
        new ChoiceBar(
            requireContext(),
            new String[] {
              I18n.choose(requireContext(), "Sistem", "System"),
              I18n.t(requireContext(), "Cerah"),
              I18n.t(requireContext(), "Gelap")
            },
            null,
            java.util.Arrays.asList("system", "light", "dark").indexOf(prefs.theme()),
            this::setTheme);
    themes.setId(R.id.theme_choices);
    Ui.add(appearance, themes, 12);
    View language =
        Ui.line(
            requireContext(),
            I18n.choose(requireContext(), "Bahasa aplikasi", "App language"),
            I18n.english(requireContext()) ? "English" : "Bahasa Melayu",
            R.drawable.ic_language,
            () ->
                new MaterialAlertDialogBuilder(requireContext())
                    .setTitle("Bahasa / Language")
                    .setSingleChoiceItems(
                        new String[] {"Bahasa Melayu", "English"},
                        I18n.english(requireContext()) ? 1 : 0,
                        (d, n) -> {
                          I18n.language(requireContext(), n == 1 ? "en" : "ms");
                          d.dismiss();
                          requireActivity().recreate();
                        })
                    .setNegativeButton(I18n.t(requireContext(), "Tutup"), null)
                    .show());
    language.setId(R.id.locale_button);
    Ui.add(appearance, language, 12);
    SwitchMaterial format = toggle("Format masa 12 jam", prefs.isTwelveHour(), R.id.format_switch);
    format.setOnCheckedChangeListener((b, on) -> prefs.setTwelveHour(on));
    Ui.add(appearance, format, 4);
    LinearLayout reading = group("Bacaan Al-Quran", "Quran reading");
    SwitchMaterial translation =
        toggle(
            "Tunjukkan terjemahan",
            store.flag("show_translation", true),
            R.id.quran_translation_switch);
    translation.setOnCheckedChangeListener((b, on) -> store.set("show_translation", on));
    reading.addView(translation);
    boolean en =
        store.text("quran_language", I18n.english(requireContext()) ? "en" : "ms").equals("en");
    Ui.add(
        reading,
        Ui.line(
            requireContext(),
            I18n.choose(requireContext(), "Bahasa terjemahan", "Translation language"),
            en ? "English · Sahih International" : "Melayu · Basmeih",
            R.drawable.ic_language,
            () ->
                new MaterialAlertDialogBuilder(requireContext())
                    .setTitle(I18n.choose(requireContext(), "Terjemahan", "Translation"))
                    .setSingleChoiceItems(
                        new String[] {"Melayu · Basmeih", "English · Sahih International"},
                        en ? 1 : 0,
                        (d, n) -> {
                          store.set("quran_language", n == 1 ? "en" : "ms");
                          d.dismiss();
                          rebuild();
                        })
                    .show()),
        8);
    TextView size =
        Ui.text(
            requireContext(),
            sizeLabel(store.number("arabic_size", 30)),
            13,
            false,
            R.color.text_secondary);
    Ui.add(reading, size, 16);
    SeekBar seek = new SeekBar(requireContext());
    seek.setMax(20);
    seek.setProgress(Math.max(0, store.number("arabic_size", 30) - 24));
    seek.setMinimumHeight(Ui.dp(requireContext(), 48));
    seek.setContentDescription(
        I18n.choose(requireContext(), "Saiz tulisan Arab", "Arabic text size"));
    seek.setOnSeekBarChangeListener(
        new SeekBar.OnSeekBarChangeListener() {
          public void onProgressChanged(SeekBar s, int n, boolean user) {
            if (user) {
              store.set("arabic_size", n + 24);
              size.setText(sizeLabel(n + 24));
            }
          }

          public void onStartTrackingTouch(SeekBar s) {}

          public void onStopTrackingTouch(SeekBar s) {}
        });
    Ui.add(reading, seek, 4);
    LinearLayout prayers = group("Solat dan peringatan", "Prayer and reminders");
    link(prayers, "Lokasi solat", prefs.zone().name, R.drawable.ic_location, R.id.locationFragment);
    link(
        prayers,
        "Peringatan & azan",
        "Pilih solat dan tetapkan penggera",
        R.drawable.ic_bell,
        R.id.scheduleFragment);
    link(
        prayers,
        "Widget waktu solat",
        "Panduan widget",
        R.drawable.ic_widget,
        R.id.widgetsFragment);
    LinearLayout data = group("Data dan aplikasi", "Data and app");
    link(
        data,
        "Sandaran & pemulihan",
        I18n.choose(
            requireContext(),
            "Eksport dan import Android / web",
            "Export and import Android / web"),
        R.drawable.ic_backup,
        R.id.backupFragment);
    link(
        data,
        "Tentang & sumber",
        "Maklumat aplikasi dan penghargaan",
        R.drawable.ic_info,
        R.id.aboutFragment);
    link(data, "Panduan ringkas", "", R.drawable.ic_info, R.id.onboardingFragment);
    Ui.add(
        page,
        Ui.text(
            requireContext(),
            I18n.choose(
                requireContext(),
                "Data peribadi kekal pada peranti ini. Lokasi digunakan untuk kompas Kiblat.",
                "Personal data stays on this device. Location is used for the Qibla compass."),
            12,
            false,
            R.color.text_secondary),
        20);
  }

  private void rebuild() {
    page.removeAllViews();
    build(null);
  }

  private String sizeLabel(int n) {
    return I18n.choose(requireContext(), "Saiz tulisan Arab · ", "Arabic text size · ") + n + " sp";
  }

  private LinearLayout group(String ms, String en) {
    Ui.add(
        page,
        Ui.text(
            requireContext(),
            I18n.choose(requireContext(), ms, en),
            16,
            true,
            R.color.text_primary),
        20);
    LinearLayout body = Ui.column(requireContext());
    Ui.add(page, Ui.card(requireContext(), body, 16), 8);
    return body;
  }

  private SwitchMaterial toggle(String title, boolean checked, int id) {
    SwitchMaterial v = new SwitchMaterial(requireContext());
    v.setId(id);
    v.setText(I18n.t(requireContext(), title));
    v.setTextColor(Ui.color(requireContext(), R.color.text_primary));
    v.setTextSize(14);
    v.setMinHeight(Ui.dp(requireContext(), 56));
    v.setPadding(
        Ui.dp(requireContext(), 4),
        Ui.dp(requireContext(), 8),
        Ui.dp(requireContext(), 4),
        Ui.dp(requireContext(), 8));
    v.setChecked(checked);
    return v;
  }

  private void setTheme(int index) {
    String value = new String[] {"system", "light", "dark"}[index];
    if (value.equals(prefs.theme())) return;
    prefs.setTheme(value);
    AppCompatDelegate.setDefaultNightMode(
        index == 0
            ? AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
            : index == 2 ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO);
  }

  private void link(LinearLayout group, String title, String sub, int icon, int route) {
    Ui.add(
        group,
        Ui.line(
            requireContext(),
            title,
            sub,
            icon,
            () -> {
              if (getParentFragment() instanceof SettingsPanel)
                ((SettingsPanel) getParentFragment()).dismiss();
              ((MainActivity) requireActivity()).openDestination(route, null);
            }),
        4);
  }
}
