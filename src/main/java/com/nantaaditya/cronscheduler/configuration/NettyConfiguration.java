package com.nantaaditya.cronscheduler.configuration;

import org.springframework.boot.web.embedded.netty.NettyReactiveWebServerFactory;
import org.springframework.boot.web.embedded.netty.NettyServerCustomizer;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;
import org.zalando.logbook.Logbook;
import org.zalando.logbook.LogbookCreator;
import org.zalando.logbook.core.Conditions;
import org.zalando.logbook.core.DefaultHttpLogWriter;
import org.zalando.logbook.core.DefaultSink;
import org.zalando.logbook.json.JsonHttpLogFormatter;
import org.zalando.logbook.netty.LogbookServerHandler;
import reactor.netty.http.server.HttpServer;

@Component
public class NettyConfiguration implements WebServerFactoryCustomizer<NettyReactiveWebServerFactory> {

  @Bean
  public Logbook logbook() {
    return LogbookCreator.builder()
        .condition(Conditions.requestTo("/api/**"))
        .sink(new DefaultSink(new JsonHttpLogFormatter(), new DefaultHttpLogWriter()))
        .build();
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
