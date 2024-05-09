package com.nantaaditya.cronscheduler.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;

@Slf4j
@Component
public class ReactorEventBus {
  public <E> void publish(Sinks.Many<E> events, E event) {
    if (events == null || event == null) {
      log.warn("#REACTOR - event payload is null");
      return;
    }
    events.tryEmitNext(event).orThrow();
  }

  public <E> Flux<E> consume(Sinks.Many<E> events, Scheduler scheduler) {
    if (events == null) {
      log.warn("#REACTOR - event payload is null");
      return Flux.empty();
    }

    if (scheduler == null) {
      scheduler = Schedulers.immediate(); //NOSONAR
    }

    return events.asFlux()
        .doOnNext(e -> log.debug("#REACTOR - consume event {}", e))
        .publishOn(scheduler);
  }
}
