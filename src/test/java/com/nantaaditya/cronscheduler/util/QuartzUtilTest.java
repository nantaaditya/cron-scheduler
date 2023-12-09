package com.nantaaditya.cronscheduler.util;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.nantaaditya.cronscheduler.entity.JobExecutor;
import io.r2dbc.postgresql.codec.Json;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.quartz.JobDetail;
import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.Trigger;

@Slf4j
@ExtendWith(MockitoExtension.class)
class QuartzUtilTest {

  @InjectMocks
  private QuartzUtil quartzUtil;

  @Mock
  private Scheduler scheduler;

  @Test
  @SneakyThrows
  void runNow() {
    String jobData = """
        {
          "clientRequest": "{\\"id\\":\\"1\\",\\"createdBy\\":\\"SYSTEM\\",\\"createdDate\\":[2023,11,21],\\"createdTime\\":[10,50,0,390049000],\\"modifiedBy\\":\\"SYSTEM\\",\\"modifiedDate\\":[2023,11,21],\\"modifiedTime\\":[10,50,0,390049000],\\"version\\":1,\\"clientName\\":\\"mock-bin\\",\\"httpMethod\\":\\"GET\\",\\"baseUrl\\":\\"https://8b3817ceae844514bd45aad137f8ee1d.api.mockbin.io\\",\\"apiPath\\":\\"/\\",\\"headers\\":{\\"Content-Type\\":[\\"application/json\\"]},\\"fullApiPath\\":\\"/\\"}",
          "jobExecutorId": "1"
        }
        """;
    JobExecutor jobExecutor = JobExecutor.builder()
        .id("1")
        .jobData(Json.of(jobData))
        .triggerCron("0 0/5 0 ? * * *")
        .build();

    when(scheduler.scheduleJob(any(JobDetail.class), any(Trigger.class)))
        .thenThrow(new SchedulerException("failed"));
    quartzUtil.runNow(jobExecutor);
    verify(scheduler).scheduleJob(any(JobDetail.class), any(Trigger.class));
  }

  @Test
  @SneakyThrows
  void removeJob() {
    when(scheduler.deleteJob(any(JobKey.class)))
        .thenThrow(new SchedulerException("failed"));
    quartzUtil.removeJob("1");
    verify(scheduler).deleteJob(any(JobKey.class));
  }
}