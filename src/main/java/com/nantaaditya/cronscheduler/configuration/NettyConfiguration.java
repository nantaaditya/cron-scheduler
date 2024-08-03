package com.nantaaditya.cronscheduler.configuration;

import com.nantaaditya.cronscheduler.listener.WebClientJobListener;
import com.nantaaditya.cronscheduler.util.IdGenerator;
import io.micrometer.tracing.internal.EncodingUtils;
import java.util.Optional;
import org.springframework.boot.web.embedded.netty.NettyReactiveWebServerFactory;
import org.springframework.boot.web.embedded.netty.NettyServerCustomizer;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;
import org.zalando.logbook.HttpRequest;
import org.zalando.logbook.Logbook;
import org.zalando.logbook.LogbookCreator;
import org.zalando.logbook.core.DefaultHttpLogFormatter;
import org.zalando.logbook.core.DefaultHttpLogWriter;
import org.zalando.logbook.core.DefaultSink;
import org.zalando.logbook.json.JsonHttpLogFormatter;
import org.zalando.logbook.netty.LogbookServerHandler;
import reactor.netty.http.server.HttpServer;

@Component
public class NettyConfiguration implements WebServerFactoryCustomizer<NettyReactiveWebServerFactory> {

  @Bean
  @Primary
  public Logbook logbook() {
    return LogbookCreator.builder()
        .correlationId(NettyConfiguration::composeCorrelationId)
        .sink(new DefaultSink(new DefaultHttpLogFormatter(), new DefaultHttpLogWriter()))
        .build();
  }

  private static String composeCorrelationId(HttpRequest request) {
    return Optional.ofNullable(request)
        .map(HttpRequest::getHeaders)
        .map(httpHeaders -> httpHeaders.getFirst(WebClientJobListener.TRACE_ID_HEADER))
        .orElseGet(() -> EncodingUtils.fromLong(IdGenerator.createLongId()));
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
