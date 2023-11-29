package com.nantaaditya.cronscheduler.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.test.StepVerifier;

@ExtendWith(MockitoExtension.class)
class NoOpNotificationCallbackImplTest {

  @InjectMocks
  private NoOpNotificationCallbackImpl callback;

  @Test
  void notifyFailed() {
    StepVerifier.create(callback.notifyFailed(null))
        .expectNext(Boolean.TRUE)
        .verifyComplete();
  }
}