package com.nantaaditya.cronscheduler.model.dto;

import com.nantaaditya.cronscheduler.entity.ClientRequest;
import com.nantaaditya.cronscheduler.entity.JobHistory;

public record JobResponse(
    JobHistory jobHistory,
    EventContext eventContext,
    ClientRequest clientRequest,
    String responseBody
) {

}
