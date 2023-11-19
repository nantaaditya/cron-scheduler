package com.nantaaditya.cronscheduler.listener;

import java.util.Map.Entry;
import lombok.extern.slf4j.Slf4j;
import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.quartz.JobKey;
import org.quartz.SchedulerException;
import org.quartz.Trigger;
import org.quartz.TriggerKey;

@Slf4j
public class SchedulerListener implements org.quartz.SchedulerListener {

  @Override
  public void jobScheduled(Trigger trigger) {
    log.info("#SCHEDULER - job scheduled: {}", trigger.getKey());
  }

  @Override
  public void jobUnscheduled(TriggerKey triggerKey) {
    log.info("#SCHEDULER - job unscheduled: {}", triggerKey);
  }

  @Override
  public void triggerFinalized(Trigger trigger) {
    log.info("#SCHEDULER - trigger fired: {}", trigger.getKey());
  }

  @Override
  public void triggerPaused(TriggerKey triggerKey) {
    log.info("#SCHEDULER - trigger paused: {}", triggerKey);
  }

  @Override
  public void triggersPaused(String triggerGroup) {
    log.info("#SCHEDULER - trigger group paused: {}", triggerGroup);
  }

  @Override
  public void triggerResumed(TriggerKey triggerKey) {
    log.info("#SCHEDULER - trigger resumed: {}", triggerKey);
  }

  @Override
  public void triggersResumed(String triggerGroup) {
    log.info("#SCHEDULER - trigger group resumed: {}", triggerGroup);
  }

  @Override
  public void jobAdded(JobDetail jobDetail) {
    log.info("#SCHEDULER - job added: {}, context {}", jobDetail.getKey(), toString(jobDetail.getJobDataMap()));
  }

  @Override
  public void jobDeleted(JobKey jobKey) {
    log.info("#SCHEDULER - job deleted: {}", jobKey);
  }

  @Override
  public void jobPaused(JobKey jobKey) {
    log.info("#SCHEDULER - job paused: {}", jobKey);
  }

  @Override
  public void jobsPaused(String jobGroup) {
    log.info("#SCHEDULER - job group paused: {}", jobGroup);
  }

  @Override
  public void jobResumed(JobKey jobKey) {
    log.info("#SCHEDULER - job resumed: {}", jobKey);
  }

  @Override
  public void jobsResumed(String jobGroup) {
    log.info("#SCHEDULER - job group resumed: {}", jobGroup);
  }

  @Override
  public void schedulerError(String msg, SchedulerException cause) {
    log.error("#SCHEDULER - error {}, ", msg, cause);
  }

  @Override
  public void schedulerInStandbyMode() {
    log.info("#SCHEDULER - standby");
  }

  @Override
  public void schedulerStarted() {
    log.info("#SCHEDULER - started");
  }

  @Override
  public void schedulerStarting() {
    log.info("#SCHEDULER - starting");
  }

  @Override
  public void schedulerShutdown() {
    log.info("#SCHEDULER - shutdown");
  }

  @Override
  public void schedulerShuttingdown() {
    log.info("#SCHEDULER - on progress shutdown");
  }

  @Override
  public void schedulingDataCleared() {
    log.info("#SCHEDULER - cleared");
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
