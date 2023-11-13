package com.nantaaditya.cronscheduler;

import com.nantaaditya.cronscheduler.properties.JobProperties;
import com.nantaaditya.cronscheduler.properties.QuartzProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.data.r2dbc.config.EnableR2dbcAuditing;

@EnableR2dbcAuditing
@SpringBootApplication
@EnableConfigurationProperties(value = {
    JobProperties.class,
    QuartzProperties.class,
})
public class CronSchedulerApplication {

  public static void main(String[] args) {
    SpringApplication.run(CronSchedulerApplication.class, args);
  }

}
