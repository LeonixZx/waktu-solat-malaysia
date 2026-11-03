package com.example.waktusolatmalaysia;

import static org.junit.Assert.*;
import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.matcher.ViewMatchers.withContentDescription;
import android.view.*;
import android.widget.*;
import androidx.navigation.fragment.NavHostFragment;
import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import com.example.waktusolatmalaysia.ui.I18n;
import com.example.waktusolatmalaysia.utils.PrayerClock;
import com.example.waktusolatmalaysia.views.MainActivity;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.*;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class CalendarInteractionTest {
  private static boolean text(View v,String value) {
    if(v instanceof TextView&&((TextView)v).getText().toString().equals(value))return true;
    if(v instanceof ViewGroup)for(int i=0;i<((ViewGroup)v).getChildCount();i++)if(text(((ViewGroup)v).getChildAt(i),value))return true;
    return false;
  }
  @Test public void monthRolloverRecreationAndSelectedDateOpenTheCorrectSchedule() throws Exception {
    var c=InstrumentationRegistry.getInstrumentation().getTargetContext();
    Map<String,?> before=new HashMap<>(c.getSharedPreferences("waktu_solat",0).getAll());
    I18n.language(c,"ms");
    YearMonth now=YearMonth.now(PrayerClock.MALAYSIA),next=now.plusMonths(2);
    String nextLabel=next.atDay(1).format(DateTimeFormatter.ofPattern("MMMM yyyy",Locale.forLanguageTag("ms-MY")));
    try(var s=ActivityScenario.launch(MainActivity.class)) {
      V3FeaturesTest.open(s,R.id.calendarFragment);
      onView(withContentDescription("Bulan berikutnya")).perform(TabletFeatureTest.reveal(),click(),click());
      V3FeaturesTest.waitFor(s,a->text(a.findViewById(R.id.nav_host),nextLabel));
      s.recreate();V3FeaturesTest.waitFor(s,a->text(a.findViewById(R.id.nav_host),nextLabel));
      onView(withContentDescription("Bulan sebelumnya")).perform(TabletFeatureTest.reveal(),click(),click());
      V3FeaturesTest.waitFor(s,a->a.findViewById(R.id.calendar_grid)!=null);
      s.onActivity(a->{
        GridLayout grid=a.findViewById(R.id.calendar_grid);boolean clicked=false;
        for(int i=0;i<grid.getChildCount();i++)if(text(grid.getChildAt(i),"15")){grid.getChildAt(i).performClick();clicked=true;break;}
        assertTrue("Day 15 is selectable",clicked);
      });
      V3FeaturesTest.waitFor(s,a->{
        NavHostFragment host=(NavHostFragment)a.getSupportFragmentManager().findFragmentById(R.id.nav_host);
        return host.getNavController().getCurrentDestination().getId()==R.id.scheduleFragment;
      });
      s.onActivity(a->{
        NavHostFragment host=(NavHostFragment)a.getSupportFragmentManager().findFragmentById(R.id.nav_host);
        var fragment=host.getChildFragmentManager().getPrimaryNavigationFragment();
        assertEquals(now.atDay(15).toString(),fragment.getArguments().getString("date"));
      });
    } finally {V3FeaturesTest.restorePrefs(c.getSharedPreferences("waktu_solat",0),before);}
  }
}
