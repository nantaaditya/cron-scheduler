package com.nantaaditya.cronscheduler.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties("cron.quartz")
public class QuartzProperties {
  private int threadPool;
}
