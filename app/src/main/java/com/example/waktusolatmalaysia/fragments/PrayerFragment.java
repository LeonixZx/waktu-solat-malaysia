package com.example.waktusolatmalaysia.fragments;

import android.os.*;
import android.view.*;
import android.widget.*;
import androidx.annotation.*;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.widget.TextViewCompat;
import androidx.lifecycle.ViewModelProvider;
import com.example.waktusolatmalaysia.R;
import com.example.waktusolatmalaysia.model.*;
import com.example.waktusolatmalaysia.preferences.*;
import com.example.waktusolatmalaysia.ui.*;
import com.example.waktusolatmalaysia.utils.*;
import com.example.waktusolatmalaysia.vms.PrayerViewModel;
import com.google.android.material.card.MaterialCardView;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;

public final class PrayerFragment extends FeatureFragment {
  private PrayerViewModel model;
  private AppPreferences prefs;
  private PrayerViewModel.State state;
  private boolean wide;
  private TextView clock, hijri, gregorian, nextName, nextTime, countdown, status;
  private android.view.View progress;
  private final List<TextView> names = new ArrayList<>(), times = new ArrayList<>();
  private final List<View> rows = new ArrayList<>();
  private final List<ImageButton> bells = new ArrayList<>();
  private int highlighted = -99;
  private final Handler handler = new Handler(Looper.getMainLooper());
  private final Runnable ticker =
      new Runnable() {
        public void run() {
          if (page == null) return;
          LocalDate today = LocalDate.now(PrayerClock.MALAYSIA);
          if (!model.date().equals(today)) model.select(today, prefs.zone());
          updateClock();
          handler.postDelayed(this, 1000);
        }
      };

  @Override
  protected int maxContentWidth() {
    return 1280;
  }

  @Override
  protected void build(@Nullable Bundle saved) {
    prefs = new AppPreferences(requireContext());
    model = new ViewModelProvider(this).get(PrayerViewModel.class);
    dashboard();
    page.addOnLayoutChangeListener(
        (v, l, t, r, b, ol, ot, or, ob) -> {
          boolean next = (r - l) / getResources().getDisplayMetrics().density >= 600;
          if (next != wide) {
            wide = next;
            dashboard();
          }
        });
    model
        .state()
        .observe(
            getViewLifecycleOwner(),
            s -> {
              state = s;
              applyState();
            });
  }

  private void dashboard() {
    if (page == null) return;
    page.removeAllViews();
    names.clear();
    times.clear();
    rows.clear();
    bells.clear();
    highlighted = -99;
    android.content.Context c = requireContext();
    View zone = Ui.quiet(c, prefs.zone().name + " · " + prefs.zone().code, R.drawable.ic_location);
    zone.setId(R.id.location_button);
    zone.setOnClickListener(v -> open(R.id.locationFragment));
    Ui.add(page, zone, 0);
    LinearLayout left = Ui.column(c), right = Ui.column(c);
    if (wide) {
      LinearLayout columns = Ui.row(c);
      columns.setGravity(Gravity.TOP);
      LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, -2, 1);
      lp.rightMargin = Ui.dp(c, 12);
      columns.addView(left, lp);
      LinearLayout.LayoutParams rp = new LinearLayout.LayoutParams(0, -2, 1);
      rp.leftMargin = Ui.dp(c, 12);
      columns.addView(right, rp);
      Ui.add(page, columns, 12);
    } else {
      Ui.add(page, left, 12);
      Ui.add(page, right, 0);
    }
    MaterialCardView hero = new MaterialCardView(c);
    hero.setRadius(Ui.dp(c, 28));
    hero.setStrokeWidth(0);
    hero.setCardElevation(0);
    ConstraintLayout frame = new ConstraintLayout(c);
    frame.setMinimumHeight(Ui.dp(c, 220));
    hero.addView(frame, new ViewGroup.LayoutParams(-1, -2));
    ImageView art = new ImageView(c);
    Artwork.bind(art, R.drawable.hero_mosque);
    art.setScaleType(ImageView.ScaleType.CENTER_CROP);
    ConstraintLayout.LayoutParams image = new ConstraintLayout.LayoutParams(0, 0);
    image.topToTop = 0;
    image.bottomToBottom = 0;
    image.startToStart = 0;
    image.endToEnd = 0;
    frame.addView(art, image);
    View wash = new View(c);
    wash.setBackgroundResource(R.drawable.bg_hero_scrim);
    ConstraintLayout.LayoutParams wp = new ConstraintLayout.LayoutParams(0, 0);
    wp.topToTop = wp.bottomToBottom = wp.startToStart = wp.endToEnd = 0;
    frame.addView(wash, wp);
    LinearLayout body = Ui.column(c);
    body.setPadding(Ui.dp(c, 20), Ui.dp(c, 16), Ui.dp(c, 20), Ui.dp(c, 16));
    ConstraintLayout.LayoutParams bp = new ConstraintLayout.LayoutParams(0, -2);
    bp.topToTop = bp.bottomToBottom = bp.startToStart = bp.endToEnd = 0;
    frame.addView(body, bp);
    clock = Ui.text(c, "", 52, true, R.color.hero_text);
    clock.setId(R.id.clock);
    clock.setMaxLines(1);
    TextViewCompat.setAutoSizeTextTypeUniformWithConfiguration(
        clock, 24, 56, 2, android.util.TypedValue.COMPLEX_UNIT_SP);
    body.addView(
        clock,
        new LinearLayout.LayoutParams(
            -1, Ui.dp(c, 72 * getResources().getConfiguration().fontScale)));
    hijri = Ui.text(c, "", 12, false, R.color.hero_text);
    hijri.setId(R.id.hijri);
    body.addView(hijri);
    gregorian = Ui.text(c, "", 11, false, R.color.hero_text);
    body.addView(gregorian);
    LinearLayout upcoming = Ui.column(c);
    Ui.add(upcoming, Ui.text(c, "SOLAT SETERUSNYA", 10, true, R.color.prayer_text), 0);
    LinearLayout line = Ui.row(c);
    nextName = Ui.text(c, "—", 20, true, R.color.text_primary);
    nextName.setId(R.id.next_name);
    nextTime = Ui.text(c, "—", 20, true, R.color.prayer_text);
    nextTime.setId(R.id.next_time);
    if (getResources().getConfiguration().fontScale > 1.3f) {
      line.setOrientation(LinearLayout.VERTICAL);
      line.addView(nextName);
      line.addView(nextTime);
    } else {
      line.addView(nextName, new LinearLayout.LayoutParams(0, -2, 1));
      line.addView(nextTime);
    }
    Ui.add(upcoming, line, 4);
    countdown = Ui.text(c, "", 13, false, R.color.text_secondary);
    countdown.setId(R.id.countdown);
    Ui.add(upcoming, countdown, 4);
    MaterialCardView next = Ui.card(c, upcoming, 12);
    next.setRadius(Ui.dp(c, 16));
    next.setOnClickListener(v -> open(R.id.scheduleFragment));
    Ui.add(body, next, 12);
    Ui.add(left, hero, 0);
    LinearLayout prayerHost = wide ? right : left;
    LinearLayout title = Ui.row(c);
    boolean large = getResources().getConfiguration().fontScale > 1.3f;
    if (large) title.setOrientation(LinearLayout.VERTICAL);
    title.addView(
        Ui.text(c, "Waktu Solat", 19, true, R.color.text_primary),
        new LinearLayout.LayoutParams(large ? -1 : 0, -2, large ? 0 : 1));
    View all = Ui.quiet(c, "Lihat semua", R.drawable.ic_chevron);
    all.setId(R.id.all_times);
    all.setOnClickListener(v -> open(R.id.scheduleFragment));
    title.addView(all);
    Ui.add(prayerHost, title, wide ? 0 : 16);
    LinearLayout table = Ui.column(c);
    table.setId(R.id.prayer_rows);
    int[] icons = {
      R.drawable.ic_sunrise,
      R.drawable.ic_sun,
      R.drawable.ic_cloud,
      R.drawable.ic_sunset,
      R.drawable.ic_moon
    };
    for (int position = 0; position < 5; position++) {
      final int index = PrayerDay.OBLIGATORY[position];
      LinearLayout row = Ui.row(c);
      row.setMinimumHeight(Ui.dp(c, 56));
      row.setPadding(Ui.dp(c, 8), Ui.dp(c, 4), Ui.dp(c, 4), Ui.dp(c, 4));
      row.addView(Ui.icon(c, icons[position], 24));
      TextView name = Ui.text(c, I18n.prayer(c, index), 14, true, R.color.text_primary);
      name.setId(R.id.name);
      TextView time = Ui.text(c, "—", 17, true, R.color.prayer_text);
      time.setId(R.id.time);
      if (getResources().getConfiguration().fontScale > 1.3f) {
        LinearLayout words = Ui.column(c);
        words.addView(name);
        words.addView(time);
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(0, -2, 1);
        p.leftMargin = Ui.dp(c, 12);
        row.addView(words, p);
      } else {
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(0, -2, 1);
        p.leftMargin = Ui.dp(c, 12);
        row.addView(name, p);
        row.addView(time);
      }
      ImageButton bell =
          Ui.iconButton(
              c,
              R.drawable.ic_bell,
              I18n.choose(c, "Peringatan ", "Reminder for ") + I18n.prayer(c, index),
              () -> ReminderSheet.show(this, index, this::applyState));
      bell.setBackgroundColor(android.graphics.Color.TRANSPARENT);
      row.addView(bell);
      table.addView(row);
      rows.add(row);
      names.add(name);
      times.add(time);
      bells.add(bell);
      row.setOnClickListener(v -> open(R.id.scheduleFragment));
    }
    Ui.add(prayerHost, Ui.card(c, table, 8), 4);
    LinearLayout shortcuts = Ui.column(c);
    Ui.add(shortcuts, Ui.text(c, "Ruang ibadah anda", 18, true, R.color.text_primary), 0);
    ShortcutGrid tools = new ShortcutGrid(c);
    tools.setId(R.id.shortcut_grid);
    String[] labels = {"Kiblat", "Tasbih", "Doa", "Hadis", "Kalendar", "Peringatan peribadi"};
    int[] dest = {
      R.id.qiblaFragment,
      R.id.tasbihFragment,
      R.id.collectionFragment,
      R.id.collectionFragment,
      R.id.calendarFragment,
      R.id.tasksFragment
    };
    int[] symbols = {
      R.drawable.ic_compass,
      R.drawable.ic_tasbih,
      R.drawable.ic_doa,
      R.drawable.ic_hadith,
      R.drawable.ic_calendar,
      R.drawable.ic_task
    };
    for (int i = 0; i < labels.length; i++) {
      final int item = i;
      tools.addShortcut(
          labels[i],
          symbols[i],
          () -> {
            if (item == 2 || item == 3)
              new UserStore(c).set("collection_tab", item == 3 ? "hadith" : "doa");
            open(dest[item]);
          });
    }
    Ui.add(shortcuts, tools, 12);
    Ui.add(left, shortcuts, 20);
    LinearLayout readingHost = right;
    UserStore store = new UserStore(c);
    Ui.add(
        readingHost,
        Ui.banner(
            c,
            R.drawable.banner_quran,
            "Sambung bacaan",
            store.text("last_name", "Al-Fatihah") + " · Ayat " + store.number("last_verse", 1),
            () -> {
              Bundle b = new Bundle();
              b.putInt("surah", store.number("last_surah", 1));
              b.putInt("verse", store.number("last_verse", 1));
              open(R.id.quranReaderFragment, b);
            }),
        16);
    Ui.add(
        readingHost,
        Ui.banner(
            c,
            R.drawable.banner_hikmah,
            "Ruang hikmah",
            I18n.choose(
                c, "Satu bacaan untuk direnungkan hari ini.", "A reading to reflect on today."),
            () -> {
              store.set("collection_tab", "hadith");
              open(R.id.collectionFragment);
            }),
        16);
    if (state != null && state.day != null && state.day.hijri.contains("-09-"))
      Ui.add(
          left,
          Ui.banner(
              c,
              R.drawable.ramadan_v3,
              "Ramadan",
              I18n.choose(c, "Kalendar dan amalan harian", "Calendar and daily practice"),
              () -> open(R.id.calendarFragment)),
          16);
    Ui.add(
        left,
        Ui.line(
            c,
            "Panduan ringkas",
            I18n.choose(
                c, "Zon, widget dan bacaan luar talian", "Zones, widgets and offline reading"),
            R.drawable.ic_info,
            () -> open(R.id.onboardingFragment)),
        16);
    status = Ui.text(c, "", 11, false, R.color.text_secondary);
    status.setId(R.id.status);
    Ui.add(page, status, 20);
    progress = new ProgressBar(c);
    progress.setId(R.id.progress);
    Ui.add(page, progress, 4);
    View refresh = Ui.quiet(c, "Segarkan jadual", R.drawable.ic_refresh);
    refresh.setId(R.id.refresh);
    refresh.setOnClickListener(v -> model.load(prefs.zone(), true));
    Ui.add(page, refresh, 4);
    applyState();
  }

  private DateTimeFormatter format() {
    return DateTimeFormatter.ofPattern(prefs.isTwelveHour() ? "h:mm a" : "HH:mm", Locale.ENGLISH);
  }

  private void applyState() {
    if (page == null || status == null) return;
    if (state != null) {
      progress.setVisibility(state.loading ? View.VISIBLE : View.GONE);
      status.setText(
          state.loading
              ? R.string.loading
              : state.error
                  ? R.string.network_error
                  : state.offline
                      ? R.string.offline_status
                      : state.cached ? R.string.cache_status : R.string.live_status);
      for (int p = 0; p < times.size(); p++) {
        int i = PrayerDay.OBLIGATORY[p];
        times.get(p).setText(state.day == null ? "—" : state.day.times.get(i).format(format()));
        PrayerMode mode = PrayerMode.get(requireContext(), i);
        bells.get(p).setAlpha(mode == PrayerMode.OFF ? .4f : 1f);
        bells
            .get(p)
            .setImageResource(mode == PrayerMode.AZAN ? R.drawable.ic_prayer : R.drawable.ic_bell);
      }
      if (state.day != null && !state.loading)
        com.example.waktusolatmalaysia.widgets.PrayerWidgets.refreshAsync(requireContext());
    }
    updateClock();
  }

  private void updateClock() {
    if (clock == null || page == null) return;
    ZonedDateTime now = ZonedDateTime.now(PrayerClock.MALAYSIA);
    String value = now.format(format());
    if (!clock.getText().toString().equals(value)) clock.setText(value);
    Ui.setTextChanged(
        gregorian,
        now.format(
            DateTimeFormatter.ofPattern(
                "EEEE, d MMMM yyyy",
                I18n.english(requireContext()) ? Locale.ENGLISH : Locale.forLanguageTag("ms"))));
    Ui.setTextChanged(
        hijri,
        state != null && state.day != null ? readableHijri(state.day.hijri) : "MALAYSIA · UTC+8");
    PrayerClock.Next next = state == null ? null : PrayerClock.next(state.day, state.tomorrow, now);
    Ui.setTextChanged(
        nextName,
        next == null
            ? "—"
            : I18n.prayer(requireContext(), next.index)
                + (next.tomorrow ? I18n.choose(requireContext(), " esok", " tomorrow") : ""));
    Ui.setTextChanged(nextTime, next == null ? "—" : next.time.format(format()));
    if (next != null) {
      long sec = next.secondsFrom(now);
      countdown.setText(
          String.format(Locale.ROOT, "%02d:%02d:%02d", sec / 3600, sec % 3600 / 60, sec % 60)
              + I18n.choose(requireContext(), " lagi", " remaining"));
    } else
      countdown.setText(
          I18n.t(
              requireContext(),
              state != null && state.loading
                  ? "Memuatkan jadual…"
                  : "Jadual seterusnya belum tersedia"));
    int selected = next == null || next.tomorrow ? -1 : next.index;
    if (selected != highlighted) {
      highlighted = selected;
      for (int p = 0; p < rows.size(); p++)
        rows.get(p)
            .setBackground(
                Ui.background(
                    requireContext(),
                    PrayerDay.OBLIGATORY[p] == selected ? R.color.soft_gold : R.color.surface,
                    12));
    }
  }

  private String readableHijri(String v) {
    try {
      String[] p = v.split("-");
      return Integer.parseInt(p[2])
          + " "
          + getResources().getStringArray(R.array.hijri_months)[Integer.parseInt(p[1]) - 1]
          + " "
          + p[0]
          + " H";
    } catch (Exception e) {
      return v;
    }
  }

  @Override
  public void onResume() {
    super.onResume();
    model.ensure(prefs.zone());
    applyState();
    handler.post(ticker);
  }

  @Override
  public void onPause() {
    handler.removeCallbacks(ticker);
    super.onPause();
  }

  @Override
  public void onDestroyView() {
    handler.removeCallbacks(ticker);
    clock = null;
    status = null;
    rows.clear();
    names.clear();
    times.clear();
    bells.clear();
    super.onDestroyView();
  }
}
