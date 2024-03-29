package com.nantaaditya.cronscheduler.configuration;

import org.quartz.JobExecutionContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import reactor.core.publisher.Sinks;

@Configuration
public class ReactorConfiguration {

  private static final int WEB_CLIENT_JOB_SINK_LIMIT = 100;

  @Bean
  public Sinks.Many<JobExecutionContext> webClientJobSink() {
    return Sinks.many().replay().all(WEB_CLIENT_JOB_SINK_LIMIT);
  }
}
