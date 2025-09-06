package com.example.backlogreport.client;

import com.example.backlogreport.dto.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.LocalDate;
import java.util.*;

@Component
public class BacklogClient {
  private final WebClient webClient;
  private final String apiKey;

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

  public int countIssues(Long projectId, Long statusId, LocalDate createdSince, LocalDate createdUntil,
                         LocalDate updatedSince, LocalDate updatedUntil,
                         LocalDate dueDateUntil, Boolean hasDueDate) {

    return webClient.get()
        .uri(builder -> {
          var b = builder.path("/issues/count").queryParam("apiKey", apiKey);
          if (projectId != null) b.queryParam("projectId[]", projectId);
          if (statusId != null)  b.queryParam("statusId[]", statusId);
          if (createdSince != null) b.queryParam("createdSince", createdSince.toString());
          if (createdUntil != null) b.queryParam("createdUntil", createdUntil.toString());
          if (updatedSince != null) b.queryParam("updatedSince", updatedSince.toString());
          if (updatedUntil != null) b.queryParam("updatedUntil", updatedUntil.toString());
          if (dueDateUntil != null) b.queryParam("dueDateUntil", dueDateUntil.toString());
          if (hasDueDate != null) b.queryParam("hasDueDate", hasDueDate);
          return b.build();
        })
        .accept(MediaType.APPLICATION_JSON)
        .retrieve()
        .bodyToMono(CountResponse.class)
        .map(CountResponse::getCount)
        .blockOptional()
        .orElse(0);
  }
}