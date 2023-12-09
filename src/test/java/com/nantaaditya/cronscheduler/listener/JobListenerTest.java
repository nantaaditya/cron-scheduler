package com.nantaaditya.cronscheduler.listener;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

@ExtendWith(MockitoExtension.class)
class JobListenerTest {

  @InjectMocks
  private JobListener jobListener;

  @Mock
  private JobExecutionContext jobExecutionContext;

  @Mock
  private JobDataMap jobDataMap;

  @Test
  void jobExecutionVetoed() {
    when(jobExecutionContext.getMergedJobDataMap()).thenReturn(jobDataMap);
    jobListener.jobExecutionVetoed(jobExecutionContext);
    verify(jobExecutionContext).getMergedJobDataMap();
  }

  @Test
  void jobWasExecuted() {
    when(jobExecutionContext.getMergedJobDataMap()).thenReturn(jobDataMap);
    jobListener.jobWasExecuted(jobExecutionContext, new JobExecutionException("error"));
    verify(jobExecutionContext).getMergedJobDataMap();
  }
}