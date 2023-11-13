package com.nantaaditya.cronscheduler.configuration;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.r2dbc.postgresql.codec.Json;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.WritingConverter;

@Slf4j
@WritingConverter
@RequiredArgsConstructor
public class PGStringToJsonConverter implements Converter<JsonNode, Json> {

  private final ObjectMapper objectMapper;

  @Override
  public Json convert(JsonNode source) {
    if (source == null) return null;

    Json response = null;
    try {
      response = Json.of(objectMapper.writeValueAsString(source));
    } catch (JsonProcessingException e) {
      log.error("#POSTGRES - error parsing JsonNode to database column", e);
    }
    return response;
  }

}
