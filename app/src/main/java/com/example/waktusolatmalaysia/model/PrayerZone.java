package com.example.waktusolatmalaysia.model;

public final class PrayerZone {
  public final String state;
  public final String name;
  public final String code;

  public PrayerZone(String state, String name, String code) {
    this.state = state;
    this.name = name;
    this.code = code;
  }

  @Override
  public String toString() {
    return name + " (" + code + ")";
  }
}
