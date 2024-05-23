package com.nantaaditya.cronscheduler.util;

import com.github.f4b6a3.tsid.TsidCreator;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class IdGenerator {

  public static String createId() {
    return TsidCreator.getTsid256().toLowerCase();
  }

  public static long createLongId() {
    return TsidCreator.getTsid256().toLong();
  }
}
