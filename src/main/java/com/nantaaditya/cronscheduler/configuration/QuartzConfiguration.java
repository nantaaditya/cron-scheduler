package com.nantaaditya.cronscheduler.configuration;

import com.nantaaditya.cronscheduler.properties.QuartzProperties;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.impl.DirectSchedulerFactory;
import org.quartz.simpl.SimpleThreadPool;
import org.quartz.spi.JobStore;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.quartz.SimpleThreadPoolTaskExecutor;

@Configuration
public class QuartzConfiguration {

  @Bean
  public Scheduler scheduler(JobStore jobStore, QuartzProperties quartzProperties)
      throws SchedulerException {
    DirectSchedulerFactory schedulerFactory = DirectSchedulerFactory.getInstance();

    SimpleThreadPool threadPool = new SimpleThreadPoolTaskExecutor();
    threadPool.setInstanceName("quartz_scheduler");
    threadPool.setInstanceId("quartz_scheduler");
    threadPool.setThreadCount(quartzProperties.getThreadPool());
    schedulerFactory.createScheduler(threadPool, jobStore);

    //TODO: listener

    return schedulerFactory.getScheduler();
  }

}
