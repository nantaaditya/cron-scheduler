package com.nantaaditya.cronscheduler.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties("cron.quartz")
public class QuartzProperties {
  private String instanceName;
  private String threadPoolClass;
  private String threadName;
  private int threadCount;
  private int threadPriority;
  private int misfireThreshold;
  private String jobStoreClass;
}
