package com.nantaaditya.cronscheduler.service;

import com.nantaaditya.cronscheduler.model.dto.NotificationCallbackDTO;
import reactor.core.publisher.Mono;

public interface NotificationCallback {
  Mono<Boolean> notifySuccess(NotificationCallbackDTO notificationCallback);

  Mono<Boolean> notifyFailed(NotificationCallbackDTO notificationCallback);
}
