package com.nantaaditya.cronscheduler.listener;

import java.util.Map.Entry;
import lombok.extern.slf4j.Slf4j;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

@Slf4j
public class JobListener implements org.quartz.JobListener {

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

  private String toString(JobDataMap jobDataMap) {
    StringBuilder sb = new StringBuilder("(");

    int i = 0;
    for (Entry<String, Object> entry : jobDataMap.entrySet()) {
      sb.append(entry.getKey()).append(":").append(entry.getValue());
      if (i != jobDataMap.size() - 1) {
        sb.append(",");
      }
      i++;
    }

    sb.append(")");
    return sb.toString();
  }
}
