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
import org.springframework.data.relational.core.mapping.Table;
import org.springframework.util.ObjectUtils;

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
        .id((String) row.get("e_id"))
        .createdBy((String) row.get("e_created_by"))
        .createdDate((LocalDate) row.get("e_created_date"))
        .createdTime((LocalTime) row.get("e_created_tim"))
        .modifiedBy((String) row.get("e_modified_by"))
        .modifiedDate((LocalDate) row.get("e_modified_date"))
        .modifiedTime((LocalTime) row.get("e_modified_time"))
        .version((Long) row.get("e_version"))
        .clientId((String) row.get("e_client_id"))
        .jobName((String) row.get("e_job_name"))
        .jobGroup((String) row.get("e_job_group"))
        .jobData((Json) row.get("e_job_data"))
        .triggerCron((String) row.get("e_trigger_cron"))
        .active((boolean) row.get("e_active"))
        .build();
  }

  public void loadJobDataMap(JobDataMap jobDataMap) {
    Map<String, Object> jobData = JsonHelper.fromJson(this.jobData.asString(), Map.class);
    jobDataMap.put(JobDataMapKey.JOB_EXECUTOR_ID, jobData.get(JobDataMapKey.JOB_EXECUTOR_ID));
    jobDataMap.put(JobDataMapKey.CLIENT_REQUEST, jobData.get(JobDataMapKey.CLIENT_REQUEST));
    jobDataMap.put(JobDataMapKey.CRON_TRIGGER, triggerCron);
  }

  public void putJobDataMap(ClientRequest clientRequest) {
    Map<String, Object> map = new HashMap<>();
    map.put(JobDataMapKey.CLIENT_REQUEST, JsonHelper.toJsonString(clientRequest));
    map.put(JobDataMapKey.CRON_TRIGGER, triggerCron);
    setJobData(JsonHelper.toJson(map));
  }

  public Map<String, Object> getJobDataMap() {
    if (ObjectUtils.isEmpty(jobData)) return new HashMap<>();

    return JsonHelper.fromJson(jobData, new TypeReference<Map<String, Object>>() {});
  }

  public Object getJobDataMap(String key) {
    return Optional.ofNullable(getJobDataMap())
        .map(map -> map.get(key))
        .orElse(null);
  }

}
