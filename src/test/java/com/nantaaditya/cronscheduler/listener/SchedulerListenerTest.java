package com.nantaaditya.cronscheduler.listener;

import com.nantaaditya.cronscheduler.listener.SchedulerListener;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.quartz.JobKey;
import org.quartz.SchedulerException;
import org.quartz.TriggerKey;

@ExtendWith(MockitoExtension.class)
class SchedulerListenerTest {

  @InjectMocks
  private SchedulerListener schedulerListener;

  @Mock
  private TriggerKey triggerKey;

  @Mock
  private JobKey jobKey;

  @Test
  void triggerPaused() {
    schedulerListener.triggerPaused(triggerKey);
  }

  @Test
  void triggersPaused() {
    schedulerListener.triggersPaused("key");
  }

  @Test
  void triggerResumed() {
    schedulerListener.triggerResumed(triggerKey);
  }

  @Test
  void triggersResumed() {
    schedulerListener.triggersResumed("key");
  }

  @Test
  void jobPaused() {
    schedulerListener.jobPaused(jobKey);
  }

  @Test
  void jobsPaused() {
    schedulerListener.jobsPaused("key");
  }

  @Test
  void jobResumed() {
    schedulerListener.jobResumed(jobKey);
  }

  @Test
  void jobsResumed() {
    schedulerListener.jobsResumed("key");
  }

  @Test
  void schedulerError() {
    schedulerListener.schedulerError("failure", new SchedulerException("error"));
  }

  @Test
  void schedulerShutdown() {
    schedulerListener.schedulerShutdown();
  }

  @Test
  void schedulerShuttingDown() {
    schedulerListener.schedulerShuttingdown();
  }

  @Test
  void schedulingDataCleared() {
    schedulerListener.schedulingDataCleared();
  }
}