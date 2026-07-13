package com.nantaaditya.cronscheduler.util;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;
import reactor.core.scheduler.Schedulers;
import reactor.test.StepVerifier;

class ReactorEventBusTest {

  private final ReactorEventBus reactorEventBus = new ReactorEventBus();

  @Test
  void publishDoesNothingWhenSinkIsNull() {
    assertDoesNotThrow(() -> reactorEventBus.publish(null, "event"));
  }

  @Test
  void publishDoesNothingWhenEventIsNull() {
    Sinks.Many<String> sink = Sinks.many().multicast().onBackpressureBuffer();

    assertDoesNotThrow(() -> reactorEventBus.publish(sink, null));
  }

  @Test
  void publishEmitsEventOntoSink() {
    Sinks.Many<String> sink = Sinks.many().multicast().onBackpressureBuffer();
    Flux<String> flux = sink.asFlux();

    reactorEventBus.publish(sink, "event-1");
    sink.tryEmitComplete();

    StepVerifier.create(flux)
        .expectNext("event-1")
        .verifyComplete();
  }

  @Test
  void publishThrowsWhenEmitFails() {
    Sinks.Many<String> sink = Sinks.many().multicast().onBackpressureBuffer();
    sink.tryEmitComplete();

    assertThrows(Sinks.EmissionException.class, () -> reactorEventBus.publish(sink, "event-1"));
  }

  @Test
  void consumeReturnsEmptyFluxWhenSinkIsNull() {
    StepVerifier.create(reactorEventBus.consume(null, Schedulers.immediate()))
        .verifyComplete();
  }

  @Test
  void consumeDefaultsToImmediateSchedulerWhenSchedulerIsNull() {
    Sinks.Many<String> sink = Sinks.many().multicast().onBackpressureBuffer();

    Flux<String> consumed = reactorEventBus.consume(sink, null);

    sink.tryEmitNext("event-1");
    sink.tryEmitComplete();

    StepVerifier.create(consumed)
        .expectNext("event-1")
        .verifyComplete();
  }

  @Test
  void consumeDeliversEventsThroughGivenScheduler() {
    Sinks.Many<String> sink = Sinks.many().multicast().onBackpressureBuffer();

    Flux<String> consumed = reactorEventBus.consume(sink, Schedulers.boundedElastic());

    sink.tryEmitNext("event-1");
    sink.tryEmitComplete();

    StepVerifier.create(consumed)
        .expectNext("event-1")
        .verifyComplete();
  }
}
