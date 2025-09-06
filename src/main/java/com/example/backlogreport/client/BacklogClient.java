package com.example.backlogreport.client;

import com.example.backlogreport.dto.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.time.LocalDate;
import java.util.*;

@Component
public class BacklogClient {
  private final WebClient webClient;
  private final String apiKey;
  private static final Logger log = LoggerFactory.getLogger(BacklogClient.class);

  public BacklogClient(WebClient backlogWebClient, @Value("${backlog.apiKey}") String apiKey) {
    this.webClient = backlogWebClient;
    this.apiKey = apiKey;
  }

  public List<ProjectDTO> getProjects(Boolean archived) {
    return webClient.get()
        .uri(builder -> builder
            .path("/projects")
            .queryParam("apiKey", apiKey)
            .queryParamIfPresent("archived", Optional.ofNullable(archived))
            .build())
        .accept(MediaType.APPLICATION_JSON)
        .retrieve()
        .bodyToFlux(ProjectDTO.class)
        .collectList()
        .block();
  }

  public List<StatusDTO> getProjectStatuses(String projectIdOrKey) {
    return webClient.get()
        .uri(builder -> builder
            .path("/projects/" + projectIdOrKey + "/statuses")
            .queryParam("apiKey", apiKey)
            .build())
        .accept(MediaType.APPLICATION_JSON)
        .retrieve()
        .bodyToFlux(StatusDTO.class)
        .collectList()
        .block();
  }

  /** /issues/count への件数問い合わせ。hasDueDate は送らない（400 になる場合があるため）。 */
  public int countIssues(Long projectId, Long statusId, LocalDate createdSince, LocalDate createdUntil,
                         LocalDate updatedSince, LocalDate updatedUntil,
                         LocalDate dueDateUntil, Boolean hasDueDate /* 受け取るが未使用 */) {
    try {
      return webClient.get()
          .uri(builder -> {
            var b = builder.path("/issues/count").queryParam("apiKey", apiKey);
            if (projectId != null)    b.queryParam("projectId[]", projectId);
            if (statusId != null)     b.queryParam("statusId[]", statusId);
            if (createdSince != null) b.queryParam("createdSince", createdSince.toString());
            if (createdUntil != null) b.queryParam("createdUntil", createdUntil.toString());
            if (updatedSince != null) b.queryParam("updatedSince", updatedSince.toString());
            if (updatedUntil != null) b.queryParam("updatedUntil", updatedUntil.toString());
            if (dueDateUntil != null) b.queryParam("dueDateUntil", dueDateUntil.toString());
            // NOTE: hasDueDate は /issues/count では送らない
            return b.build();
          })
          .accept(MediaType.APPLICATION_JSON)
          .retrieve()
          .bodyToMono(CountResponse.class)
          .map(CountResponse::getCount)
          .blockOptional()
          .orElse(0);
    } catch (WebClientResponseException e) {
      log.warn("issues/count 失敗 status={} body={}", e.getStatusCode(), e.getResponseBodyAsString());
      return 0; // レポートは続行
    } catch (Exception e) {
      log.warn("issues/count 失敗: {}", e.toString());
      return 0;
    }
  }
}
