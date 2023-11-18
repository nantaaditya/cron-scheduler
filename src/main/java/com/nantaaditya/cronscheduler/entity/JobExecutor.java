package com.nantaaditya.cronscheduler.entity;

import com.fasterxml.jackson.core.type.TypeReference;
import com.nantaaditya.cronscheduler.job.WebClientJob;
import com.nantaaditya.cronscheduler.model.constant.JobDataMapKey;
import com.nantaaditya.cronscheduler.model.request.CreateJobExecutorRequestDTO;
import com.nantaaditya.cronscheduler.util.JsonHelper;
import io.r2dbc.postgresql.codec.Json;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.quartz.JobDataMap;
import org.springframework.data.annotation.Transient;
import org.springframework.data.relational.core.mapping.Table;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@Table(value = "job_executor")
public class JobExecutor extends BaseEntity {
  private String clientId;
  private String jobName;
  private String jobGroup;
  private Json jobData;
  private String triggerCron;
  private boolean active;

  public static JobExecutor create(CreateJobExecutorRequestDTO request, ClientRequest clientRequest) {
    Map<String, Object> map = new HashMap<>();
    String id = BaseEntity.generateId();
    map.put(JobDataMapKey.CLIENT_REQUEST, JsonHelper.toJsonString(clientRequest));
    map.put(JobDataMapKey.JOB_EXECUTOR_ID, id);

    return JobExecutor.builder()
        .id(id)
        .createdBy(BaseEntity.AUDITOR)
        .modifiedBy(BaseEntity.AUDITOR)
        .clientId(request.getClientId())
        .jobName(request.getJobName())
        .jobGroup(WebClientJob.WEB_CLIENT_JOB_GROUP)
        .triggerCron(request.getCronTriggerExpression())
        .active(request.isEnable())
        .jobData(JsonHelper.toJson(map))
        .build();
  }

  public static List<JobExecutor> from(List<Map<String, Object>> rows) {
    return rows.stream()
        .map(JobExecutor::from)
        .collect(Collectors.toList());
  }

  public static JobExecutor from(Map<String, Object> row) {
    return JobExecutor.builder()
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
        .triggerCron((String) row.get("trigger_cron"))
        .active((boolean) row.get("e_active"))
        .build();
  }

  public void loadJobDataMap(JobDataMap jobDataMap) {
    String clientRequestJson = (String) getJobData(JobDataMapKey.CLIENT_REQUEST);
    jobDataMap.put(JobDataMapKey.CLIENT_REQUEST, JsonHelper.fromJson(clientRequestJson, ClientRequest.class));
    jobDataMap.put(JobDataMapKey.JOB_EXECUTOR_ID, getId());
  }

  public void putJobDataMap(ClientRequest clientRequest) {
    Map<String, Object> map = new HashMap<>();
    map.put(JobDataMapKey.CLIENT_REQUEST, JsonHelper.toJsonString(clientRequest));
    setJobData(JsonHelper.toJson(map));
  }

  @Transient
  public Map<String, Object> getJobData() {
    if (ObjectUtils.isEmpty(jobData)) return new HashMap<>();

    return JsonHelper.fromJson(jobData, new TypeReference<Map<String, Object>>() {});
  }

  @Transient
  public Object getJobData(String key) {
    return Optional.ofNullable(getJobData())
        .map(map -> map.get(key))
        .orElse(null);
  }

  @Transient
  public Optional<ClientRequest> getClientRequest() {
    return Optional.ofNullable(getJobData())
        .map(result -> (String) result.get(JobDataMapKey.CLIENT_REQUEST))
        .filter(StringUtils::hasLength)
        .map(clientRequestString -> JsonHelper.fromJson(clientRequestString, ClientRequest.class));
  }
}
