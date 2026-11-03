package com.example.waktusolatmalaysia;
import static org.junit.Assert.*;
import static org.junit.Assume.assumeTrue;
import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.matcher.ViewMatchers.*;
import static androidx.test.espresso.action.ViewActions.*;
import android.content.*;
import android.content.pm.PackageManager;
import android.os.Build;
import androidx.core.content.ContextCompat;
import androidx.test.core.app.ActivityScenario;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.uiautomator.*;
import com.example.waktusolatmalaysia.preferences.*;
import com.example.waktusolatmalaysia.repo.TaskStore;
import com.example.waktusolatmalaysia.views.MainActivity;
import java.util.*;
import java.util.regex.Pattern;
import org.junit.Test;

public class ReminderPermissionTest {
  @Test public void denyingNotificationsSavesTheReminderDisabled() throws Exception {
    assumeTrue("Emulator-only permission test",Build.MODEL.contains("sdk_gphone"));
    Context c=InstrumentationRegistry.getInstrumentation().getTargetContext();
    assertEquals(PackageManager.PERMISSION_DENIED,ContextCompat.checkSelfPermission(c,android.Manifest.permission.POST_NOTIFICATIONS));
    TaskStore store=new TaskStore(c);var tasks=store.load();
    var app=new HashMap<>(c.getSharedPreferences("waktu_solat",0).getAll());
    var personal=new HashMap<>(c.getSharedPreferences("personal_library",0).getAll());
    try(var s=ActivityScenario.launch(MainActivity.class)) {
      V3FeaturesTest.open(s,R.id.tasksFragment);
      onView(withId(R.id.task_add)).perform(TabletFeatureTest.reveal(),click());
      onView(withId(R.id.v31_task_title)).perform(replaceText("V31 permission-denied check"),closeSoftKeyboard());
      onView(withText("Simpan")).perform(click());
      UiDevice device=UiDevice.getInstance(InstrumentationRegistry.getInstrumentation());
      UiObject2 deny=device.wait(Until.findObject(By.res(Pattern.compile(".*:id/permission_deny_button"))),10000);
      assertNotNull("A real Android permission prompt must appear",deny);deny.click();
      device.wait(Until.gone(By.res(Pattern.compile(".*:id/permission_deny_button"))),5000);
      long end=android.os.SystemClock.elapsedRealtime()+5000;
      while(store.load().stream().noneMatch(t->t.title.equals("V31 permission-denied check"))&&android.os.SystemClock.elapsedRealtime()<end)android.os.SystemClock.sleep(100);
      var task=store.load().stream().filter(t->t.title.equals("V31 permission-denied check")).findFirst().orElseThrow();
      assertFalse(task.enabled);TabletFeatureTest.capture("v31-permission-denied");
    } finally {store.replace(tasks);V3FeaturesTest.restorePrefs(c.getSharedPreferences("waktu_solat",0),app);V3FeaturesTest.restorePrefs(c.getSharedPreferences("personal_library",0),personal);}
  }
}
