package com.nantaaditya.cronscheduler.model.dto;

import com.nantaaditya.cronscheduler.entity.ClientRequest;

public record NotificationCallbackDTO(
    String jobExecutorId,
    String cronTrigger,
    ClientRequest clientRequest,
    String response
) {

}
