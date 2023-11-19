package com.nantaaditya.cronscheduler.listener;

import lombok.extern.slf4j.Slf4j;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

@Slf4j
public class JobListener
    extends BaseListener
    implements org.quartz.JobListener{

  @Override
  public String getName() {
    return "job-listener";
  }

  @Override
  public void jobToBeExecuted(JobExecutionContext context) {
    log.info("#JOB_LISTENER - job to be executed: context {}", toString(context.getMergedJobDataMap()));
  }

  @Override
  public void jobExecutionVetoed(JobExecutionContext context) {
    log.info("#JOB_LISTENER - job execution vetoed: context {}", toString(context.getMergedJobDataMap()));
  }

  @Override
  public void jobWasExecuted(JobExecutionContext context, JobExecutionException jobException) {
    log.info("#JOB_LISTENER - job was executed: context {}", toString(context.getMergedJobDataMap()));
    if (jobException != null) {
      log.error("#JOB_LISTENER - job was executed error, ", jobException);
    }
  }

}
