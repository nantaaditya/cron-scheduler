package com.nantaaditya.cronscheduler.listener;

import java.util.Map.Entry;
import lombok.extern.slf4j.Slf4j;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.Trigger;
import org.quartz.Trigger.CompletedExecutionInstruction;
import org.quartz.TriggerListener;

@Slf4j
public class JobTriggerListener implements TriggerListener {

  @Override
  public String getName() {
    return "job-trigger-listener";
  }

  @Override
  public void triggerFired(Trigger trigger, JobExecutionContext context) {
    log.info("#JOB_TRIGGER - trigger fired: name {}, context {}", trigger.getKey(), toString(context.getMergedJobDataMap()));
  }

  @Override
  public boolean vetoJobExecution(Trigger trigger, JobExecutionContext context) {
    log.info("#JOB_TRIGGER - trigger vetoed: name {}, context {}", trigger.getKey(), toString(context.getMergedJobDataMap()));
    return false;
  }

  @Override
  public void triggerMisfired(Trigger trigger) {
    log.info("#JOB_TRIGGER - trigger misfired, name {}", trigger.getKey());
  }

  @Override
  public void triggerComplete(Trigger trigger, JobExecutionContext context,
      CompletedExecutionInstruction triggerInstructionCode) {
    log.info("#JOB_TRIGGER - trigger complete: name {}, context {}", trigger.getKey(), toString(context.getMergedJobDataMap()));
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
