package com.example.waktusolatmalaysia.widgets;

import static org.junit.Assert.*;
import org.junit.Test;

public class WidgetDatesTest {
  @Test public void formatsTheSuppliedDateWithoutConversion() {
    assertEquals("23 Jamadil Awal 1448", WidgetDates.hijri("1448-05-23", false));
    assertEquals("23 Jumada al-Ula 1448", WidgetDates.hijri("1448-05-23", true));
    assertEquals("23 Jamadil Akhir 1448", WidgetDates.hijri("1448-06-23", false));
  }
  @Test public void preservesHijriYearAndMonthBoundaries() {
    assertEquals("30 Zulhijah 1448", WidgetDates.hijri("1448-12-30", false));
    assertEquals("1 Muharam 1449", WidgetDates.hijri("1449-01-01", false));
  }
  @Test public void missingOrInvalidDatesAreNotInvented() {
    for (String value : new String[]{null,"","1448-13-01","1448-00-01","1448-01-31",
        "1448-01-00","0000-01-01","1448-1-1","2026-11-03T00:00:00"})
      assertEquals("", WidgetDates.hijri(value, false));
  }
}
