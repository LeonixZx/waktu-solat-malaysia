package com.example.waktusolatmalaysia.fragments;

import android.os.Bundle;
import android.text.*;
import android.view.*;
import android.widget.*;
import androidx.annotation.Nullable;
import com.example.waktusolatmalaysia.R;
import com.example.waktusolatmalaysia.model.*;
import com.example.waktusolatmalaysia.preferences.*;
import com.example.waktusolatmalaysia.repo.*;
import com.example.waktusolatmalaysia.services.*;
import com.example.waktusolatmalaysia.ui.*;
import com.example.waktusolatmalaysia.utils.*;
import com.example.waktusolatmalaysia.views.MainActivity;
import com.google.android.material.chip.*;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.switchmaterial.SwitchMaterial;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;

public final class TasksFragment extends FeatureFragment {
  private LinearLayout list;

  @Override
  protected void build(@Nullable Bundle saved) {
    heading(
        "Peringatan peribadi",
        I18n.choose(
            requireContext(),
            "Rancang amalan sebelum atau selepas waktu solat.",
            "Plan a practice before or after a prayer time."));
    View add = Ui.button(requireContext(), "Tambah peringatan", R.drawable.ic_task);
    add.setId(R.id.task_add);
    add.setOnClickListener(v -> edit(null));
    Ui.add(page, add, 20);
    list = Ui.column(requireContext());
    list.setId(R.id.task_list);
    Ui.add(page, list, 12);
    Ui.add(
        page,
        Ui.text(
            requireContext(),
            I18n.choose(
                requireContext(),
                "Hari yang dipilih ialah hari waktu rujukan. Isyak Isnin + 6 jam boleh berbunyi"
                    + " pada Selasa. Peringatan peribadi menggunakan pemberitahuan, tanpa azan.",
                "Selected weekdays refer to the prayer’s day. Monday Isha + 6 hours can notify on"
                    + " Tuesday. Personal reminders use notifications, never adhan."),
            12,
            false,
            R.color.text_secondary),
        20);
    render();
  }

  private void render() {
    if (list == null) return;
    list.removeAllViews();
    try {
      List<PrayerRelativeReminder> tasks = new TaskStore(requireContext()).load();
      if (tasks.isEmpty())
        Ui.add(
            list,
            Ui.text(requireContext(), "Tiada peringatan lagi.", 15, false, R.color.text_secondary),
            24);
      String zone = new AppPreferences(requireContext()).zone().code;
      LocalDate today = LocalDate.now(PrayerClock.MALAYSIA);
      List<PrayerDay> days =
          new PrayerRepository(requireContext())
              .cachedDays(zone, today.minusDays(1), today.plusDays(9));
      for (PrayerRelativeReminder t : tasks) {
        LinearLayout body = Ui.column(requireContext());
        body.setTag("task:" + t.id);
        LinearLayout top = Ui.row(requireContext());
        top.addView(
            Ui.text(requireContext(), t.title, 16, true, R.color.text_primary),
            new LinearLayout.LayoutParams(0, -2, 1));
        SwitchMaterial enabled = new SwitchMaterial(requireContext());
        enabled.setShowText(false);
        enabled.setContentDescription(
            I18n.choose(requireContext(), "Aktifkan ", "Enable ") + t.title);
        enabled.setChecked(t.enabled);
        enabled.setOnCheckedChangeListener(
            (v, on) -> {
              if (on) ((MainActivity) requireActivity()).notificationPermission(ok -> save(t, ok));
              else save(t, false);
            });
        top.addView(enabled);
        body.addView(top);
        Ui.add(
            body,
            Ui.text(
                requireContext(),
                Math.abs(t.offsetMinutes)
                    + " "
                    + I18n.t(requireContext(), "minit")
                    + " "
                    + I18n.t(requireContext(), t.offsetMinutes < 0 ? "Sebelum" : "Selepas")
                    + " "
                    + I18n.prayer(requireContext(), t.anchor),
                13,
                false,
                R.color.text_secondary),
            8);
        String[] labels =
            I18n.english(requireContext())
                ? new String[] {"Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"}
                : new String[] {"Isn", "Sel", "Rab", "Kha", "Jum", "Sab", "Ahd"};
        List<String> selected = new ArrayList<>();
        for (int d : new TreeSet<>(t.weekdays)) selected.add(labels[d - 1]);
        Ui.add(
            body,
            Ui.text(requireContext(), String.join(" · ", selected), 12, true, R.color.prayer_text),
            8);
        TaskPlanner.Occurrence next =
            TaskPlanner.next(t, days, ZonedDateTime.now(PrayerClock.MALAYSIA));
        String upcoming =
            !t.enabled
                ? I18n.t(requireContext(), "Matikan")
                : next == null
                    ? I18n.choose(
                        requireContext(), "Menunggu jadual zon", "Waiting for the zone schedule")
                    : next.time.format(
                        DateTimeFormatter.ofPattern(
                            "EEE, d MMM · HH:mm",
                            I18n.english(requireContext())
                                ? Locale.ENGLISH
                                : Locale.forLanguageTag("ms")));
        Ui.add(body, Ui.text(requireContext(), upcoming, 12, false, R.color.text_secondary), 8);
        LinearLayout actions = Ui.row(requireContext());
        actions.addView(
            Ui.iconButton(
                requireContext(),
                R.drawable.ic_edit,
                I18n.choose(requireContext(), "Sunting", "Edit"),
                () -> edit(t)));
        actions.addView(
            Ui.iconButton(
                requireContext(),
                R.drawable.ic_delete,
                "Padam",
                () ->
                    new MaterialAlertDialogBuilder(requireContext())
                        .setTitle(I18n.t(requireContext(), "Padam") + " " + t.title + "?")
                        .setNegativeButton(I18n.t(requireContext(), "Batal"), null)
                        .setPositiveButton(
                            I18n.t(requireContext(), "Padam"),
                            (d, w) -> {
                              try {
                                new TaskStore(requireContext()).remove(t.id);
                                ReminderScheduler.reschedule(requireContext());
                                render();
                              } catch (Exception e) {
                                message(e);
                              }
                            })
                        .show()));
        Ui.add(body, actions, 12);
        Ui.add(list, Ui.card(requireContext(), body, 16), 12);
      }
    } catch (Exception e) {
      Ui.add(
          list,
          Ui.text(
              requireContext(),
              I18n.choose(
                  requireContext(),
                  "Peringatan tidak dapat dibaca. Fail asal dikekalkan.",
                  "Reminders could not be read. The original file is preserved."),
              14,
              false,
              R.color.text_secondary),
          12);
    }
  }

  private void save(PrayerRelativeReminder t, boolean enabled) {
    try {
      new TaskStore(requireContext())
          .put(
              new PrayerRelativeReminder(
                  t.id, t.title, t.anchor, t.offsetMinutes, t.weekdays, enabled));
      ReminderScheduler.reschedule(requireContext());
      ScheduleRefreshWorker.enqueue(requireContext(), false);
      render();
    } catch (Exception e) {
      message(e);
    }
  }

  private void message(Exception e) {
    Toast.makeText(
            requireContext(),
            I18n.choose(
                requireContext(),
                "Perubahan belum dapat disimpan.",
                "The change could not be saved."),
            android.widget.Toast.LENGTH_LONG)
        .show();
  }

  private void edit(PrayerRelativeReminder original) {
    android.content.Context c = requireContext();
    ScrollView scroller = new ScrollView(c);
    LinearLayout form = Ui.column(c);
    form.setPadding(Ui.dp(c, 20), Ui.dp(c, 12), Ui.dp(c, 20), Ui.dp(c, 12));
    scroller.addView(form);
    EditText title = new EditText(c);
    title.setId(R.id.v31_task_title);
    title.setHint(I18n.t(c, "Tajuk"));
    title.setSingleLine(true);
    title.setFilters(new InputFilter[] {new InputFilter.LengthFilter(80)});
    title.setText(original == null ? "" : original.title);
    Ui.add(form, title, 0);
    Spinner anchor = new Spinner(c);
    anchor.setId(R.id.v31_task_anchor);
    String[] prayers = new String[8];
    for (int i = 0; i < 8; i++) prayers[i] = I18n.prayer(c, i);
    anchor.setAdapter(
        new ArrayAdapter<>(c, android.R.layout.simple_spinner_dropdown_item, prayers));
    anchor.setSelection(original == null ? 1 : original.anchor);
    Ui.add(form, anchor, 16);
    LinearLayout offsetRow = Ui.row(c);
    EditText offset = new EditText(c);
    offset.setId(R.id.v31_task_offset);
    offset.setInputType(InputType.TYPE_CLASS_NUMBER);
    offset.setHint("0–360 " + I18n.t(c, "minit"));
    offset.setText(String.valueOf(original == null ? 15 : Math.abs(original.offsetMinutes)));
    offsetRow.addView(offset, new LinearLayout.LayoutParams(0, -2, 1));
    Spinner direction = new Spinner(c);
    direction.setId(R.id.v31_task_direction);
    direction.setAdapter(
        new ArrayAdapter<>(
            c,
            android.R.layout.simple_spinner_dropdown_item,
            new String[] {I18n.t(c, "Sebelum"), I18n.t(c, "Selepas")}));
    direction.setSelection(original == null || original.offsetMinutes >= 0 ? 1 : 0);
    offsetRow.addView(direction);
    offsetRow.addOnLayoutChangeListener((v,l,t,r,b,ol,ot,or,ob) -> {
      boolean stacked = r-l < Ui.dp(c,360) || getResources().getConfiguration().fontScale > 1.3f;
      int orientation = stacked ? LinearLayout.VERTICAL : LinearLayout.HORIZONTAL;
      if (offsetRow.getOrientation() != orientation) {
        offsetRow.setOrientation(orientation);
        offset.setLayoutParams(new LinearLayout.LayoutParams(stacked ? -1 : 0, -2, stacked ? 0 : 1));
        direction.setLayoutParams(new LinearLayout.LayoutParams(stacked ? -1 : -2, -2));
      }
    });
    Ui.add(form, offsetRow, 12);
    Ui.add(form, Ui.text(c, "Pilih hari rujukan solat.", 12, false, R.color.text_secondary), 16);
    ChipGroup weekdays = new ChipGroup(c);
    String[] dayLabels =
        I18n.english(c)
            ? new String[] {"Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"}
            : new String[] {"Isn", "Sel", "Rab", "Kha", "Jum", "Sab", "Ahd"};
    List<Chip> chips = new ArrayList<>();
    for (int i = 1; i <= 7; i++) {
      Chip chip = new Chip(c);
      chip.setText(dayLabels[i - 1]);
      chip.setCheckable(true);
      chip.setEnsureMinTouchTargetSize(true);
      chip.setChecked(original == null || original.weekdays.contains(i));
      weekdays.addView(chip);
      chips.add(chip);
    }
    Ui.add(form, weekdays, 8);
    SwitchMaterial enabled = new SwitchMaterial(c);
    enabled.setId(R.id.v31_task_enabled);
    enabled.setShowText(false);
    enabled.setText(I18n.t(c, "Aktif"));
    enabled.setChecked(original == null || original.enabled);
    Ui.add(form, enabled, 12);
    TextView preview = Ui.text(c, "", 12, false, R.color.prayer_text);
    Ui.add(form, preview, 16);
    Runnable update =
        () -> {
          try {
            Set<Integer> days = new HashSet<>();
            for (int i = 0; i < 7; i++) if (chips.get(i).isChecked()) days.add(i + 1);
            int minutes =
                Integer.parseInt(offset.getText().toString())
                    * (direction.getSelectedItemPosition() == 0 ? -1 : 1);
            PrayerRelativeReminder t =
                new PrayerRelativeReminder(
                    original == null ? UUID.randomUUID().toString() : original.id,
                    "preview",
                    anchor.getSelectedItemPosition(),
                    minutes,
                    days,
                    enabled.isChecked());
            LocalDate date = LocalDate.now(PrayerClock.MALAYSIA);
            TaskPlanner.Occurrence next =
                TaskPlanner.next(
                    t,
                    new PrayerRepository(c)
                        .cachedDays(
                            new AppPreferences(c).zone().code, date.minusDays(1), date.plusDays(9)),
                    ZonedDateTime.now(PrayerClock.MALAYSIA));
            preview.setText(
                next == null
                    ? I18n.choose(
                        c,
                        "Tiada waktu tersedia; muat jadual zon dahulu.",
                        "No next time is available; download the zone schedule first.")
                    : next.time.format(
                        DateTimeFormatter.ofPattern(
                            "EEEE, d MMM · HH:mm",
                            I18n.english(c) ? Locale.ENGLISH : Locale.forLanguageTag("ms"))));
          } catch (Exception e) {
            preview.setText(
                I18n.choose(
                    c,
                    "Pilih hari dan nilai 0–360 minit.",
                    "Choose weekdays and an offset of 0–360 minutes."));
          }
        };
    offset.addTextChangedListener(
        new TextWatcher() {
          public void beforeTextChanged(CharSequence s, int st, int count, int after) {}

          public void onTextChanged(CharSequence s, int st, int before, int count) {
            update.run();
          }

          public void afterTextChanged(Editable e) {}
        });
    AdapterView.OnItemSelectedListener changed =
        new AdapterView.OnItemSelectedListener() {
          public void onItemSelected(AdapterView<?> a, View v, int p, long id) {
            update.run();
          }

          public void onNothingSelected(AdapterView<?> a) {}
        };
    anchor.setOnItemSelectedListener(changed);
    direction.setOnItemSelectedListener(changed);
    for (Chip chip : chips) chip.setOnCheckedChangeListener((v, on) -> update.run());
    enabled.setOnCheckedChangeListener((v, on) -> update.run());
    update.run();
    androidx.appcompat.app.AlertDialog dialog =
        new MaterialAlertDialogBuilder(c)
            .setTitle(I18n.t(c, "Tambah peringatan"))
            .setView(scroller)
            .setNegativeButton(I18n.t(c, "Batal"), null)
            .setPositiveButton(I18n.t(c, "Simpan"), null)
            .create();
    dialog.setOnShowListener(
        d ->
            dialog
                .getButton(-1)
                .setOnClickListener(
                    v -> {
                      try {
                        Set<Integer> selected = new HashSet<>();
                        for (int i = 0; i < 7; i++)
                          if (chips.get(i).isChecked()) selected.add(i + 1);
                        int minutes =
                            Integer.parseInt(offset.getText().toString())
                                * (direction.getSelectedItemPosition() == 0 ? -1 : 1);
                        PrayerRelativeReminder t =
                            new PrayerRelativeReminder(
                                original == null ? UUID.randomUUID().toString() : original.id,
                                title.getText().toString(),
                                anchor.getSelectedItemPosition(),
                                minutes,
                                selected,
                                enabled.isChecked());
                        java.util.function.Consumer<Boolean> persist =
                            ok -> {
                              try {
                                new TaskStore(c)
                                    .put(
                                        new PrayerRelativeReminder(
                                            t.id,
                                            t.title,
                                            t.anchor,
                                            t.offsetMinutes,
                                            t.weekdays,
                                            t.enabled && ok));
                                ReminderScheduler.reschedule(c);
                                ScheduleRefreshWorker.enqueue(c, false);
                                dialog.dismiss();
                                render();
                              } catch (Exception e) {
                                message(e);
                              }
                            };
                        if (t.enabled)
                          ((MainActivity) requireActivity()).notificationPermission(persist);
                        else persist.accept(false);
                      } catch (Exception e) {
                        title.setError(
                            I18n.choose(
                                c,
                                "Masukkan tajuk, hari dan nilai yang sah.",
                                "Enter a title, weekdays and a valid offset."));
                      }
                    }));
    dialog.show();
    if (dialog.getWindow() != null) dialog.getWindow().setLayout(
        Math.min(getResources().getDisplayMetrics().widthPixels - Ui.dp(c,32), Ui.dp(c,560)),
        android.view.ViewGroup.LayoutParams.WRAP_CONTENT);
  }

  @Override
  public void onDestroyView() {
    list = null;
    super.onDestroyView();
  }
}
