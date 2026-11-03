package com.example.waktusolatmalaysia;

import static org.junit.Assert.*;
import android.content.Context;
import android.hardware.*;
import android.os.*;
import android.view.WindowManager;
import androidx.test.core.app.ActivityScenario;
import androidx.test.platform.app.InstrumentationRegistry;
import com.example.waktusolatmalaysia.utils.QiblaOrientation;
import com.example.waktusolatmalaysia.utils.QiblaState;
import com.example.waktusolatmalaysia.views.MainActivity;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import org.json.*;
import org.junit.Test;

/** Opt-in diagnostic: compares sensor paths, never declares physical accuracy. No coordinates. */
public class QiblaSensorDiagnosticTest {
  @Test public void compareAbsoluteSensorPathsWithoutChangingPersonalData() throws Exception {
    org.junit.Assume.assumeTrue("Explicit diagnostic only",InstrumentationRegistry.getArguments().getString("physicalDiagnostic","").equals("true"));
    Context c=InstrumentationRegistry.getInstrumentation().getTargetContext();
    SensorManager manager=(SensorManager)c.getSystemService(Context.SENSOR_SERVICE);
    int display=((WindowManager)c.getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay().getRotation();
    HandlerThread thread=new HandlerThread("compass-diagnostic");thread.start();
    Map<Integer,List<double[]>> samples=new HashMap<>();Map<Integer,String> names=new HashMap<>();List<Double> strength=new ArrayList<>();
    float[][] vectors={null,null};int[] accuracy={0,0};
    SensorEventListener listener=new SensorEventListener(){
      private void capture(float[] matrix,int type,int quality,double uncertainty){
        float[] out=new float[9],angle=new float[3];if(!QiblaOrientation.remap(matrix,display,out))return;
        SensorManager.getOrientation(out,angle);
        samples.computeIfAbsent(type,k->new ArrayList<>()).add(new double[]{QiblaState.normalize(Math.toDegrees(angle[0])),Math.toDegrees(Math.acos(Math.max(-1,Math.min(1,out[8])))),quality,uncertainty});
      }
      public void onSensorChanged(SensorEvent e){
        int type=e.sensor.getType();float[] matrix=new float[9];
        if(type==Sensor.TYPE_ROTATION_VECTOR||type==Sensor.TYPE_GEOMAGNETIC_ROTATION_VECTOR){
          SensorManager.getRotationMatrixFromVector(matrix,e.values);
          capture(matrix,type,e.accuracy,e.values.length>=5&&e.values[4]>=0?Math.toDegrees(e.values[4]):Double.NaN);
        } else {
          if(type==Sensor.TYPE_ACCELEROMETER){vectors[0]=e.values.clone();accuracy[0]=e.accuracy;}
          if(type==Sensor.TYPE_MAGNETIC_FIELD){vectors[1]=e.values.clone();accuracy[1]=e.accuracy;strength.add(Math.sqrt(e.values[0]*e.values[0]+e.values[1]*e.values[1]+e.values[2]*e.values[2]));}
          if(vectors[0]!=null&&vectors[1]!=null&&SensorManager.getRotationMatrix(matrix,null,vectors[0],vectors[1]))capture(matrix,-1,Math.min(accuracy[0],accuracy[1]),Double.NaN);
        }
      }
      public void onAccuracyChanged(Sensor sensor,int quality){}
    };
    try(var scenario=ActivityScenario.launch(MainActivity.class)) {
      V3FeaturesTest.open(scenario,R.id.qiblaFragment);
      for(int type:new int[]{Sensor.TYPE_ROTATION_VECTOR,Sensor.TYPE_GEOMAGNETIC_ROTATION_VECTOR,Sensor.TYPE_ACCELEROMETER,Sensor.TYPE_MAGNETIC_FIELD}){
        Sensor sensor=manager.getDefaultSensor(type);
        if(sensor!=null){names.put(type,sensor.getName());manager.registerListener(listener,sensor,SensorManager.SENSOR_DELAY_UI,new Handler(thread.getLooper()));}
      }
      SystemClock.sleep(6000);manager.unregisterListener(listener);thread.quitSafely();thread.join(3000);
      JSONObject result=new JSONObject().put("displayRotation",display).put("physicalAccuracyVerified",false);
      JSONArray paths=new JSONArray();
      for(var entry:samples.entrySet()){
        List<double[]> values=entry.getValue();double sx=0,sy=0,maxTilt=0,maxError=0;int minQuality=3;
        for(double[] v:values){sx+=Math.cos(Math.toRadians(v[0]));sy+=Math.sin(Math.toRadians(v[0]));maxTilt=Math.max(maxTilt,v[1]);minQuality=Math.min(minQuality,(int)v[2]);}
        double mean=QiblaState.normalize(Math.toDegrees(Math.atan2(sy,sx))),spread=0;boolean hasUncertainty=false;
        for(double[] v:values){spread=Math.max(spread,Math.abs(QiblaState.delta(v[0],mean)));if(Double.isFinite(v[3])){hasUncertainty=true;maxError=Math.max(maxError,v[3]);}}
        paths.put(new JSONObject().put("type",entry.getKey()).put("name",names.getOrDefault(entry.getKey(),"accelerometer + magnetometer"))
          .put("samples",values.size()).put("magneticHeadingMean",mean).put("headingSpread",spread).put("maximumTilt",maxTilt).put("minimumAccuracy",minQuality).put("reportedUncertaintyMaximum",hasUncertainty?maxError:JSONObject.NULL));
      }
      result.put("paths",paths);
      if(!strength.isEmpty())result.put("magneticFieldMicroTesla",new JSONObject().put("minimum",Collections.min(strength)).put("maximum",Collections.max(strength)));
      assertTrue("Absolute sensor samples arrived",samples.containsKey(Sensor.TYPE_ROTATION_VECTOR)||samples.containsKey(-1));
      File file=new File(c.getExternalFilesDir(null),"physical-qibla-sensors.json");
      try(var output=new FileOutputStream(file)){output.write(result.toString(2).getBytes(StandardCharsets.UTF_8));}
    } finally {manager.unregisterListener(listener);thread.quitSafely();}
  }
}
