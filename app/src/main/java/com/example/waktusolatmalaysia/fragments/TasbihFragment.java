package com.example.waktusolatmalaysia.fragments;

import android.os.Bundle;
import android.text.InputType;
import android.view.*;
import android.widget.*;
import androidx.annotation.Nullable;
import com.example.waktusolatmalaysia.R;
import com.example.waktusolatmalaysia.customs.TasbihDialView;
import com.example.waktusolatmalaysia.preferences.UserStore;
import com.example.waktusolatmalaysia.ui.Ui;
import com.example.waktusolatmalaysia.utils.PrayerClock;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.checkbox.MaterialCheckBox;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import java.time.LocalDate;

public final class TasbihFragment extends FeatureFragment {
  private UserStore store;
  private final java.util.Map<String, MaterialCheckBox> prayerChecks =
      new java.util.LinkedHashMap<>();
  private LocalDate trackerDate;
  private boolean syncing;
  private final android.content.SharedPreferences.OnSharedPreferenceChangeListener trackListener =
      (p, key) -> {
        if ((key == null || key.startsWith("prayed_")) && getActivity() != null)
          getActivity().runOnUiThread(() -> syncChecks());
      };

  private void syncChecks() {
    if (!isAdded() || trackerDate == null) return;
    syncing = true;
    java.util.Map<String, Boolean> values =
        new com.example.waktusolatmalaysia.preferences.PrayerTrackingStore(requireContext())
            .read(trackerDate);
    for (java.util.Map.Entry<String, MaterialCheckBox> e : prayerChecks.entrySet())
      e.getValue().setChecked(Boolean.TRUE.equals(values.get(e.getKey())));
    syncing = false;
  }

  @Override
  public void onResume() {
    super.onResume();
    requireContext()
        .getSharedPreferences("personal_library", 0)
        .registerOnSharedPreferenceChangeListener(trackListener);
    syncChecks();
  }

  @Override
  public void onPause() {
    requireContext()
        .getSharedPreferences("personal_library", 0)
        .unregisterOnSharedPreferenceChangeListener(trackListener);
    super.onPause();
  }

  private TasbihDialView dial;
  private TextView summary;
  private int count, goal;

  @Override
  protected void build(@Nullable Bundle state) {
    store = new UserStore(requireContext());
    count = store.number("tasbih_count", 0);
    goal = Math.max(1, Math.min(10000, store.number("tasbih_goal", 33)));
    heading("Tasbih digital", "Satu ketikan, satu zikir. Teruskan mengikut rentak anda.");
    TextView phrase = Ui.arabic(requireContext(), "سُبْحَانَ اللَّهِ", 32);
    phrase.setGravity(Gravity.CENTER);
    phrase.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
    Ui.add(page, phrase, 22);
    dial = new TasbihDialView(requireContext());
    dial.setId(R.id.tasbih_tap);
    dial.setCount(count, goal);
    page.addView(dial, new LinearLayout.LayoutParams(-1, Ui.dp(requireContext(), 300)));
    dial.setOnClickListener(
        v -> {
          if (count < Integer.MAX_VALUE) count++;
          store.set("tasbih_count", count);
          v.performHapticFeedback(
              count % goal == 0
                  ? HapticFeedbackConstants.LONG_PRESS
                  : HapticFeedbackConstants.KEYBOARD_TAP);
          update();
        });
    summary = Ui.text(requireContext(), "", 13, false, R.color.text_secondary);
    summary.setId(R.id.tasbih_count);
    summary.setGravity(Gravity.CENTER);
    Ui.add(page, summary, 0);
    LinearLayout goals = Ui.row(requireContext());
    for (int target : new int[] {33, 99, 100}) {
      MaterialButton choice = Ui.quiet(requireContext(), String.valueOf(target), 0);
      LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(0, -2, 1);
      p.setMargins(Ui.dp(requireContext(), 3), 0, Ui.dp(requireContext(), 3), 0);
      goals.addView(choice, p);
      choice.setOnClickListener(
          v -> {
            goal = target;
            store.set("tasbih_goal", goal);
            update();
          });
    }
    Ui.add(page, goals, 18);
    LinearLayout actions = Ui.row(requireContext());
    MaterialButton undo = Ui.quiet(requireContext(), "Undur satu", 0);
    undo.setOnClickListener(
        v -> {
          count = Math.max(0, count - 1);
          store.set("tasbih_count", count);
          update();
        });
    actions.addView(undo, new LinearLayout.LayoutParams(0, -2, 1));
    MaterialButton reset = Ui.quiet(requireContext(), "Mula semula", R.drawable.ic_reset);
    reset.setId(R.id.tasbih_reset);
    actions.addView(reset, new LinearLayout.LayoutParams(0, -2, 1));
    reset.setOnClickListener(
        v ->
            new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Mulakan kiraan baharu?")
                .setMessage("Kiraan semasa akan kembali kepada sifar.")
                .setNegativeButton("Batal", null)
                .setPositiveButton(
                    "Mula semula",
                    (d, w) -> {
                      count = 0;
                      store.set("tasbih_count", 0);
                      update();
                    })
                .show());
    Ui.add(page, actions, 8);
    MaterialButton custom = Ui.quiet(requireContext(), "Tetapkan sasaran sendiri", 0);
    custom.setId(R.id.tasbih_goal);
    custom.setOnClickListener(
        v -> {
          EditText input = new EditText(requireContext());
          input.setId(R.id.tasbih_goal_input);
          input.setInputType(InputType.TYPE_CLASS_NUMBER);
          input.setText(String.valueOf(goal));
          androidx.appcompat.app.AlertDialog dialog =
              new MaterialAlertDialogBuilder(requireContext())
                  .setTitle(
                      com.example.waktusolatmalaysia.ui.I18n.choose(
                          requireContext(), "Sasaran zikir", "Dhikr goal"))
                  .setView(input)
                  .setNegativeButton(
                      com.example.waktusolatmalaysia.ui.I18n.t(requireContext(), "Batal"), null)
                  .setPositiveButton(
                      com.example.waktusolatmalaysia.ui.I18n.t(requireContext(), "Simpan"), null)
                  .create();
          dialog.setOnShowListener(
              d ->
                  dialog
                      .getButton(-1)
                      .setOnClickListener(
                          saveGoal -> {
                            try {
                              int value = Integer.parseInt(input.getText().toString());
                              if (value < 1 || value > 10000) throw new NumberFormatException();
                              goal = value;
                              store.set("tasbih_goal", goal);
                              update();
                              dialog.dismiss();
                            } catch (NumberFormatException invalid) {
                              input.setError(
                                  com.example.waktusolatmalaysia.ui.I18n.choose(
                                      requireContext(), "Masukkan 1–10000.", "Enter 1–10000."));
                            }
                          }));
          dialog.show();
        });
    Ui.add(page, custom, 4);
    Ui.add(page, Ui.text(requireContext(), "Amalan hari ini", 20, true, R.color.text_primary), 28);
    LinearLayout tracker = Ui.column(requireContext());
    String today = LocalDate.now(PrayerClock.MALAYSIA).toString();
    trackerDate = LocalDate.parse(today);
    prayerChecks.clear();
    String[] names = {"Subuh", "Zohor", "Asar", "Maghrib", "Isyak"};
    String[] ids = {"fajr", "dhuhr", "asr", "maghrib", "isha"};
    com.example.waktusolatmalaysia.preferences.PrayerTrackingStore tracking =
        new com.example.waktusolatmalaysia.preferences.PrayerTrackingStore(requireContext());
    for (int index = 0; index < ids.length; index++) {
      String prayer = ids[index];
      MaterialCheckBox done = new MaterialCheckBox(requireContext());
      done.setText(
          com.example.waktusolatmalaysia.ui.I18n.prayer(
              requireContext(), new int[] {1, 4, 5, 6, 7}[index]));
      done.setMinHeight(Ui.dp(requireContext(), 48));
      done.setChecked(Boolean.TRUE.equals(tracking.read(trackerDate).get(prayer)));
      done.setOnCheckedChangeListener(
          (button, checked) -> {
            if (!syncing) tracking.set(trackerDate, prayer, checked);
          });
      prayerChecks.put(prayer, done);
      tracker.addView(done);
    }
    MaterialCheckBox fasting = new MaterialCheckBox(requireContext());
    fasting.setText("Saya berpuasa hari ini");
    fasting.setChecked(store.flag("fasted_" + today, false));
    fasting.setOnCheckedChangeListener((button, checked) -> store.set("fasted_" + today, checked));
    tracker.addView(fasting);
    Ui.add(page, Ui.card(requireContext(), tracker, 14), 12);
    Ui.add(
        page,
        Ui.text(
            requireContext(),
            "Catatan amalan kekal pada peranti anda.",
            11,
            false,
            R.color.text_secondary),
        14);
    update();
  }

  private void update() {
    dial.setCount(count, goal);
    summary.setText("Sasaran " + goal + " · " + (count / goal) + " pusingan lengkap");
  }
}
