package com.example.waktusolatmalaysia;
import static org.junit.Assert.*;
import android.content.*;
import android.content.res.Configuration;
import android.graphics.*;
import android.graphics.drawable.Drawable;
import androidx.core.content.ContextCompat;
import androidx.test.platform.app.InstrumentationRegistry;
import java.io.*;
import org.junit.Test;

public class LauncherBrandTest {
  @Test public void normalLauncherKeepsEspressoBackgroundInLightAndDarkConfigurations() throws Exception {
    Context c=InstrumentationRegistry.getInstrumentation().getTargetContext();
    assertEquals(BuildConfig.VERSION_NAME,c.getPackageManager().getPackageInfo(c.getPackageName(),0).versionName);
    File out=new File(c.getExternalFilesDir(null),"tablet-verification");out.mkdirs();
    for(int mode:new int[]{Configuration.UI_MODE_NIGHT_NO,Configuration.UI_MODE_NIGHT_YES}) {
      Configuration config=new Configuration(c.getResources().getConfiguration());config.uiMode=(config.uiMode&~Configuration.UI_MODE_NIGHT_MASK)|mode;
      Context themed=c.createConfigurationContext(config);
      assertEquals(Color.parseColor("#2B2119"),ContextCompat.getColor(themed,R.color.launcher_background));
      Drawable icon=ContextCompat.getDrawable(themed,c.getApplicationInfo().icon);assertNotNull(icon);
      Bitmap image=Bitmap.createBitmap(512,512,Bitmap.Config.ARGB_8888);icon.setBounds(0,0,512,512);icon.draw(new Canvas(image));
      int minX=512,minY=512,maxX=0,maxY=0,count=0;
      for(int y=0;y<512;y++)for(int x=0;x<512;x++) {
        int pixel=image.getPixel(x,y);
        if(Color.alpha(pixel)>200 && Color.red(pixel)>150 && Color.green(pixel)>95 && Color.blue(pixel)<180 && Color.red(pixel)>Color.green(pixel)*1.05) {
          minX=Math.min(minX,x);minY=Math.min(minY,y);maxX=Math.max(maxX,x);maxY=Math.max(maxY,y);count++;
        }
      }
      assertTrue("Gold foreground remains legible",count>3000);
      assertTrue("Launcher mask must not cut the gold mark",minX>=16 && minY>=16 && maxX<496 && maxY<496);
      try(FileOutputStream stream=new FileOutputStream(new File(out,"launcher-"+(mode==Configuration.UI_MODE_NIGHT_NO?"light":"dark")+".png"))){image.compress(Bitmap.CompressFormat.PNG,100,stream);}image.recycle();
    }
  }
}
