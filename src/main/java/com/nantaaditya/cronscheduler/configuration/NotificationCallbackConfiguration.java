package com.nantaaditya.cronscheduler.configuration;

import com.nantaaditya.cronscheduler.service.NoOpNotificationCallbackImpl;
import com.nantaaditya.cronscheduler.service.NotificationCallback;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class NotificationCallbackConfiguration {

  @Bean
  public NotificationCallback notificationCallback() {
    return new NoOpNotificationCallbackImpl();
  }
}
