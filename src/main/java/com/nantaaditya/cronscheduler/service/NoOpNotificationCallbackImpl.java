package com.nantaaditya.cronscheduler.service;

import com.nantaaditya.cronscheduler.model.dto.NotificationCallbackDTO;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

@Slf4j
public class NoOpNotificationCallbackImpl implements
    NotificationCallback {

  @Override
  public Mono<Boolean> notifySuccess(NotificationCallbackDTO notificationCallback) {
    return Mono.just(Boolean.TRUE);
  }

  @Override
  public Mono<Boolean> notifyFailed(NotificationCallbackDTO notificationCallback) {
    log.warn("#NoOpNotification - notify failed");
    return Mono.just(Boolean.TRUE);
  }
}
