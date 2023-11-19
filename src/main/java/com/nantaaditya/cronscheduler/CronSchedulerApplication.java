package com.nantaaditya.cronscheduler;

import com.nantaaditya.cronscheduler.properties.JobProperties;
import com.nantaaditya.cronscheduler.properties.QuartzProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.data.r2dbc.config.EnableR2dbcAuditing;
import reactor.core.publisher.Hooks;

@EnableR2dbcAuditing
@SpringBootApplication
@EnableConfigurationProperties(value = {
    JobProperties.class,
    QuartzProperties.class,
})
public class CronSchedulerApplication {

  public static void main(String[] args) {
    Hooks.enableAutomaticContextPropagation();
    SpringApplication.run(CronSchedulerApplication.class, args);
  }

}
