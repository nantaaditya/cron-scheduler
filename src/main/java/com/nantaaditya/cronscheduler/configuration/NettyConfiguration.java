package com.nantaaditya.cronscheduler.configuration;

import com.nantaaditya.cronscheduler.listener.WebClientJobListener;
import com.nantaaditya.cronscheduler.util.IdGenerator;
import com.nantaaditya.cronscheduler.util.TracerHelper;
import io.micrometer.tracing.internal.EncodingUtils;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.web.embedded.netty.NettyReactiveWebServerFactory;
import org.springframework.boot.web.embedded.netty.NettyServerCustomizer;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;
import org.zalando.logbook.HttpMessage;
import org.zalando.logbook.HttpRequest;
import org.zalando.logbook.Logbook;
import org.zalando.logbook.LogbookCreator;
import org.zalando.logbook.core.DefaultHttpLogFormatter;
import org.zalando.logbook.core.DefaultHttpLogWriter;
import org.zalando.logbook.core.DefaultSink;
import org.zalando.logbook.netty.LogbookServerHandler;
import reactor.netty.http.server.HttpServer;

@Component
@RequiredArgsConstructor
public class NettyConfiguration implements WebServerFactoryCustomizer<NettyReactiveWebServerFactory> {

  private final TracerHelper tracerHelper;

  @Bean
  public Logbook logbook() {
    return LogbookCreator.builder()
        .correlationId(this::composeCorrelationId)
        .sink(new DefaultSink(new DefaultHttpLogFormatter(), new DefaultHttpLogWriter()))
        .build();
  }

  private String composeCorrelationId(HttpRequest request) {
    String correlationId = Optional.ofNullable(request)
        .map(HttpRequest::getHeaders)
        .map(httpHeaders -> httpHeaders.getFirst(WebClientJobListener.TRACE_ID_HEADER))
        .orElseGet(() -> EncodingUtils.fromLong(IdGenerator.createLongId()));
    tracerHelper.setBaggage("reqId", correlationId);

    Optional.ofNullable(request)
        .map(HttpMessage::getHeaders)
        .ifPresent(httpHeaders -> {
          tracerHelper.setBaggage("traceId", httpHeaders.getFirst(WebClientJobListener.TRACE_ID_HEADER));
          tracerHelper.setBaggage("spanId", httpHeaders.getFirst(WebClientJobListener.SPAN_ID_HEADER));
        });
    return correlationId;
  }

  @Override
  public void customize(NettyReactiveWebServerFactory factory) {
    factory.addServerCustomizers(new EventLoopNettyCustomizer());
  }

  class EventLoopNettyCustomizer implements NettyServerCustomizer {

    @Override
    public HttpServer apply(HttpServer httpServer) {
      return httpServer
          .doOnConnection(
              connection -> connection.addHandlerLast(new LogbookServerHandler(logbook()))
          );
    }
  }
}
