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

  public static <T, R> Mono<T> execute(Function<R, Mono<T>> callback, R resource,
      Mono<T> fallback, Duration timeout) {
    return execute(
        callback,
        resource,
        error -> ReactorLogContext.syncMdc(() ->
                log.error("#REACTOR - executor error, {}", error))
            .then(fallback),
        timeout
    );
  }

  public static <T, R> Mono<T> execute(Function<R, Mono<T>> callback, R resource,
      Function<Throwable, Mono<T>> fallback, Duration timeout) {
    return Mono.deferContextual(contextView -> Mono.using(
        () -> {
          ReactorLogContext.applyMdc(contextView);
          log.info("#REACTOR - executor start");
          log.debug("#REACTOR - request {} timeout {}", resource, timeout);
          return resource;
        },
        source -> Mono.just(source)
            .publishOn(Schedulers.single())
            .flatMap(callback)
            .timeout(timeout)
            .doOnError(TimeoutException.class, error -> {
              ReactorLogContext.applyMdc(contextView);
              log.error("#REACTOR - timeout when execute, {}", error.getMessage());
            })
            .onErrorResume(fallback),
        source -> {
          ReactorLogContext.applyMdc(contextView);
          log.info("#REACTOR - executor finish");
        }
    ));
  }
}
