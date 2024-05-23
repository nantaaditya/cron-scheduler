package com.nantaaditya.cronscheduler.util;

import java.time.Duration;
import java.util.concurrent.TimeoutException;
import java.util.function.Function;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@ExtendWith(MockitoExtension.class)
class ReactorJobExecutorTest {

  @Test
  void executeTimeout() {
    Function<String, Mono<String>> callback = (str) -> Mono.fromCallable(() -> {
      for (int i = 0; i < 5; i++) {
        System.out.println("delay "+i);
        Thread.sleep(1000);
      }
      return str;
    });

    StepVerifier.create(ReactorJobExecutor.execute(callback, "str", Mono.just("fallback"), Duration.ofSeconds(1)))
        .expectNext("fallback")
        .verifyComplete();
  }
}
