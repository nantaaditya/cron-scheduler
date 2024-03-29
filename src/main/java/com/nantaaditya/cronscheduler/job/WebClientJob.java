package com.nantaaditya.cronscheduler.job;

import com.nantaaditya.cronscheduler.util.ReactorEventBus;
import lombok.extern.slf4j.Slf4j;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Sinks;

@Slf4j
@Component
@DisallowConcurrentExecution
public class WebClientJob implements Job {

  @Autowired
  private Sinks.Many<JobExecutionContext> webClientJobSink;

  @Autowired
  private ReactorEventBus reactorEventBus;

  public static final String WEB_CLIENT_JOB_GROUP = "WebClientJobGroup";
  public static final String INSTANT_WEB_CLIENT_JOB_GROUP = "InstantWebClientJobGroup";

  @Override
  public void execute(JobExecutionContext context) throws JobExecutionException {
    reactorEventBus.publish(webClientJobSink, context);
  }

}
