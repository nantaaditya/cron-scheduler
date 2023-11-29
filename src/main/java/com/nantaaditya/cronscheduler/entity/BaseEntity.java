package com.nantaaditya.cronscheduler.entity;

import com.github.f4b6a3.tsid.TsidCreator;
import java.time.LocalDate;
import java.time.LocalTime;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.annotation.Version;

@Data
@NoArgsConstructor
@SuperBuilder
@AllArgsConstructor
public class BaseEntity {

  public static final String AUDITOR = "SYSTEM";

  @Id
  private String id;

  @CreatedBy
  private String createdBy;

  @CreatedDate
  private LocalDate createdDate;

  @CreatedDate
  private LocalTime createdTime;

  @LastModifiedBy
  private String modifiedBy;

  @LastModifiedDate
  private LocalDate modifiedDate;

  @LastModifiedDate
  private LocalTime modifiedTime;

  @Version
  private long version;

  public static String generateId() {
    return TsidCreator.getTsid1024().toLowerCase();
  }
}
