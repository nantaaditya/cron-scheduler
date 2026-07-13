package com.nantaaditya.cronscheduler.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.fasterxml.jackson.core.type.TypeReference;
import io.r2dbc.postgresql.codec.Json;
import java.util.Map;
import org.junit.jupiter.api.Test;

class JsonHelperTest {

  @Test
  void toJsonStringReturnsNullForNullObject() {
    assertNull(JsonHelper.toJsonString(null));
  }

  @Test
  void toJsonStringSerializesObject() {
    assertEquals("{\"k\":\"v\"}", JsonHelper.toJsonString(Map.of("k", "v")));
  }

  @Test
  void toJsonReturnsNullForNullObject() {
    assertNull(JsonHelper.toJson(null));
  }

  @Test
  void toJsonWrapsSerializedObject() {
    Json json = JsonHelper.toJson(Map.of("k", "v"));
    assertEquals("{\"k\":\"v\"}", json.asString());
  }

  @Test
  void fromJsonStringReturnsNullForNullOrBlankString() {
    Json json = null;
    assertNull(JsonHelper.fromJson(json, new TypeReference<Map<String, Object>>() {}));
  }

  @Test
  void fromJsonStringParsesValidJson() {
    Json json = Json.of("{\"k\":\"v\"}");
    Map<String, Object> result = JsonHelper.fromJson(json, new TypeReference<Map<String, Object>>() {});
    assertEquals(Map.of("k", "v"), result);
  }

  @Test
  void fromJsonJsonReturnsNullForNullOrBlankJson() {
    assertNull(JsonHelper.fromJson((Json) null, new TypeReference<Map<String, Object>>() {}));
    assertNull(JsonHelper.fromJson(Json.of(""), new TypeReference<Map<String, Object>>() {}));
  }

  @Test
  void fromJsonJsonParsesValidJson() {
    Map<String, Object> result = JsonHelper.fromJson(Json.of("{\"k\":\"v\"}"), new TypeReference<Map<String, Object>>() {});
    assertEquals(Map.of("k", "v"), result);
  }

}
