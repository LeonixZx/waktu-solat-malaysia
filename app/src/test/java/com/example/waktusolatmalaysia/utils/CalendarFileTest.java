package com.example.waktusolatmalaysia.utils;
import static org.junit.Assert.*;
import com.example.waktusolatmalaysia.model.PrayerDay;
import java.time.*;
import java.util.*;
import org.junit.Test;
public class CalendarFileTest {
  @Test public void exportUsesFiveRealTimesAndMalaysiaTimezoneAcrossMidnight() {
    List<LocalTime> times=new ArrayList<>();for(String t:new String[]{"05:38","05:48","06:58","07:26","13:00","16:20","18:58","20:09"})times.add(LocalTime.parse(t));
    String ics=CalendarFile.prayers(List.of(new PrayerDay(LocalDate.of(2026,11,2),"1448-05-22",times)),"WLY01",false);
    assertEquals(5,ics.split("BEGIN:VEVENT",-1).length-1);assertTrue(ics.contains("DTSTART:20261101T214800Z"));
    assertTrue(ics.contains("SUMMARY:Subuh\r\n"));assertTrue(ics.endsWith("END:VCALENDAR\r\n"));
    assertFalse(CalendarFile.prayers(List.of(),"WLY01",false).contains("BEGIN:VEVENT"));
  }
}
