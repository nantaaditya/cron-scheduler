package com.nantaaditya.cronscheduler.entity;

import com.nantaaditya.cronscheduler.util.JsonHelper;
import io.r2dbc.postgresql.codec.Json;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.springframework.data.relational.core.mapping.Table;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@Table(value = "job_history_detail")
public class JobHistoryDetail extends BaseEntity{
  private String jobHistoryId;
  private Json clientData;
  private Json jobTrigger;
  private Json resultDetail;

  public static JobHistoryDetail of(String jobHistoryId, ClientRequest clientRequest,
      JobTrigger jobTrigger, Json resultDetail) {
    return JobHistoryDetail.builder()
        .id(BaseEntity.generateId())
        .createdBy(BaseEntity.AUDITOR)
        .modifiedBy(BaseEntity.AUDITOR)
        .jobHistoryId(jobHistoryId)
        .clientData(JsonHelper.toJson(clientRequest))
        .jobTrigger(JsonHelper.toJson(jobTrigger))
        .resultDetail(resultDetail)
        .build();
  }
}
