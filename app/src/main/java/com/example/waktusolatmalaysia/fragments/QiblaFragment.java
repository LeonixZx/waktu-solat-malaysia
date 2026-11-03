package com.example.waktusolatmalaysia.fragments;

import android.Manifest;
import android.content.*;
import android.content.pm.PackageManager;
import android.hardware.*;
import android.location.*;
import android.net.Uri;
import android.os.*;
import android.view.*;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.*;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import com.example.waktusolatmalaysia.R;
import com.example.waktusolatmalaysia.databinding.ScreenQiblaBinding;
import com.example.waktusolatmalaysia.ui.*;
import com.example.waktusolatmalaysia.utils.*;
import java.util.Locale;

public final class QiblaFragment extends Fragment implements SensorEventListener, LocationListener {
  private ScreenQiblaBinding binding;
  private SensorManager sensors;
  private LocationManager locations;
  private Sensor rotation, accelerometer, magnetometer;
  private Location fix;
  private final QiblaState state = new QiblaState();
  private final Handler handler = new Handler(Looper.getMainLooper());
  private final float[] matrix = new float[9], remapped = new float[9], orientation = new float[3];
  private float[] gravity, magnetic;
  private long gravityAt = -1, magneticAt = -1;
  private int gravityAccuracy, magneticAccuracy;
  private boolean requesting;
  private final Runnable timeout = () -> {
    requesting = false;
    if (binding != null) {
      binding.locate.setEnabled(true);
      if (fix == null || age(fix) > 120000)
        binding.coordinates.setText(I18n.choose(requireContext(),
            "Lokasi baharu belum diterima. Kemas kini diteruskan semasa skrin ini dibuka.",
            "No fresh location yet. Updates continue while this screen is open."));
    }
  };
  private final Runnable tick = new Runnable() {
    public void run() { if (binding != null) { render(); handler.postDelayed(this, 500); } }
  };
  private final ActivityResultLauncher<String[]> permission = registerForActivityResult(
      new ActivityResultContracts.RequestMultiplePermissions(), result -> {
        if (binding == null) return;
        if (hasPermission()) locate(); else binding.coordinates.setText(R.string.location_denied);
      });

  @Override public View onCreateView(@NonNull LayoutInflater i, ViewGroup parent, Bundle saved) {
    binding = ScreenQiblaBinding.inflate(i, parent, false);
    return binding.getRoot();
  }
  @Override public void onViewCreated(@NonNull View view, Bundle saved) {
    sensors = (SensorManager) requireContext().getSystemService(Context.SENSOR_SERVICE);
    locations = (LocationManager) requireContext().getSystemService(Context.LOCATION_SERVICE);
    binding.locate.setOnClickListener(v -> {
      if (hasPermission()) locate();
      else permission.launch(new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION});
    });
    binding.reference.setOnClickListener(v -> startActivity(new Intent(Intent.ACTION_VIEW,
        Uri.parse("https://efalak.muftiselangor.gov.my"))));
    if (saved != null) {
      fix = saved.getParcelable("qibla_location");
      if (fix != null) displayLocation(fix);
    }
    binding.title.setText(I18n.choose(requireContext(), "Kenali arah, semak keyakinan.", "Direction with clear confidence."));
    binding.reference.setText(I18n.choose(requireContext(), "Semak rujukan rasmi · eFalak Selangor", "Official reference · eFalak Selangor"));
    binding.guideTitle.setText(I18n.choose(requireContext(), "Sebelum menyemak arah", "Before checking direction"));
    binding.guideBody.setText(I18n.choose(requireContext(),
        "Letakkan peranti rata, tanggalkan penutup magnet atau papan kekunci dan jauhkan daripada logam. Putar perlahan untuk menentukur. Arah ini panduan; bandingkan dengan tanda Kiblat yang telah disahkan. eFalak ialah rujukan Jabatan Mufti Selangor.",
        "Lay the device flat, remove magnetic covers/keyboards and move away from metal. Rotate slowly to calibrate. This is guidance; compare with an independently verified Qibla marker. eFalak is a Selangor Mufti reference."));
  }
  private boolean hasPermission() {
    return ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        || ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED;
  }
  @SuppressWarnings("MissingPermission") private void locate() {
    if (!hasPermission() || binding == null || requesting) return;
    stopLocation(); requesting = true;
    binding.locate.setEnabled(false); binding.coordinates.setText(R.string.location_wait);
    Location recent = null; boolean requested = false;
    try {
      for (String provider : new String[]{LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER}) {
        try {
        if (!locations.isProviderEnabled(provider)) continue;
        Location cached = locations.getLastKnownLocation(provider);
        if (valid(cached) && age(cached) <= 120000 && (recent == null || cached.getAccuracy() < recent.getAccuracy())) recent = cached;
        locations.requestLocationUpdates(provider, 1000, 0, this, Looper.getMainLooper()); requested = true;
        } catch (SecurityException | IllegalArgumentException deniedProvider) {
          // Approximate permission can allow the network provider while GPS is denied.
        }
      }
      if (recent != null) {
        // A usable cached fix may be shown immediately, but must not cancel live updates.
        fix = recent; displayLocation(recent); requesting = false; binding.locate.setEnabled(true);
      }
      if (requested) handler.postDelayed(timeout, 45000); else timeout.run();
    } catch (SecurityException | IllegalArgumentException e) { timeout.run(); }
  }
  private boolean valid(Location l) {
    return l != null && l.hasAccuracy() && Float.isFinite(l.getAccuracy()) && l.getAccuracy() >= 0
        && Double.isFinite(QiblaMath.bearing(l.getLatitude(), l.getLongitude()));
  }
  private long age(Location l) {
    return QiblaState.locationAge(SystemClock.elapsedRealtimeNanos(), l.getElapsedRealtimeNanos(), System.currentTimeMillis(), l.getTime());
  }
  private void displayLocation(Location l) {
    if (binding == null || !valid(l)) return;
    GeomagneticField field = new GeomagneticField((float) l.getLatitude(), (float) l.getLongitude(), (float) l.getAltitude(), System.currentTimeMillis());
    state.location(l.getLatitude(), l.getLongitude(), field.getDeclination(), SystemClock.elapsedRealtime() - age(l), l.getAccuracy(), l.getProvider());
    binding.bearing.setText(getString(R.string.qibla_degrees, state.bearing));
    binding.coordinates.setText(I18n.choose(requireContext(), "Utara benar", "True north") + " · ±" + Math.round(l.getAccuracy()) + " m · " + l.getProvider());
    render();
  }
  @Override public void onLocationChanged(@NonNull Location l) {
    if (!valid(l) || age(l) > 120000) return;
    handler.removeCallbacks(timeout); requesting = false; fix = l;
    if (binding != null) { binding.locate.setEnabled(true); displayLocation(l); }
  }
  @Override public void onProviderEnabled(@NonNull String p) {}
  @Override public void onProviderDisabled(@NonNull String p) { if (requesting) timeout.run(); }
  @Override public void onStatusChanged(String p, int status, Bundle b) {}
  private void stopLocation() {
    handler.removeCallbacks(timeout); requesting = false;
    try { if (locations != null) locations.removeUpdates(this); } catch (SecurityException ignored) {}
  }
  @Override public void onResume() {
    super.onResume(); state.resetHeading(); gravity = magnetic = null; gravityAt = magneticAt = -1;
    rotation = null;
    for (int type : new int[]{Sensor.TYPE_ROTATION_VECTOR, Sensor.TYPE_GEOMAGNETIC_ROTATION_VECTOR}) {
      Sensor candidate = sensors.getDefaultSensor(type);
      if (candidate != null && sensors.registerListener(this, candidate, SensorManager.SENSOR_DELAY_UI)) { rotation = candidate; break; }
    }
    if (rotation == null) {
      accelerometer = sensors.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
      magnetometer = sensors.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
      state.available = accelerometer != null && magnetometer != null
          && sensors.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_UI)
          && sensors.registerListener(this, magnetometer, SensorManager.SENSOR_DELAY_UI);
      state.source = state.available ? "accelerometer + magnetometer" : "unavailable";
    } else { state.available = true; state.source = rotation.getType() == Sensor.TYPE_ROTATION_VECTOR ? "rotation vector" : "geomagnetic rotation vector"; }
    handler.post(tick);
    // onPause removes subscriptions, so every resume must restore them, even with a fresh fix.
    if (hasPermission()) locate();
  }
  @Override public void onPause() {
    sensors.unregisterListener(this); handler.removeCallbacks(tick); stopLocation();
    if (binding != null) binding.locate.setEnabled(true);
    super.onPause();
  }
  @SuppressWarnings("deprecation") @Override public void onSensorChanged(SensorEvent event) {
    if (binding == null) return;
    long now = SystemClock.elapsedRealtime(); double uncertainty = Double.NaN; int accuracy = event.accuracy;
    if (rotation != null && event.sensor.getType() == rotation.getType()) {
      SensorManager.getRotationMatrixFromVector(matrix, event.values);
      if (event.values.length >= 5 && event.values[4] >= 0) uncertainty = Math.toDegrees(event.values[4]);
    } else if (rotation == null) {
      if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) { gravity = event.values.clone(); gravityAt = now; gravityAccuracy = event.accuracy; }
      if (event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD) { magnetic = event.values.clone(); magneticAt = now; magneticAccuracy = event.accuracy; }
      if (gravity == null || magnetic == null || now - gravityAt > 2000 || now - magneticAt > 2000
          || !SensorManager.getRotationMatrix(matrix, null, gravity, magnetic)) return;
      accuracy = Math.min(gravityAccuracy, magneticAccuracy);
    } else return;
    int display = requireActivity().getWindowManager().getDefaultDisplay().getRotation();
    if (!QiblaOrientation.remap(matrix, display, remapped)) return;
    SensorManager.getOrientation(remapped, orientation);
    double tilt = Math.toDegrees(Math.acos(Math.max(-1, Math.min(1, remapped[8]))));
    state.sample(Math.toDegrees(orientation[0]), tilt, uncertainty, accuracy, now);
    render();
  }
  private void render() {
    if (binding == null) return;
    long now = SystemClock.elapsedRealtime(); QiblaState.Quality quality = state.quality(now);
    binding.compass.setDirection(Double.isFinite(state.heading) ? (float) state.heading : 0,
        state.headingAt >= 0 ? (float) state.bearing : Float.NaN);
    binding.compass.setAlpha(quality == QiblaState.Quality.READY ? 1 : .55f);
    String message;
    switch (quality) {
      case ACQUIRING_LOCATION: message = I18n.choose(requireContext(), "Dapatkan lokasi untuk mengira arah Kiblat.", "Get a location to calculate the Qibla bearing."); break;
      case ACQUIRING_HEADING: message = I18n.choose(requireContext(), "Menunggu bacaan kompas…", "Waiting for compass readings…"); break;
      case UNAVAILABLE: message = I18n.choose(requireContext(), "Kompas tiada. Gunakan nilai arah dari utara benar dan rujukan rasmi.", "Compass unavailable. Use the true-north bearing and official reference."); break;
      case STALE: message = I18n.choose(requireContext(), "Bacaan sudah lama. Segarkan lokasi atau tunggu sensor.", "Reading is stale. Refresh location or wait for the sensor."); break;
      case LOW_CONFIDENCE: message = state.tilt > 25 ? I18n.choose(requireContext(), "Letakkan peranti rata (dalam 25°).", "Lay the device flat (within 25°).")
          : I18n.choose(requireContext(), "Keyakinan rendah. Tanggalkan penutup magnet dan tentukur semula.", "Low confidence. Remove magnetic covers and recalibrate."); break;
      default:
        double delta = QiblaState.delta(state.bearing, state.heading);
        message = state.aligned(now) ? I18n.choose(requireContext(), "Hampir sejajar · anggaran ±5°", "Approximately aligned · ±5° band")
            : String.format(Locale.ROOT, I18n.choose(requireContext(), "Putar %d° ke %s", "Turn %d° %s"), Math.round(Math.abs(delta)),
                delta < 0 ? I18n.choose(requireContext(), "kiri", "left") : I18n.choose(requireContext(), "kanan", "right"));
    }
    Ui.setTextChanged(binding.alignment, message);
    String details = I18n.choose(requireContext(), "Sensor: ", "Sensor: ") + state.source
        + " · " + qualityText(quality) + "\n"
        + (state.headingAt < 0 ? "—" : String.format(Locale.ROOT, I18n.choose(requireContext(), "Arah peranti %.1f° · condong %.0f°", "Device heading %.1f° · tilt %.0f°"), state.heading, state.tilt));
    if (state.locationAt >= 0) details += String.format(Locale.ROOT, I18n.choose(requireContext(), "\nLokasi %ds lalu · deklinasi %.1f°", "\nLocation %ds ago · declination %.1f°"), Math.max(0, now-state.locationAt)/1000, state.declination);
    Ui.setTextChanged(binding.sensorStatus, details);
    binding.compass.setContentDescription(message);
  }
  private String qualityText(QiblaState.Quality quality) {
    switch (quality) {
      case READY: return I18n.choose(requireContext(), "Bacaan sedia", "Reading ready");
      case LOW_CONFIDENCE: return I18n.choose(requireContext(), "Keyakinan rendah", "Low confidence");
      case STALE: return I18n.choose(requireContext(), "Bacaan lama", "Stale reading");
      case UNAVAILABLE: return I18n.choose(requireContext(), "Tidak tersedia", "Unavailable");
      case ACQUIRING_HEADING: return I18n.choose(requireContext(), "Menunggu kompas", "Waiting for compass");
      default: return I18n.choose(requireContext(), "Menunggu lokasi", "Waiting for location");
    }
  }
  @Override public void onAccuracyChanged(Sensor sensor, int accuracy) { state.accuracy = accuracy; render(); }
  @Override public void onSaveInstanceState(@NonNull Bundle out) { super.onSaveInstanceState(out); if (fix != null) out.putParcelable("qibla_location", fix); }
  @Override public void onDestroyView() { stopLocation(); handler.removeCallbacks(tick); binding = null; super.onDestroyView(); }
}
