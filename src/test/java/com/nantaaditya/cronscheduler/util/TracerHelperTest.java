package com.nantaaditya.cronscheduler.util;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.micrometer.tracing.Baggage;
import io.micrometer.tracing.BaggageInScope;
import io.micrometer.tracing.BaggageManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.MDC;

@ExtendWith(MockitoExtension.class)
class TracerHelperTest {

  @InjectMocks
  private TracerHelper tracerHelper;

  @Mock
  private BaggageManager baggageManager;

  @AfterEach
  void clearMdc() {
    MDC.clear();
  }

  @Test
  void setBaggageUsesExistingBaggageWhenPresent() {
    Baggage baggage = mock(Baggage.class);
    BaggageInScope scope = mock(BaggageInScope.class);
    when(baggageManager.getBaggage("key")).thenReturn(baggage);
    when(baggage.makeCurrent("value")).thenReturn(scope);

    tracerHelper.setBaggage("key", "value");

    verify(baggageManager, times(0)).createBaggage("key");
    assertEquals("value", MDC.get("key"));
  }

  @Test
  void setBaggageCreatesBaggageWhenNotPresent() {
    Baggage baggage = mock(Baggage.class);
    BaggageInScope scope = mock(BaggageInScope.class);
    when(baggageManager.getBaggage("key")).thenReturn(null);
    when(baggageManager.createBaggage("key")).thenReturn(baggage);
    when(baggage.makeCurrent("value")).thenReturn(scope);

    tracerHelper.setBaggage("key", "value");

    verify(baggageManager).createBaggage("key");
    assertEquals("value", MDC.get("key"));
  }

  @Test
  void setBaggageClosesPreviousScopeOnSecondCall() {
    Baggage baggage = mock(Baggage.class);
    BaggageInScope firstScope = mock(BaggageInScope.class);
    BaggageInScope secondScope = mock(BaggageInScope.class);
    when(baggageManager.getBaggage("key")).thenReturn(baggage);
    when(baggage.makeCurrent("value-1")).thenReturn(firstScope);
    when(baggage.makeCurrent("value-2")).thenReturn(secondScope);

    tracerHelper.setBaggage("key", "value-1");
    tracerHelper.setBaggage("key", "value-2");

    verify(firstScope).close();
    verify(secondScope, times(0)).close();
    assertEquals("value-2", MDC.get("key"));
  }

  @Test
  void setBaggageSwallowsExceptionsFromBaggageManager() {
    when(baggageManager.getBaggage("key")).thenThrow(new RuntimeException("boom"));

    assertDoesNotThrow(() -> tracerHelper.setBaggage("key", "value"));
  }
}
