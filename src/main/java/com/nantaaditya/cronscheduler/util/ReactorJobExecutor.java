package com.nantaaditya.cronscheduler.util;

import java.time.Duration;
import java.util.concurrent.TimeoutException;
import java.util.function.Function;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@Slf4j
public class ReactorJobExecutor {

  private ReactorJobExecutor() {}

  public static <T, R> Mono<T> execute(Function<R, Mono<T>> callback, R resource, Duration timeout) {
    return Mono.using(
        () -> {
          log.info("#REACTOR - executor start");
          return resource;
        },
        source -> Mono.just(source)
            .publishOn(Schedulers.single())
            .flatMap(callback)
            .timeout(timeout)
            .doOnError(TimeoutException.class, error -> log.error("#Reactor - timeout when execute, ", error)),
        source -> log.info("#REACTOR - executor finish")
    );
  }


}
