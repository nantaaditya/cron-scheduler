package com.nantaaditya.cronscheduler.configuration;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PGStringToJsonConverterTest {

  @InjectMocks
  private PGStringToJsonConverter converter;

  @Mock
  private ObjectMapper objectMapper;

  @Test
  @SneakyThrows
  void convert() {
    assertNull(converter.convert(null));

    ObjectMapper mapper = new ObjectMapper();
    JsonNode jsonNode = mapper.readTree("{\"k\":\"v\"}");

    when(objectMapper.writeValueAsString(any(JsonNode.class)))
        .thenReturn("{\"k\":\"v\"}");

    assertNotNull(converter.convert(jsonNode));

    verify(objectMapper).writeValueAsString(any(JsonNode.class));
  }
}