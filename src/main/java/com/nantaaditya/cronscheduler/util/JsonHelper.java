package com.nantaaditya.cronscheduler.util;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.r2dbc.postgresql.codec.Json;
import java.util.Map;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.util.StringUtils;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class JsonHelper {

  private final static ObjectMapper objectMapper = new ObjectMapper();

  static {
    objectMapper.registerModule(new JavaTimeModule());
  }

  @SneakyThrows
  public static Json toJson(Object object) {
    if (object == null) return null;
    String jsonString = objectMapper.writeValueAsString(object);
    return Json.of(jsonString);
  }

  @SneakyThrows
  public static Json toJson(Map<String, Object> json) {
    if (json == null || json.isEmpty()) return null;
    String jsonString = objectMapper.writeValueAsString(json);
    return Json.of(jsonString);
  }

  @SneakyThrows
  public static String toJsonString(Object object) {
    if (object == null) return null;
    return objectMapper.writeValueAsString(object);
  }

  @SneakyThrows
  public static Map<String, String> fromJson(Json json) {
    return fromJson(json, new TypeReference<Map<String, String>>() {});
  }

  @SneakyThrows
  public static <T> T fromJson(Json json, TypeReference<T> typeReference) {
    if (json == null) return null;
    String jsonString = json.asString();
    return objectMapper.readValue(jsonString, typeReference);
  }

  @SneakyThrows
  public static <T> T fromJson(String json, Class<T> modelClass) {
    if (!StringUtils.hasLength(json)) return null;
    return objectMapper.readValue(json, modelClass);
  }
}
