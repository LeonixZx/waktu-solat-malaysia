package com.example.waktusolatmalaysia;

import android.app.Activity;
import android.view.WindowManager;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.runner.lifecycle.*;
import java.util.*;
import org.junit.runner.Description;
import org.junit.runner.Result;
import org.junit.runner.notification.RunListener;

/** Test-only window flags; never changes global timeout/security settings. */
public final class KeepAwakeListener extends RunListener {
  private final Map<Activity,Integer> original = new WeakHashMap<>();
  private final ActivityLifecycleCallback callback = (activity, stage) -> {
    if(stage==Stage.CREATED||stage==Stage.RESUMED){
      original.putIfAbsent(activity,activity.getWindow().getAttributes().flags);
      activity.getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }
  };

  @Override public void testRunStarted(Description description){
    InstrumentationRegistry.getInstrumentation().runOnMainSync(() ->
        ActivityLifecycleMonitorRegistry.getInstance().addLifecycleCallback(callback));
  }

  @Override public void testRunFinished(Result result){
    InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
      ActivityLifecycleMonitorRegistry.getInstance().removeLifecycleCallback(callback);
      for(Map.Entry<Activity,Integer> entry:new ArrayList<>(original.entrySet()))
        if(!entry.getKey().isDestroyed()&&(entry.getValue()&WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)==0)
          entry.getKey().getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
      original.clear();
    });
  }
}
