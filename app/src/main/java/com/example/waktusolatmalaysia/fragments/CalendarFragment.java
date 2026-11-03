package com.example.waktusolatmalaysia.fragments;

import android.os.Bundle;
import android.view.*;
import android.widget.*;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import com.example.waktusolatmalaysia.R;
import com.example.waktusolatmalaysia.model.PrayerDay;
import com.example.waktusolatmalaysia.preferences.AppPreferences;
import com.example.waktusolatmalaysia.repo.*;
import com.example.waktusolatmalaysia.ui.I18n;
import com.example.waktusolatmalaysia.ui.Ui;
import com.example.waktusolatmalaysia.utils.PrayerClock;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import java.time.*;
import java.time.chrono.HijrahDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoField;
import java.util.*;
import org.json.*;

public final class CalendarFragment extends FeatureFragment {
  private final ActivityResultLauncher<String> export =
      registerForActivityResult(
          new ActivityResultContracts.CreateDocument("text/calendar"),
          uri -> {
            if (uri == null) return;
            try (java.io.OutputStream out =
                requireContext().getContentResolver().openOutputStream(uri, "wt")) {
              String value =
                  com.example.waktusolatmalaysia.utils.CalendarFile.prayers(
                      this.days.values(),
                      new AppPreferences(requireContext()).zone().code,
                      I18n.english(requireContext()));
              out.write(value.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            } catch (Exception e) {
              android.widget.Toast.makeText(
                      requireContext(),
                      I18n.choose(
                          requireContext(),
                          "Eksport belum dapat diselesaikan.",
                          "Export could not be completed."),
                      android.widget.Toast.LENGTH_LONG)
                  .show();
            }
          });
  private YearMonth month = YearMonth.now(PrayerClock.MALAYSIA);
  private LinearLayout content;
  private TextView monthLabel;
  private final Map<LocalDate, PrayerDay> days = new HashMap<>();

  @Override
  protected void build(@Nullable Bundle saved) {
    if (saved != null) month = YearMonth.parse(saved.getString("month", month.toString()));
    heading("Kalendar Islam", "Rancang hari dan semak tarikh penting dalam setahun.");
    LinearLayout controls = Ui.row(requireContext());
    MaterialButton previous = Ui.quiet(requireContext(), "‹", 0),
        next = Ui.quiet(requireContext(), "›", 0);
    previous.setContentDescription("Bulan sebelumnya");
    next.setContentDescription("Bulan berikutnya");
    controls.addView(previous, new LinearLayout.LayoutParams(Ui.dp(requireContext(), 52), -2));
    monthLabel = Ui.text(requireContext(), "", 18, true, R.color.text_primary);
    monthLabel.setGravity(Gravity.CENTER);
    controls.addView(monthLabel, new LinearLayout.LayoutParams(0, -2, 1));
    controls.addView(next, new LinearLayout.LayoutParams(Ui.dp(requireContext(), 52), -2));
    Ui.add(page, controls, 20);
    android.view.View exportButton =
        Ui.quiet(
            requireContext(),
            I18n.choose(requireContext(), "Eksport kalendar solat", "Export prayer calendar"),
            R.drawable.ic_calendar);
    exportButton.setOnClickListener(
        v -> {
          if (days.isEmpty())
            android.widget.Toast.makeText(
                    requireContext(),
                    I18n.choose(
                        requireContext(),
                        "Muat jadual zon terlebih dahulu.",
                        "Load the zone schedule first."),
                    android.widget.Toast.LENGTH_LONG)
                .show();
          else export.launch("Prayer-times-" + month + ".ics");
        });
    Ui.add(page, exportButton, 8);
    content = Ui.column(requireContext());
    Ui.add(page, content, 12);
    previous.setOnClickListener(
        v -> {
          month = month.minusMonths(1);
          load();
        });
    next.setOnClickListener(
        v -> {
          month = month.plusMonths(1);
          load();
        });
    load();
  }

  private void load() {
    monthLabel.setText(
        month
            .atDay(1)
            .format(
                DateTimeFormatter.ofPattern(
                    "MMMM yyyy",
                    I18n.english(requireContext())
                        ? Locale.ENGLISH
                        : Locale.forLanguageTag("ms-MY"))));
    days.clear();
    render();
    YearMonth selected = month;
    task(
        () ->
            new PrayerRepository(requireContext())
                .month(new AppPreferences(requireContext()).zone().code, selected, false),
        result -> {
          if (month.equals(selected)) {
            days.putAll(result.days);
            render();
          }
        },
        failure -> {
          if (month.equals(selected)) render();
        });
  }

  private void render() {
    content.removeAllViews();
    GridLayout grid = new GridLayout(requireContext());
    grid.setId(R.id.calendar_grid);
    boolean list =
        getResources().getConfiguration().fontScale > 1.3f
            || getResources().getConfiguration().screenWidthDp < 400;
    grid.setColumnCount(list ? 1 : 7);
    for (String label :
        list
            ? new String[] {}
            : I18n.english(requireContext())
                ? new String[] {"Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"}
                : new String[] {"Isn", "Sel", "Rab", "Kha", "Jum", "Sab", "Ahd"})
      cell(grid, label, "", null, false);
    int offset = month.atDay(1).getDayOfWeek().getValue() - 1;
    for (int i = 0; i < (list ? 0 : offset); i++) cell(grid, "", "", null, false);
    for (int day = 1; day <= month.lengthOfMonth(); day++) {
      LocalDate date = month.atDay(day);
      PrayerDay data = days.get(date);
      String hijri;
      if (data != null && !data.hijri.isEmpty()) {
        String[] pieces = data.hijri.split("-");
        hijri = pieces.length == 3 ? Integer.parseInt(pieces[2]) + " H" : "";
      } else {
        try {
          hijri = "~" + HijrahDate.from(date).get(ChronoField.DAY_OF_MONTH) + " H";
        } catch (Exception ignored) {
          hijri = "";
        }
      }
      cell(
          grid, String.valueOf(day), hijri, date, date.equals(LocalDate.now(PrayerClock.MALAYSIA)));
    }
    Ui.add(content, Ui.card(requireContext(), grid, 10), 0);
    Ui.add(
        content,
        Ui.text(
            requireContext(),
            days.size() < month.lengthOfMonth()
                ? "~ menandakan anggaran Hijri. Muat jadual zon untuk padanan"
                    + " e-Solat."
                : "Padanan Hijri daripada jadual e-Solat bagi zon pilihan anda.",
            11,
            false,
            R.color.text_secondary),
        12);
    Ui.add(
        content,
        Ui.text(
            requireContext(),
            "Tarikh penting · " + month.getYear(),
            19,
            true,
            R.color.text_primary),
        24);
    Ui.add(content, Ui.text(requireContext(), I18n.choose(requireContext(),
        "Tarikh rujukan tertakluk kepada pengisytiharan rasmi Malaysia.",
        "Reference dates remain subject to Malaysian official announcements."),
        12, false, R.color.text_secondary), 8);
    try {
      JSONArray years =
          new JSONObject(new ContentRepository(requireContext()).asset("islamic_dates.json"))
              .getJSONArray("data");
      boolean found = false;
      for (int i = 0; i < years.length(); i++) {
        JSONObject year = years.getJSONObject(i);
        if (!year.optString("year").equals(String.valueOf(month.getYear()))) continue;
        JSONArray events = year.getJSONArray("dates");
        for (int j = 0; j < events.length(); j++) {
          JSONObject event = events.getJSONObject(j);
          String title = I18n.english(requireContext()) ? event.optString("title_english", event.optString("title")) : event.optString("title"),
              date = event.optString("date"),
              hijri = event.optString("hijri_date");
          Ui.add(
              content,
              Ui.line(
                  requireContext(),
                  title,
                  date + " · " + hijri,
                  R.drawable.ic_calendar,
                  () ->
                      new MaterialAlertDialogBuilder(requireContext())
                          .setTitle(title)
                          .setMessage(date + "\n" + hijri)
                          .setPositiveButton("Tutup", null)
                          .show()),
              8);
          found = true;
        }
      }
      if (!found)
        Ui.add(
            content,
            Ui.text(
                requireContext(),
                "Rujukan peristiwa bagi tahun ini belum tersedia.",
                13,
                false,
                R.color.text_secondary),
            12);
    } catch (Exception failure) {
      Ui.add(
          content,
          Ui.text(
              requireContext(),
              "Rujukan peristiwa belum dapat dibaca.",
              13,
              false,
              R.color.text_secondary),
          12);
    }
  }

  private void cell(GridLayout grid, String day, String hijri, LocalDate date, boolean today) {
    if (grid.getColumnCount() == 1 && date != null)
      day =
          date.format(
              DateTimeFormatter.ofPattern(
                  "EEE, d MMM",
                  I18n.english(requireContext()) ? Locale.ENGLISH : Locale.forLanguageTag("ms")));
    LinearLayout box = Ui.column(requireContext());
    box.setGravity(Gravity.CENTER);
    box.setMinimumHeight(Ui.dp(requireContext(), 48));
    box.setPadding(0, Ui.dp(requireContext(), 12), 0, Ui.dp(requireContext(), 12));
    TextView label =
        Ui.text(
            requireContext(),
            day,
            day.length() > 2 ? 11 : 16,
            true,
            today ? R.color.white : R.color.text_primary);
    label.setGravity(Gravity.CENTER);
    box.addView(label);
    if (!hijri.isEmpty()) {
      TextView sub =
          Ui.text(
              requireContext(), hijri, 10, false, today ? R.color.white : R.color.text_secondary);
      sub.setGravity(Gravity.CENTER);
      box.addView(sub);
    }
    if (today) box.setBackground(Ui.background(requireContext(), R.color.colorPrimary, 16));
    GridLayout.LayoutParams p =
        new GridLayout.LayoutParams(
            GridLayout.spec(GridLayout.UNDEFINED), GridLayout.spec(GridLayout.UNDEFINED, 1f));
    p.width = 0;
    p.setGravity(Gravity.FILL_HORIZONTAL);
    p.setMargins(
        Ui.dp(requireContext(), 2),
        Ui.dp(requireContext(), 2),
        Ui.dp(requireContext(), 2),
        Ui.dp(requireContext(), 2));
    grid.addView(box, p);
    if (date != null)
      box.setOnClickListener(
          v -> {
            Bundle args = new Bundle();
            args.putString("date", date.toString());
            open(R.id.scheduleFragment, args);
          });
  }

  @Override
  public void onSaveInstanceState(@androidx.annotation.NonNull Bundle out) {
    out.putString("month", month.toString());
    super.onSaveInstanceState(out);
  }
}
