package com.example.waktusolatmalaysia;

import static org.junit.Assert.*;
import android.content.Context;
import android.location.*;
import android.os.*;
import android.widget.TextView;
import androidx.lifecycle.Lifecycle;
import androidx.test.core.app.ActivityScenario;
import androidx.test.platform.app.InstrumentationRegistry;
import com.example.waktusolatmalaysia.utils.QiblaMath;
import com.example.waktusolatmalaysia.views.MainActivity;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.*;

/** Mock locations are strictly opt-in and emulator-only. Never run on the user's tablet. */
public class QiblaLocationUpdatesTest {
  private LocationManager locations;
  @Before @SuppressWarnings("deprecation") public void prepare() {
    org.junit.Assume.assumeTrue(InstrumentationRegistry.getArguments().getString("locationUpdates","").equals("true")
        && (Build.MODEL.contains("sdk")||Build.MODEL.contains("Emulator")));
    Context c=InstrumentationRegistry.getInstrumentation().getTargetContext();
    locations=(LocationManager)c.getSystemService(Context.LOCATION_SERVICE);
    for(String name:new String[]{LocationManager.GPS_PROVIDER,LocationManager.NETWORK_PROVIDER}) {
      locations.addTestProvider(name,false,false,false,false,true,true,true,Criteria.POWER_LOW,Criteria.ACCURACY_FINE);
      locations.setTestProviderEnabled(name,name.equals(LocationManager.GPS_PROVIDER));
    }
  }
  @After public void removeMocks() {
    if(locations!=null)for(String name:new String[]{LocationManager.GPS_PROVIDER,LocationManager.NETWORK_PROVIDER})
      try {locations.removeTestProvider(name);}catch(IllegalArgumentException ignored){}
  }
  private void publish(double lat,double lon,long ageMillis) {
    Location l=new Location(LocationManager.GPS_PROVIDER);l.setLatitude(lat);l.setLongitude(lon);l.setAccuracy(10);
    l.setTime(System.currentTimeMillis()-ageMillis);l.setElapsedRealtimeNanos(SystemClock.elapsedRealtimeNanos()-ageMillis*1000000);
    locations.setTestProviderLocation(LocationManager.GPS_PROVIDER,l);
  }
  private void awaitBearing(ActivityScenario<MainActivity> s,double lat,double lon) {
    String expected=String.format(Locale.ROOT,"%.1f°",QiblaMath.bearing(lat,lon));
    long deadline=SystemClock.elapsedRealtime()+7000;AtomicBoolean matches=new AtomicBoolean();
    do {s.onActivity(a->{TextView v=a.findViewById(R.id.bearing);matches.set(v!=null&&v.getText().toString().equals(expected));});
      if(matches.get())return;SystemClock.sleep(100);
    }while(SystemClock.elapsedRealtime()<deadline);
    fail("Live location did not update bearing to "+expected);
  }
  @Test public void cachedFixDoesNotCancelSubsequentLiveUpdates() {
    publish(3.139,101.6869,90000);
    try(var scenario=ActivityScenario.launch(MainActivity.class)) {
      V3FeaturesTest.open(scenario,R.id.qiblaFragment);awaitBearing(scenario,3.139,101.6869);
      SystemClock.sleep(1200);publish(-33.8688,151.2093,0);awaitBearing(scenario,-33.8688,151.2093);
      SystemClock.sleep(1200);publish(51.5074,-.1278,0);awaitBearing(scenario,51.5074,-.1278);
    }
  }
  @Test public void resumeResubscribesEvenWhenPreviousFixIsStillFresh() {
    publish(3.139,101.6869,10000);
    try(var scenario=ActivityScenario.launch(MainActivity.class)) {
      V3FeaturesTest.open(scenario,R.id.qiblaFragment);awaitBearing(scenario,3.139,101.6869);
      scenario.moveToState(Lifecycle.State.CREATED);publish(-33.8688,151.2093,0);
      scenario.moveToState(Lifecycle.State.RESUMED);awaitBearing(scenario,-33.8688,151.2093);
      SystemClock.sleep(1200);publish(51.5074,-.1278,0);awaitBearing(scenario,51.5074,-.1278);
    }
  }
  @Test public void lateFixAfterInitialTimeoutArrivesWithoutAnotherTap() {
    publish(3.139,101.6869,180000);
    try(var scenario=ActivityScenario.launch(MainActivity.class)) {
      V3FeaturesTest.open(scenario,R.id.qiblaFragment);
      SystemClock.sleep(46000);
      scenario.onActivity(a->{TextView v=a.findViewById(R.id.coordinates);
        assertTrue(v.getText().toString().contains("Kemas kini diteruskan")||v.getText().toString().contains("Updates continue"));});
      publish(-33.8688,151.2093,0);awaitBearing(scenario,-33.8688,151.2093);
    }
  }
}
