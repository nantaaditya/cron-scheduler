package com.nantaaditya.cronscheduler.listener;

import java.util.Map.Entry;
import org.quartz.JobDataMap;

public class BaseListener {

  protected String toString(JobDataMap jobDataMap) {
    StringBuilder sb = new StringBuilder("(");

    int i = 0;
    for (Entry<String, Object> entry : jobDataMap.entrySet()) {
      sb.append(entry.getKey()).append(":").append(entry.getValue());
      if (i != jobDataMap.size() - 1) {
        sb.append(",");
      }
      i++;
    }

    sb.append(")");
    return sb.toString();
  }
}
