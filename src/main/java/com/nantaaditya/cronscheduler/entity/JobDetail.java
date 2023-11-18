package com.nantaaditya.cronscheduler.entity;

import com.fasterxml.jackson.core.type.TypeReference;
import com.nantaaditya.cronscheduler.job.WebClientJob;
import com.nantaaditya.cronscheduler.model.constant.JobDataMapKey;
import com.nantaaditya.cronscheduler.model.request.CreateJobExecutorRequestDTO;
import com.nantaaditya.cronscheduler.util.JsonHelper;
import io.r2dbc.postgresql.codec.Json;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.quartz.JobDataMap;
import org.springframework.data.annotation.Transient;
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
  private String jobGroup;
  private Json jobData;
  @Transient
  private String jobExecutorId;
  @Transient
  private String jobTriggerId;
  @Transient
  private boolean active;

  public static JobDetail of(CreateJobExecutorRequestDTO request) {
    return JobDetail.builder()
        .id(BaseEntity.generateId())
        .createdBy(BaseEntity.AUDITOR)
        .modifiedBy(BaseEntity.AUDITOR)
        .clientId(request.getClientId())
        .jobName(request.getJobName())
        .jobGroup(WebClientJob.WEB_CLIENT_JOB_GROUP)
        .build();
  }
  
  public static List<JobDetail> from(List<Map<String, Object>> rows) {
    List<JobDetail> jobDetails = new ArrayList<>();
    for (Map<String, Object> row : rows) {
      jobDetails.add(JobDetail.builder()
          .id((String) row.get("j_id"))
          .createdBy((String) row.get("j_created_by"))
          .createdDate((LocalDate) row.get("j_created_date"))
          .createdTime((LocalTime) row.get("j_created_tim"))
          .modifiedBy((String) row.get("j_modified_by"))
          .modifiedDate((LocalDate) row.get("j_modified_date"))
          .modifiedTime((LocalTime) row.get("j_modified_time"))
          .version(Long.parseLong((String) row.get("j_version")))
          .clientId((String) row.get("j_client_id"))
          .jobName((String) row.get("j_job_name"))
          .jobGroup((String) row.get("j_job_group"))
          .jobData((Json) row.get("j_job_data"))
          .jobExecutorId((String) row.get("e_id"))
          .jobTriggerId((String) row.get("e_trigger_id"))
          .active((boolean) row.get("e_active"))
          .build()
      );
    }
    return jobDetails;
  }

  @Transient
  public Map<String, Object> getJobData() {
    if (ObjectUtils.isEmpty(jobData)) return new HashMap<>();

    return JsonHelper.fromJson(jobData, new TypeReference<Map<String, Object>>() {});
  }

  @Transient
  public Object getJobData(String key) {
    return Optional.ofNullable(getJobData())
        .map(result -> result.get(key));
  }

  public void updateClientRequest(ClientRequest clientRequest) {
    Map<String, Object> jobData = getJobData();
    jobData.put(JobDataMapKey.CLIENT_REQUEST, JsonHelper.toJsonString(clientRequest));

    setJobData(JsonHelper.toJson(jobData));
  }

  public void updateJobTrigger(JobTrigger jobTrigger) {
    Map<String, Object> jobData = getJobData();
    jobData.put(JobDataMapKey.JOB_TRIGGER, JsonHelper.toJsonString(jobTrigger));

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
