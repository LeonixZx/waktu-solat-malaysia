package com.example.waktusolatmalaysia;

import android.appwidget.*;
import android.content.Context;
import android.os.Bundle;
import androidx.test.platform.app.InstrumentationRegistry;
import java.io.File;
import java.nio.charset.StandardCharsets;
import org.json.*;
import org.junit.Test;

/** Installed-widget evidence and an optional capture without changing widget configuration. */
public class WidgetAllocationDiagnosticTest {
  @Test
  public void recordInstalledWidgetAllocations() throws Exception {
    var instrument = InstrumentationRegistry.getInstrumentation();
    Context c = instrument.getTargetContext();
    AppWidgetManager manager = AppWidgetManager.getInstance(c);
    JSONArray entries = new JSONArray();
    for (AppWidgetProviderInfo provider : manager.getInstalledProviders()) {
      if (!provider.provider.getPackageName().equals(c.getPackageName())) continue;
      for (int id : manager.getAppWidgetIds(provider.provider)) {
        Bundle opts = manager.getAppWidgetOptions(id);
        JSONObject options = new JSONObject();
        for (String key : opts.keySet()) options.put(key, String.valueOf(opts.get(key)));
        JSONObject stored =
            new JSONObject(c.getSharedPreferences("widget_config", 0).getString("" + id, "{}"));
        entries.put(
            new JSONObject()
                .put("id", id)
                .put("component", provider.provider.getClassName())
                .put("options", options)
                .put("layoutStyle", stored.optString("layoutStyle", "classic"))
                .put("theme", stored.optString("theme", "system")));
      }
    }
    JSONObject report =
        new JSONObject()
            .put("version", c.getPackageManager().getPackageInfo(c.getPackageName(), 0).versionName)
            .put("fontScale", c.getResources().getConfiguration().fontScale)
            .put("density", c.getResources().getDisplayMetrics().density)
            .put("widgets", entries);
    File target = new File(c.getExternalFilesDir(null), "widget-allocation.json");
    try (java.io.FileOutputStream out = new java.io.FileOutputStream(target)) {
      out.write(report.toString(2).getBytes(StandardCharsets.UTF_8));
    }
    Bundle data = new Bundle();
    data.putString("widgetAllocation", report.toString());
    instrument.sendStatus(0, data);
    if (InstrumentationRegistry.getArguments().getString("capturePreview", "false").equals("true")) {
      int id=Integer.parseInt(InstrumentationRegistry.getArguments().getString("widgetId"));
      capturePreview(id);
    }
    if(InstrumentationRegistry.getArguments().getString("pinDaily","false").equals("true")) {
      try(var scenario=androidx.test.core.app.ActivityScenario.launch(com.example.waktusolatmalaysia.views.MainActivity.class)){
        scenario.onActivity(a->{
          boolean requested=AppWidgetManager.getInstance(a).requestPinAppWidget(
              new android.content.ComponentName(a,com.example.waktusolatmalaysia.widgets.DayPrayerWidget.class),null,null);
          Bundle status=new Bundle();status.putString("pinRequested",String.valueOf(requested));instrument.sendStatus(0,status);
        });
        long end=android.os.SystemClock.uptimeMillis()+60000;
        while(android.os.SystemClock.uptimeMillis()<end){
          if(manager.getAppWidgetIds(new android.content.ComponentName(c,com.example.waktusolatmalaysia.widgets.DayPrayerWidget.class)).length>0)break;
          android.os.SystemClock.sleep(250);
        }
      }
    }
  }

  private <T extends android.view.View>T find(android.view.View root,Class<T>type) {
    if(type.isInstance(root))return type.cast(root);
    if(root instanceof android.view.ViewGroup)for(int i=0;i<((android.view.ViewGroup)root).getChildCount();i++){
      T result=find(((android.view.ViewGroup)root).getChildAt(i),type);if(result!=null)return result;
    }return null;
  }

  private void capturePreview(int id)throws Exception {
    var inst=InstrumentationRegistry.getInstrumentation();Context c=inst.getTargetContext();
    AppWidgetProviderInfo info=AppWidgetManager.getInstance(c).getAppWidgetInfo(id);
    org.junit.Assert.assertNotNull(info);org.junit.Assert.assertEquals(c.getPackageName(),info.provider.getPackageName());
    try(var scenario=androidx.test.core.app.ActivityScenario.launch(com.example.waktusolatmalaysia.views.MainActivity.class)){
      scenario.onActivity(a->a.openDestination(a.getResources().getIdentifier("widgetsFragment","id",a.getPackageName()),null));
      inst.waitForIdleSync();android.os.SystemClock.sleep(200);
      scenario.onActivity(a->{var tabs=find(a.getWindow().getDecorView(),com.google.android.material.tabs.TabLayout.class);tabs.selectTab(tabs.getTabAt(1));});
      inst.waitForIdleSync();android.os.SystemClock.sleep(200);
      scenario.onActivity(a->{
        var preview=(com.example.waktusolatmalaysia.widgets.WidgetPreviewView)a.getWindow().getDecorView().findViewWithTag("widget-preview-"+id);
        org.junit.Assert.assertNotNull(preview);
        preview.requestRectangleOnScreen(new android.graphics.Rect(0,0,preview.getWidth(),preview.getHeight()),true);
        android.view.View root=preview.getChildAt(0);
        android.graphics.Bitmap b=android.graphics.Bitmap.createBitmap(root.getWidth(),root.getHeight(),android.graphics.Bitmap.Config.ARGB_8888);
        root.draw(new android.graphics.Canvas(b));
        try(var out=new java.io.FileOutputStream(new File(c.getExternalFilesDir(null),"widget-preview-"+id+"-native.png"))){b.compress(android.graphics.Bitmap.CompressFormat.PNG,100,out);}
        catch(java.io.IOException e){throw new RuntimeException(e);}finally{b.recycle();}
      });
      inst.waitForIdleSync();android.os.SystemClock.sleep(250);
      var image=inst.getUiAutomation().takeScreenshot();org.junit.Assert.assertNotNull(image);
      try(var out=new java.io.FileOutputStream(new File(c.getExternalFilesDir(null),"widget-preview-"+id+"-app.png"))){image.compress(android.graphics.Bitmap.CompressFormat.PNG,100,out);}image.recycle();
    }
    inst.runOnMainSync(()->com.example.waktusolatmalaysia.widgets.PrayerWidgets.refresh(c));
  }
}
