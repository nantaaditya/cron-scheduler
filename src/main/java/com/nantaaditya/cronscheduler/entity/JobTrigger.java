package com.nantaaditya.cronscheduler.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.springframework.data.relational.core.mapping.Table;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@Table(value = "job_trigger")
public class JobTrigger extends BaseEntity {
  private String triggerName;
  private String triggerIdentity;
  private String triggerGroup;
  private String triggerCron;
}
