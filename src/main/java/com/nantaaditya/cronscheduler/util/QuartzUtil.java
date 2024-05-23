package com.nantaaditya.cronscheduler.util;

import com.nantaaditya.cronscheduler.entity.JobExecutor;
import com.nantaaditya.cronscheduler.job.WebClientJob;
import com.nantaaditya.cronscheduler.model.constant.JobDataMapKey;
import io.micrometer.tracing.TraceContext;
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
  private final TraceUtil traceUtil;

  public void createJob(JobExecutor jobExecutor) {
    if (!jobExecutor.isActive()) return;

    Tuple2<org.quartz.JobDetail, Trigger> tuples = buildJobDetailAndTrigger(jobExecutor, false);
    run(jobExecutor.getId(), tuples);
  }

  private void run(String jobExecutorId, Tuple2<org.quartz.JobDetail, Trigger> tuples) {
    try {
      scheduler.scheduleJob(tuples.getT1(), tuples.getT2());
    } catch (SchedulerException e) {
      log.error("#JOB - failed initialize job {}, ", jobExecutorId, e);
    }
  }

  private Tuple2<org.quartz.JobDetail, Trigger> buildJobDetailAndTrigger(JobExecutor jobExecutor, boolean runOnce) {
    JobDataMap jobDataMap = new JobDataMap();
    jobExecutor.loadJobDataMap(jobDataMap);

    // set up trace context
    TraceContext traceContext = traceUtil.getTraceContext();
    jobDataMap.put(JobDataMapKey.TRACE_ID, traceContext.traceId());
    jobDataMap.put(JobDataMapKey.SPAN_ID, traceContext.spanId());

    String jobGroup = runOnce ?
        WebClientJob.INSTANT_WEB_CLIENT_JOB_GROUP : WebClientJob.WEB_CLIENT_JOB_GROUP;

    org.quartz.JobDetail job = JobBuilder.newJob(WebClientJob.class)
        .withIdentity(jobExecutor.getId(), jobGroup)
        .setJobData(jobDataMap)
        .build();

    TriggerBuilder<Trigger> triggerBuilder = TriggerBuilder.newTrigger()
        .withIdentity(jobExecutor.getId(), jobGroup)
        .forJob(job);

    if (runOnce) {
      triggerBuilder
          .startNow();
    } else {
      triggerBuilder
          .withSchedule(CronScheduleBuilder.cronSchedule(jobExecutor.getTriggerCron()));
    }

    return Tuples.of(job, triggerBuilder.build());
  }

  public void updateJob(JobExecutor jobExecutor) {
    removeJob(jobExecutor.getId());
    createJob(jobExecutor);
  }

  public void removeJobs(List<String> jobExecutorIds) {
    jobExecutorIds
        .forEach(this::removeJob);
  }

  public void removeJob(String jobExecutorId) {
    try {
      scheduler.deleteJob(JobKey.jobKey(jobExecutorId, WebClientJob.WEB_CLIENT_JOB_GROUP));
    } catch (SchedulerException e) {
      log.error("#JOB - failed remove job {}", jobExecutorId, e);
    }
  }

  public void runNow(JobExecutor jobExecutor) {
    Tuple2<org.quartz.JobDetail, Trigger> tuples = buildJobDetailAndTrigger(jobExecutor, true);
    run(jobExecutor.getId(), tuples);
  }
}
