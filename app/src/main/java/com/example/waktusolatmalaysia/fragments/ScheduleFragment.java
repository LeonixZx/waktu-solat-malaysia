package com.example.waktusolatmalaysia.fragments;

import android.app.*;
import android.content.*;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.*;
import android.provider.Settings;
import android.view.*;
import android.widget.*;
import androidx.activity.result.*;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.*;
import com.example.waktusolatmalaysia.R;
import com.example.waktusolatmalaysia.model.*;
import com.example.waktusolatmalaysia.preferences.*;
import com.example.waktusolatmalaysia.repo.*;
import com.example.waktusolatmalaysia.services.*;
import com.example.waktusolatmalaysia.ui.I18n;
import com.example.waktusolatmalaysia.ui.ReminderSheet;
import com.example.waktusolatmalaysia.ui.Ui;
import com.example.waktusolatmalaysia.utils.PrayerClock;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.tabs.TabLayout;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;

public final class ScheduleFragment extends FeatureFragment {
  private LocalDate date = LocalDate.now(PrayerClock.MALAYSIA);
  private int mode = 0, pending = -1, generation;
  private LinearLayout rows;
  private TextView label, status;
  private MaterialButton today, preview;
  private MediaPlayer player;
  private UserStore store;
  private AppPreferences prefs;
  private String loadedZone = "";
  private PrayerZone overrideZone;

  private PrayerZone selectedZone() {
    return overrideZone == null ? prefs.zone() : overrideZone;
  }

  private final ActivityResultLauncher<String> permission =
      registerForActivityResult(
          new ActivityResultContracts.RequestPermission(),
          granted -> {
            if (pending >= 0) {
              store.set("reminder_" + pending, granted);
              pending = -1;
              ReminderScheduler.reschedule(requireContext());
              load(false);
            }
          });

  @Override
  protected void build(@Nullable Bundle saved) {
    store = new UserStore(requireContext());
    prefs = new AppPreferences(requireContext());
    if (saved != null) {
      date = LocalDate.parse(saved.getString("date", date.toString()));
      mode = saved.getInt("mode", 0);
    } else if (getArguments() != null && getArguments().containsKey("date"))
      date = LocalDate.parse(getArguments().getString("date"));
    if (getArguments() != null && getArguments().getString("zone") != null) {
      String code = getArguments().getString("zone");
      for (java.util.List<PrayerZone> group : ZoneRepository.load(requireContext()).values())
        for (PrayerZone z : group) if (z.code.equals(code)) overrideZone = z;
    }
    heading("Jadual solat", "Waktu e-Solat JAKIM mengikut zon pilihan anda.");
    Ui.add(
        page,
        Ui.line(
            requireContext(),
            selectedZone().name,
            selectedZone().code + " Â· Tukar zon",
            R.drawable.ic_location,
            () -> open(R.id.locationFragment)),
        16);
    TabLayout tabs = new TabLayout(requireContext());
    tabs.setId(R.id.schedule_tabs);
    tabs.setBackgroundColor(android.graphics.Color.TRANSPARENT);
    for (String t : new String[] {"Harian", "Mingguan", "Bulanan"})
      tabs.addTab(tabs.newTab().setText(t));
    tabs.getTabAt(mode).select();
    Ui.add(page, tabs, 16);
    LinearLayout controls = Ui.row(requireContext());
    MaterialButton prev = Ui.quiet(requireContext(), "â€¹", 0),
        next = Ui.quiet(requireContext(), "â€º", 0);
    prev.setId(R.id.previous);
    next.setId(R.id.next);
    prev.setContentDescription("Sebelumnya");
    next.setContentDescription("Seterusnya");
    controls.addView(prev, new LinearLayout.LayoutParams(Ui.dp(requireContext(), 48), -2));
    label = Ui.text(requireContext(), "", 14, true, R.color.text_primary);
    label.setId(R.id.date_button);
    label.setGravity(Gravity.CENTER);
    label.setPadding(0, Ui.dp(requireContext(), 14), 0, Ui.dp(requireContext(), 14));
    controls.addView(label, new LinearLayout.LayoutParams(0, -2, 1));
    controls.addView(next, new LinearLayout.LayoutParams(Ui.dp(requireContext(), 48), -2));
    Ui.add(page, controls, 12);
    today = Ui.quiet(requireContext(), "Hari ini", R.drawable.ic_calendar);
    today.setId(R.id.today);
    Ui.add(page, today, 0);
    prev.setOnClickListener(
        v -> {
          date = mode == 2 ? date.minusMonths(1) : date.minusDays(mode == 1 ? 7 : 1);
          load(false);
        });
    next.setOnClickListener(
        v -> {
          date = mode == 2 ? date.plusMonths(1) : date.plusDays(mode == 1 ? 7 : 1);
          load(false);
        });
    today.setOnClickListener(
        v -> {
          date = LocalDate.now(PrayerClock.MALAYSIA);
          load(false);
        });
    label.setOnClickListener(
        v ->
            new DatePickerDialog(
                    requireContext(),
                    (picker, y, m, d) -> {
                      date = LocalDate.of(y, m + 1, d);
                      load(false);
                    },
                    date.getYear(),
                    date.getMonthValue() - 1,
                    date.getDayOfMonth())
                .show());
    status = Ui.text(requireContext(), "", 12, false, R.color.text_secondary);
    status.setId(R.id.status);
    Ui.add(page, status, 12);
    rows = Ui.column(requireContext());
    rows.setId(R.id.schedule_rows);
    Ui.add(page, rows, 12);
    MaterialButton refresh = Ui.quiet(requireContext(), "Segarkan jadual", R.drawable.ic_refresh);
    refresh.setId(R.id.refresh);
    refresh.setOnClickListener(v -> load(true));
    Ui.add(page, refresh, 12);
    Ui.add(
        page, Ui.text(requireContext(), "Peringatan & azan", 20, true, R.color.text_primary), 28);
    Ui.add(
        page,
        Ui.text(
            requireContext(),
            "Hidupkan ikon loceng pada jadual harian untuk setiap solat. Peringatan menggunakan"
                + " jadual yang telah dimuat turun; buka aplikasi untuk menyimpan bulan baharu.",
            12,
            false,
            R.color.text_secondary),
        8);
    Ui.add(page, new com.example.waktusolatmalaysia.ui.ReminderDiagnostics(this), 8);
    tabs.addOnTabSelectedListener(
        new TabLayout.OnTabSelectedListener() {
          public void onTabSelected(TabLayout.Tab t) {
            mode = t.getPosition();
            load(false);
          }

          public void onTabUnselected(TabLayout.Tab t) {}

          public void onTabReselected(TabLayout.Tab t) {}
        });
    load(false);
  }

  private void load(boolean refresh) {
    if (rows == null) return;
    int token = ++generation;
    LocalDate selected = date;
    int selectedMode = mode;
    String zone = selectedZone().code;
    loadedZone = zone;
    label.setText(
        date.format(
            DateTimeFormatter.ofPattern(
                mode == 2 ? "MMMM yyyy" : "d MMM yyyy",
                I18n.english(requireContext()) ? Locale.ENGLISH : Locale.forLanguageTag("ms-MY"))));
    today.setVisibility(
        date.equals(LocalDate.now(PrayerClock.MALAYSIA)) ? View.GONE : View.VISIBLE);
    rows.removeAllViews();
    status.setText("Memuatkan jadualâ€¦");
    task(
        () -> {
          LocalDate first = selectedMode == 2 ? YearMonth.from(selected).atDay(1) : selected,
              last =
                  selectedMode == 2
                      ? YearMonth.from(selected).atEndOfMonth()
                      : selected.plusDays(selectedMode == 1 ? 6 : 0);
          Map<LocalDate, PrayerDay> result = new TreeMap<>();
          boolean offline = false, cached = true;
          Set<YearMonth> months = new HashSet<>();
          for (LocalDate d = first; !d.isAfter(last); d = d.plusDays(1))
            months.add(YearMonth.from(d));
          PrayerRepository repository = new PrayerRepository(requireContext());
          for (YearMonth m : months) {
            PrayerRepository.Month data = repository.month(zone, m, refresh);
            result.putAll(data.days);
            offline |= data.offline;
            cached &= data.cached;
          }
          return new Loaded(result, first, last, offline, cached);
        },
        data -> {
          if (token != generation) return;
          rows.removeAllViews();
          status.setText(
              data.offline
                  ? R.string.offline_status
                  : data.cached ? R.string.cache_status : R.string.live_status);
          if (selectedMode == 0) {
            PrayerDay day = data.days.get(selected);
            if (day == null) status.setText(R.string.no_data);
            else daily(day);
          } else period(data, selectedMode);
          ReminderScheduler.reschedule(requireContext());
        },
        error -> {
          if (token != generation) return;
          rows.removeAllViews();
          status.setText(R.string.network_error);
        });
  }

  private DateTimeFormatter time() {
    return DateTimeFormatter.ofPattern(prefs.isTwelveHour() ? "h:mm a" : "HH:mm", Locale.ENGLISH);
  }

  private void daily(PrayerDay day) {
    int[] ids = {
      0,
      R.id.reminder_fajr,
      0,
      0,
      R.id.reminder_dhuhr,
      R.id.reminder_asr,
      R.id.reminder_maghrib,
      R.id.reminder_isha
    };
    for (int i = 0; i < 8; i++) {
      final int index = i;
      LinearLayout row = Ui.row(requireContext());
      row.setPadding(
          Ui.dp(requireContext(), 12),
          Ui.dp(requireContext(), 8),
          Ui.dp(requireContext(), 8),
          Ui.dp(requireContext(), 8));
      row.setMinimumHeight(Ui.dp(requireContext(), 64));
      row.setBackground(Ui.background(requireContext(), R.color.surface, 16));
      LinearLayout words = Ui.column(requireContext());
      TextView name =
          Ui.text(
              requireContext(), I18n.prayer(requireContext(), i), 14, true, R.color.text_primary);
      name.setId(R.id.name);
      words.addView(name);
      TextView value =
          Ui.text(requireContext(), day.times.get(i).format(time()), 18, true, R.color.prayer_text);
      value.setId(R.id.time);
      words.addView(value);
      row.addView(words, new LinearLayout.LayoutParams(0, -2, 1));
      if (ids[i] != 0) {
        PrayerMode mode = PrayerMode.get(requireContext(), i);
        MaterialButton bell =
            Ui.quiet(
                requireContext(),
                mode == PrayerMode.OFF
                    ? "Matikan"
                    : mode == PrayerMode.AZAN ? "Azan" : "Pemberitahuan",
                R.drawable.ic_bell);
        bell.setId(ids[i]);
        bell.setOnClickListener(v -> ReminderSheet.show(this, index, () -> load(false)));
        if (overrideZone != null && !overrideZone.code.equals(prefs.zone().code)) {
          bell.setText(I18n.choose(requireContext(), "Zon lain", "Other zone"));
          bell.setOnClickListener(
              v ->
                  new com.google.android.material.dialog.MaterialAlertDialogBuilder(
                          requireContext())
                      .setMessage(
                          I18n.choose(
                              requireContext(),
                              "Peringatan menggunakan zon utama aplikasi. Tukar zon utama untuk"
                                  + " mengaktifkannya di sini.",
                              "Reminders use the appâ€™s main zone. Change the main zone to enable"
                                  + " them here."))
                      .setPositiveButton(I18n.t(requireContext(), "Tutup"), null)
                      .show());
        }
        row.addView(bell);
      }
      Ui.add(rows, row, 8);
    }
  }

  private void period(Loaded data, int mode) {
    HorizontalScrollView horizontal = new HorizontalScrollView(requireContext());
    LinearLayout table = Ui.column(requireContext());
    horizontal.addView(table);
    LinearLayout head = Ui.row(requireContext());
    table.addView(head);
    cell(head, "Tarikh", 100, true);
    for (int i : PrayerDay.OBLIGATORY) cell(head, PrayerAlarmReceiver.NAMES[i], 83, true);
    for (LocalDate d = data.first; !d.isAfter(data.last); d = d.plusDays(1)) {
      LinearLayout row = Ui.row(requireContext());
      row.setBackground(
          Ui.background(
              requireContext(),
              d.equals(LocalDate.now(PrayerClock.MALAYSIA)) ? R.color.soft_gold : R.color.surface,
              10));
      cell(row, d.format(DateTimeFormatter.ofPattern("d MMM", Locale.ENGLISH)), 100, true);
      PrayerDay day = data.days.get(d);
      for (int i : PrayerDay.OBLIGATORY)
        cell(row, day == null ? "â€”" : day.times.get(i).format(time()), 83, false);
      Ui.add(table, row, 4);
    }
    Ui.add(rows, horizontal, 0);
  }

  private void cell(LinearLayout row, String text, int width, boolean bold) {
    TextView cell = Ui.text(requireContext(), text, 12, bold, R.color.text_primary);
    cell.setPadding(
        Ui.dp(requireContext(), 10), Ui.dp(requireContext(), 14), 0, Ui.dp(requireContext(), 14));
    row.addView(cell, new LinearLayout.LayoutParams(Ui.dp(requireContext(), width), -2));
  }

  private void stopPreview() {
    if (player != null) {
      player.release();
      player = null;
    }
    if (preview != null) preview.setText("Dengar contoh azan");
  }

  @Override
  public void onResume() {
    super.onResume();
    if (prefs != null && !selectedZone().code.equals(loadedZone)) load(false);
    ReminderScheduler.reschedule(requireContext());
  }

  @Override
  public void onStop() {
    stopPreview();
    super.onStop();
  }

  @Override
  public void onSaveInstanceState(@NonNull Bundle out) {
    out.putString("date", date.toString());
    out.putInt("mode", mode);
    super.onSaveInstanceState(out);
  }

  @Override
  public void onDestroyView() {
    generation++;
    rows = null;
    preview = null;
    super.onDestroyView();
  }

  private static final class Loaded {
    final Map<LocalDate, PrayerDay> days;
    final LocalDate first, last;
    final boolean offline, cached;

    Loaded(Map<LocalDate, PrayerDay> d, LocalDate f, LocalDate l, boolean o, boolean c) {
      days = d;
      first = f;
      last = l;
      offline = o;
      cached = c;
    }
  }
}
