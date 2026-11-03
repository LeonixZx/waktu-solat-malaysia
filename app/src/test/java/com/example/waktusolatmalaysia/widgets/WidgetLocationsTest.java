package com.example.waktusolatmalaysia.widgets;

import static org.junit.Assert.*;
import com.example.waktusolatmalaysia.model.PrayerZone;
import org.junit.Test;

public class WidgetLocationsTest {
  @Test public void ordinaryLocationNamesStayReadableWithoutCodes() {
    PrayerZone zone=new PrayerZone("Wilayah Persekutuan","Kuala Lumpur, Putrajaya","WLY01");
    assertEquals("Kuala Lumpur",WidgetLocations.name(zone,false));
    assertEquals("Kuala Lumpur",WidgetLocations.name(zone,true));
    assertEquals("K. Terengganu",WidgetLocations.name(new PrayerZone("Terengganu","Kuala Terengganu, Marang","TRG01"),true));
  }
  @Test public void compactAreasAndMissingNamesDoNotFallBackToAZoneCode() {
    assertEquals("Pulau Aur",WidgetLocations.name(new PrayerZone("Johor","Pulau Aur dan Pulau Pemanggil","JHR01"),true));
    assertEquals("Johor",WidgetLocations.name(new PrayerZone("Johor","","JHR01"),true));
    assertEquals("",WidgetLocations.name(null,true));
  }
}
