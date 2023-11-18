package com.nantaaditya.cronscheduler.configuration;

import com.nantaaditya.cronscheduler.repository.JobExecutorRepository;
import com.nantaaditya.cronscheduler.util.QuartzUtil;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class QuartzInitializerConfiguration {

  private final JobExecutorRepository jobExecutorRepository;

  private final Scheduler scheduler;

  private final QuartzUtil quartzUtil;

  @EventListener(ApplicationReadyEvent.class)
  public void onStart() throws SchedulerException {
    scheduler.start();

    jobExecutorRepository.findAllByActiveTrue()
        .doOnNext(jobExecutor -> quartzUtil.createJob(jobExecutor))
        .subscribe(result -> log.info("#JOB - initialization completed"));
  }

  @PreDestroy
  public void onStop() throws SchedulerException {
    scheduler.shutdown(true);
    log.info("#JOB - shutdown");
  }

}
