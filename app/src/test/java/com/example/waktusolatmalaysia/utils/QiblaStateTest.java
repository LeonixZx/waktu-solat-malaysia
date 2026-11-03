package com.example.waktusolatmalaysia.utils;
import static org.junit.Assert.*;
import org.junit.Test;

public class QiblaStateTest {
  private QiblaState ready() {
    QiblaState s=new QiblaState();s.available=true;s.location(3.139,101.6869,2,10000,20,"test");s.sample(s.bearing-2,0,2,3,10000);return s;
  }
  @Test public void alignedRequiresEveryQualityGate() {
    QiblaState s=ready();assertTrue(s.aligned(10000));
    s.accuracy=1;assertFalse(s.aligned(10000));s.accuracy=2;
    s.tilt=26;assertFalse(s.aligned(10000));s.tilt=0;
    s.uncertainty=11;assertFalse(s.aligned(10000));s.uncertainty=10;
    assertTrue(s.aligned(10000));assertEquals(QiblaState.Quality.STALE,s.quality(12001));
    s.headingAt=140000;assertEquals(QiblaState.Quality.STALE,s.quality(140000));
    s.available=false;assertEquals(QiblaState.Quality.UNAVAILABLE,s.quality(10000));
  }
  @Test public void missingAndRestoredReadingsNeverPretendToBeAligned() {
    QiblaState s=new QiblaState();assertEquals(QiblaState.Quality.ACQUIRING_LOCATION,s.quality(0));
    s.available=true;s.location(3,101,0,0,10,"test");assertEquals(QiblaState.Quality.ACQUIRING_HEADING,s.quality(0));
    s=ready();s.resetHeading();assertFalse(s.aligned(10000));
  }
  @Test public void declinationIsAppliedExactlyOnceWithoutResettingHeading() {
    QiblaState s=ready();s.sample(100,0,2,3,13000);assertEquals(102,s.heading,.001);
    s.location(3,101,4,13000,10,"test");assertEquals(104,s.heading,.001);
    s.location(3,101,4,13000,10,"test");assertEquals(104,s.heading,.001);
  }
  @Test public void circularSmoothingUsesTheShortArc() {
    QiblaState s=new QiblaState();s.sample(359,0,0,3,1000);s.sample(1,0,0,3,1150);
    assertTrue(Math.abs(QiblaState.delta(s.heading,0))<1);assertEquals(2,QiblaState.delta(1,359),.001);
  }
  @Test public void invalidCoordinatesAndKaabaHaveNoInventedBearing() {
    for(double n:new double[]{Double.NaN,Double.POSITIVE_INFINITY,91,-91})assertTrue(Double.isNaN(QiblaMath.bearing(n,10)));
    assertTrue(Double.isNaN(QiblaMath.bearing(10,181)));assertTrue(Double.isNaN(QiblaMath.bearing(21.422487,39.826206)));
  }
  @Test public void previousBootAndFutureLocationsAreNeverRejuvenated() {
    assertEquals(10,QiblaState.locationAge(20000000,10000000,200000,190000));
    assertEquals(130000,QiblaState.locationAge(1000,9000000000L,200000,70000));
    assertEquals(Long.MAX_VALUE,QiblaState.locationAge(1000,0,200000,210000));
  }
}
