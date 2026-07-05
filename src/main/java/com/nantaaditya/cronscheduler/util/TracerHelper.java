package com.nantaaditya.cronscheduler.util;

import io.micrometer.tracing.Baggage;
import io.micrometer.tracing.BaggageInScope;
import io.micrometer.tracing.BaggageManager;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;

@Log4j2
@Component
@RequiredArgsConstructor
public class TracerHelper {

  private final BaggageManager baggageManager;

  private final Map<String, BaggageInScope> currentScopes = new ConcurrentHashMap<>();

  public void setBaggage(String key, String value) {
    try {
      Baggage baggage = Optional.ofNullable(baggageManager.getBaggage(key))
          .orElseGet(() -> baggageManager.createBaggage(key));

      BaggageInScope previousScope = currentScopes.put(key, baggage.makeCurrent(value));
      if (previousScope != null) {
        previousScope.close();
      }

      MDC.put(key, value);
    } catch (Exception e) {
      log.error("#Baggage - failed to set baggage {} with value {}, error {}", key, value, e);
    }
  }

}
