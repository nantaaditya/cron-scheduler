package com.nantaaditya.cronscheduler.entity;

import com.fasterxml.jackson.core.type.TypeReference;
import com.nantaaditya.cronscheduler.model.constant.JobDataMapKey;
import com.nantaaditya.cronscheduler.util.JsonHelper;
import io.r2dbc.postgresql.codec.Json;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.quartz.JobDataMap;
import org.springframework.data.relational.core.mapping.Table;
import org.springframework.util.ObjectUtils;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@Table(value = "job_detail")
public class JobDetail extends BaseEntity {
  private String clientId;
  private String jobName;
  private String jobIdentity;
  private String jobGroup;
  private Json jobData;

  public Map<String, Object> getJobData() {
    if (ObjectUtils.isEmpty(jobData)) return null;

    return JsonHelper.fromJson(jobData, new TypeReference<Map<String, Object>>() {});
  }

  public Object getJobData(String key) {
    return Optional.ofNullable(getJobData())
        .map(result -> result.get(key));
  }

  public void updateClientRequest(ClientRequest clientRequest) {
    Map<String, Object> jobData = getJobData();
    jobData.put(JobDataMapKey.CLIENT_REQUEST, JsonHelper.toJsonString(clientRequest));

    setJobData(JsonHelper.toJson(jobData));
  }

  public void initializeJobDataMap(JobDataMap jobDataMap) {
    String jobExecutorId = (String) getJobData(JobDataMapKey.JOB_EXECUTOR_ID);
    jobDataMap.put(JobDataMapKey.JOB_EXECUTOR_ID, jobExecutorId);
    String clientRequestJson = (String) getJobData(JobDataMapKey.CLIENT_REQUEST);
    jobDataMap.put(JobDataMapKey.CLIENT_REQUEST, JsonHelper.fromJson(clientRequestJson, ClientRequest.class));
    String jobTriggerJson = (String) getJobData(JobDataMapKey.JOB_TRIGGER);
    jobDataMap.put(JobDataMapKey.JOB_TRIGGER, JsonHelper.fromJson(jobTriggerJson, JobTrigger.class));
  }

  public void putJobDataMap(String jobExecutorId, ClientRequest clientRequest, JobTrigger jobTrigger) {
    Map<String, Object> map = new HashMap<>();
    map.put(JobDataMapKey.JOB_EXECUTOR_ID, jobExecutorId);
    map.put(JobDataMapKey.CLIENT_REQUEST, JsonHelper.toJsonString(clientRequest));
    map.put(JobDataMapKey.JOB_TRIGGER, JsonHelper.toJsonString(jobTrigger));
    setJobData(JsonHelper.toJson(map));
  }
}
