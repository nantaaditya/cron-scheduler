package com.nantaaditya.cronscheduler.configuration;

import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;

import com.nantaaditya.cronscheduler.configuration.QuartzInitializerConfiguration;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.quartz.Scheduler;

@ExtendWith(MockitoExtension.class)
class QuartzInitializerConfigurationTest {

  @InjectMocks
  private QuartzInitializerConfiguration configuration;

  @Mock
  private Scheduler scheduler;

  @Test
  @SneakyThrows
  void onStop() {
    doNothing().when(scheduler).shutdown(true);
    configuration.onStop();
    verify(scheduler).shutdown(true);
  }
}