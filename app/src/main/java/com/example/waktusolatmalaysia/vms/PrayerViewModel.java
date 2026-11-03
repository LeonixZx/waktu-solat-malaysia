package com.example.waktusolatmalaysia.vms;

import android.app.Application;
import android.os.Handler;
import android.os.Looper;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import com.example.waktusolatmalaysia.model.PrayerDay;
import com.example.waktusolatmalaysia.model.PrayerZone;
import com.example.waktusolatmalaysia.repo.PrayerRepository;
import com.example.waktusolatmalaysia.utils.PrayerClock;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public final class PrayerViewModel extends AndroidViewModel {
  private final PrayerRepository repository;
  private final MutableLiveData<State> state = new MutableLiveData<>();
  private final ExecutorService worker = Executors.newSingleThreadExecutor();
  private final Handler main = new Handler(Looper.getMainLooper());
  private Future<?> pending;
  private int generation;
  private LocalDate date = LocalDate.now(PrayerClock.MALAYSIA);
  private boolean followToday = true;

  public PrayerViewModel(@NonNull Application application) {
    super(application);
    repository = new PrayerRepository(application);
  }

  public LiveData<State> state() {
    return state;
  }

  public LocalDate date() {
    return date;
  }

  public boolean followsToday() {
    return followToday;
  }

  public void select(LocalDate date, PrayerZone zone) {
    this.date = date;
    followToday = date.equals(LocalDate.now(PrayerClock.MALAYSIA));
    load(zone, false);
  }

  public void ensure(PrayerZone zone) {
    if (followToday) date = LocalDate.now(PrayerClock.MALAYSIA);
    State current = state.getValue();
    if (current == null
        || !current.zone.code.equals(zone.code)
        || !current.zone.name.equals(zone.name)
        || !current.date.equals(date)) load(zone, false);
  }

  public void load(PrayerZone zone, boolean refresh) {
    int request = ++generation;
    if (pending != null) pending.cancel(true);
    LocalDate selected = date;
    State old = state.getValue();
    boolean same = old != null && old.date.equals(selected) && old.zone.code.equals(zone.code);
    state.setValue(
        new State(
            selected,
            zone,
            same ? old.day : null,
            same ? old.tomorrow : null,
            true,
            false,
            false,
            false));
    pending =
        worker.submit(
            () -> {
              PrayerDay day = null;
              PrayerDay tomorrow = null;
              boolean error = false;
              boolean cached = false;
              boolean offline = false;
              try {
                PrayerRepository.Month month =
                    repository.month(zone.code, YearMonth.from(selected), refresh);
                day = month.days.get(selected);
                error = day == null;
                cached = month.cached;
                offline = month.offline;
                LocalDate next = selected.plusDays(1);
                if (next.getMonth() == selected.getMonth()) tomorrow = month.days.get(next);
                else if (selected.equals(LocalDate.now(PrayerClock.MALAYSIA))) {
                  try {
                    tomorrow =
                        repository.month(zone.code, YearMonth.from(next), refresh).days.get(next);
                  } catch (Exception ignored) {
                    /* No guessed times across a missing month. */
                  }
                }
              } catch (Exception failure) {
                error = true;
              }
              State result =
                  new State(selected, zone, day, tomorrow, false, error, cached, offline);
              main.post(
                  () -> {
                    if (request == generation) state.setValue(result);
                  });
            });
  }

  @Override
  protected void onCleared() {
    generation++;
    worker.shutdownNow();
    main.removeCallbacksAndMessages(null);
  }

  public static final class State {
    public final LocalDate date;
    public final PrayerZone zone;
    public final PrayerDay day;
    public final PrayerDay tomorrow;
    public final boolean loading, error, cached, offline;

    State(
        LocalDate date,
        PrayerZone zone,
        PrayerDay day,
        PrayerDay tomorrow,
        boolean loading,
        boolean error,
        boolean cached,
        boolean offline) {
      this.date = date;
      this.zone = zone;
      this.day = day;
      this.tomorrow = tomorrow;
      this.loading = loading;
      this.error = error;
      this.cached = cached;
      this.offline = offline;
    }
  }
}
