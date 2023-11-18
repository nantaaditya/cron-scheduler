package com.nantaaditya.cronscheduler.util;

import com.nantaaditya.cronscheduler.entity.JobDetail;
import com.nantaaditya.cronscheduler.entity.JobTrigger;
import com.nantaaditya.cronscheduler.job.WebClientJob;
import com.nantaaditya.cronscheduler.model.constant.JobDataMapKey;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.quartz.CronScheduleBuilder;
import org.quartz.JobBuilder;
import org.quartz.JobDataMap;
import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;
import org.springframework.stereotype.Component;
import reactor.util.function.Tuple2;
import reactor.util.function.Tuples;

@Slf4j
@Component
@RequiredArgsConstructor
public class QuartzUtil {

  private final Scheduler scheduler;

  public void createJob(JobDetail jobDetail, boolean runNow) {
    if (!runNow) return;

    Tuple2<org.quartz.JobDetail, Trigger> tuples = buildJobDetailAndTrigger(jobDetail);
    run(tuples, jobDetail);
  }

  private void run(Tuple2<org.quartz.JobDetail, Trigger> tuples, JobDetail jobDetail) {
    try {
      scheduler.scheduleJob(tuples.getT1(), tuples.getT2());
    } catch (SchedulerException e) {
      log.error("#JOB - failed initialize job {}, ", jobDetail.getJobData(JobDataMapKey.JOB_EXECUTOR_ID), e);
    }
  }

  private Tuple2<org.quartz.JobDetail, Trigger> buildJobDetailAndTrigger(JobDetail jobDetail) {
    JobDataMap jobDataMap = new JobDataMap();
    jobDetail.initializeJobDataMap(jobDataMap);
    JobTrigger jobTrigger = (JobTrigger) jobDataMap.get(JobDataMapKey.JOB_TRIGGER);

    org.quartz.JobDetail job = JobBuilder.newJob(WebClientJob.class)
        .withIdentity(jobDataMap.getString(JobDataMapKey.JOB_EXECUTOR_ID), WebClientJob.WEB_CLIENT_JOB_GROUP)
        .setJobData(jobDataMap)
        .build();

    Trigger trigger = TriggerBuilder.newTrigger()
        .withIdentity(jobDataMap.getString(JobDataMapKey.JOB_EXECUTOR_ID), WebClientJob.WEB_CLIENT_JOB_GROUP)
        .startNow()
        .withSchedule(CronScheduleBuilder.cronSchedule(jobTrigger.getTriggerCron()))
        .forJob(job)
        .build();

    return Tuples.of(job, trigger);
  }

  public void updateJob(JobDetail jobDetail, boolean runNow) {
    removeJob((String) jobDetail.getJobData(JobDataMapKey.JOB_EXECUTOR_ID));
    createJob(jobDetail, runNow);
  }

  public void removeJobs(List<String> jobExecutorIds) {
    jobExecutorIds.stream()
        .forEach(this::removeJob);
  }

  public void removeJob(String jobExecutorId) {
    try {
      scheduler.deleteJob(JobKey.jobKey(jobExecutorId, WebClientJob.WEB_CLIENT_JOB_GROUP));
    } catch (SchedulerException e) {
      log.error("#JOB - failed remove job {}", jobExecutorId, e);
    }
  }
}
