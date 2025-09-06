package com.example.backlogreport.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

@Configuration
public class BacklogConfig {
  @Value("${backlog.host}")
  private String host; // e.g. yourspace.backlog.com

  @Bean
  public WebClient backlogWebClient() {
    String base = "https://" + host + "/api/v2";
    HttpClient http = HttpClient.create();
    return WebClient.builder()
        .baseUrl(base)
        .clientConnector(new ReactorClientHttpConnector(http))
        .build();
  }
}