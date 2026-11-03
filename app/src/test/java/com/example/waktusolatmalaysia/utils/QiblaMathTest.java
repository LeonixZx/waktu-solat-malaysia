package com.example.waktusolatmalaysia.utils;

import static org.junit.Assert.*;

import org.junit.Test;

public class QiblaMathTest {
  @Test
  public void kualaLumpurPointsWestNorthwest() {
    assertEquals(292.5, QiblaMath.bearing(3.139, 101.6869), 0.2);
  }

  @Test
  public void dueSouthOfKaabaPointsNorth() {
    assertEquals(0, QiblaMath.bearing(0, 39.826206), 0.001);
  }

  @Test
  public void dueNorthOfKaabaPointsSouth() {
    assertEquals(180, QiblaMath.bearing(40, 39.826206), 0.001);
  }
}
