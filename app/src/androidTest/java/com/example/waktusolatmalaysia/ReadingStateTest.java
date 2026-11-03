package com.example.waktusolatmalaysia;

import static org.junit.Assert.*;
import android.os.Bundle;
import android.view.View;
import androidx.core.widget.NestedScrollView;
import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import com.example.waktusolatmalaysia.preferences.UserStore;
import com.example.waktusolatmalaysia.repo.ContentRepository;
import com.example.waktusolatmalaysia.views.MainActivity;
import java.util.*;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class ReadingStateTest {
  private static NestedScrollView reader(MainActivity a) {
    View v=a.findViewById(R.id.reading_bookmark);
    while(v!=null && !(v instanceof NestedScrollView))v=v.getParent() instanceof View?(View)v.getParent():null;
    return (NestedScrollView)v;
  }
  @Test public void longReadingRetainsScrollAfterRecreation() throws Exception {
    var c=InstrumentationRegistry.getInstrumentation().getTargetContext();
    Map<String,?> before=new HashMap<>(c.getSharedPreferences("personal_library",0).getAll());
    new UserStore(c).set("arabic_size",56);
    try(var s=ActivityScenario.launch(MainActivity.class)) {
      var rows=new ContentRepository(c).readings("doa");
      var longest=rows.stream().max(Comparator.comparingInt(r->r.arabic.length())).orElseThrow();
      s.onActivity(a->{Bundle b=new Bundle();b.putString("kind","doa");b.putString("reading_id",longest.id);a.openDestination(R.id.readingFragment,b);});
      V3FeaturesTest.waitFor(s,a->reader(a)!=null&&reader(a).getChildAt(0).getHeight()>reader(a).getHeight()+200);
      int[] position={0};
      s.onActivity(a->{var v=reader(a);v.scrollTo(0,Math.min(600,(v.getChildAt(0).getHeight()-v.getHeight())/2));position[0]=v.getScrollY();assertTrue(position[0]>50);});
      s.recreate();
      V3FeaturesTest.waitFor(s,a->reader(a)!=null&&Math.abs(reader(a).getScrollY()-position[0])<3);
    } finally {V3FeaturesTest.restorePrefs(c.getSharedPreferences("personal_library",0),before);}
  }
}
