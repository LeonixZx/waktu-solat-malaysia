package com.example.waktusolatmalaysia;

import static org.junit.Assert.*;
import android.content.*;
import android.graphics.Bitmap;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.view.WindowCompat;
import androidx.test.core.app.ActivityScenario;
import androidx.test.platform.app.InstrumentationRegistry;
import com.example.waktusolatmalaysia.views.MainActivity;
import java.io.*;
import org.junit.Test;

/** Checks window flags after the activity has focus, independently of resource colours. */
public class SystemBarAppearanceTest {
  @Test public void lightAndDarkSystemIconsFollowTheVisibleActivity() throws Exception {
    var inst=InstrumentationRegistry.getInstrumentation();
    Context c=inst.getTargetContext();SharedPreferences p=c.getSharedPreferences("waktu_solat",0);
    boolean hadTheme=p.contains("theme"),hadDark=p.contains("dark"),oldDark=p.getBoolean("dark",false);
    String oldTheme=p.getString("theme",oldDark?"dark":"light");int oldMode=AppCompatDelegate.getDefaultNightMode();
    try {
      for(boolean dark:new boolean[]{false,true}){
        p.edit().putString("theme",dark?"dark":"light").putBoolean("dark",dark).commit();
        inst.runOnMainSync(()->AppCompatDelegate.setDefaultNightMode(dark?2:1));
        try(ActivityScenario<MainActivity> scenario=ActivityScenario.launch(MainActivity.class)){
          inst.waitForIdleSync();android.os.SystemClock.sleep(1000);
          File folder=new File(c.getExternalFilesDir(null),"system-bar-check");folder.mkdirs();
          Bitmap image=inst.getUiAutomation().takeScreenshot();assertNotNull(image);
          try(FileOutputStream out=new FileOutputStream(new File(folder,dark?"dark.png":"light.png"))){image.compress(Bitmap.CompressFormat.PNG,100,out);}image.recycle();
          scenario.onActivity(a->{
            assertTrue("Activity must have focus; an OS dialog invalidates this check",a.hasWindowFocus());
            var controls=WindowCompat.getInsetsController(a.getWindow(),a.getWindow().getDecorView());
            assertEquals("Status icon appearance must match the visible theme",!dark,controls.isAppearanceLightStatusBars());
            if(android.os.Build.VERSION.SDK_INT>=26)assertEquals("Navigation icon appearance must match the visible theme",!dark,controls.isAppearanceLightNavigationBars());
          });
        }
      }
    } finally {
      SharedPreferences.Editor e=p.edit();if(hadTheme)e.putString("theme",oldTheme);else e.remove("theme");if(hadDark)e.putBoolean("dark",oldDark);else e.remove("dark");e.commit();
      inst.runOnMainSync(()->AppCompatDelegate.setDefaultNightMode(oldMode));
    }
  }
}
