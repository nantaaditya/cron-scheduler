package com.nantaaditya.cronscheduler.properties;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties("job.configuration")
public class JobProperties {

  private WebClient webClient;

  @Data
  @NoArgsConstructor
  public static class WebClient {
    private int connectTimeOut;
    private int responseTimeOut;
    private int readTimeOut;
    private int writeTimeOut;
  }
}
