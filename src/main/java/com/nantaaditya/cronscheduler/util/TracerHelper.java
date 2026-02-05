package com.nantaaditya.cronscheduler.util;

import io.micrometer.tracing.Baggage;
import io.micrometer.tracing.BaggageManager;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class TracerHelper {

  private final BaggageManager baggageManager;

  public void setBaggage(String key, String value) {
    try {
      Baggage baggage = Optional.ofNullable(baggageManager.getBaggage(key))
          .orElseGet(() -> baggageManager.createBaggage(key));
      baggage.makeCurrent(value);
      MDC.put(key, value);
    } catch (Exception e) {
      log.error("#Baggage - failed to set baggage {} with value {}, error {}", key, value, e);
    }
  }

}
