package com.example.backlogreport.service;

import com.example.backlogreport.client.BacklogClient;
import com.example.backlogreport.dto.ProjectDTO;
import com.example.backlogreport.dto.StatusDTO;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ReportService {
  private final BacklogClient client;
  private final Set<String> closedNames;

  public ReportService(BacklogClient client, @Value("${backlog.closedStatusNames}") String closedStatusNames) {
    this.client = client;
    this.closedNames = Arrays.stream(closedStatusNames.split(","))
        .map(String::trim).filter(s -> !s.isEmpty())
        .map(String::toLowerCase)
        .collect(Collectors.toSet());
  }

  public ReportData buildReport(List<String> projectKeys, LocalDate since, LocalDate until) {
    // アーカイブ含む全件
    List<ProjectDTO> all = Optional.ofNullable(client.getProjects(null)).orElseGet(List::of);
    List<ProjectDTO> projects = (projectKeys == null || projectKeys.isEmpty())
        ? all
        : all.stream().filter(p -> projectKeys.contains(p.getProjectKey())).toList();

    List<ProjectRow> rows = new ArrayList<>();

    for (ProjectDTO p : projects) {
      List<StatusDTO> statuses = Optional.ofNullable(client.getProjectStatuses(p.getProjectKey()))
          .orElseGet(List::of);

      Map<String,Integer> byStatus = new LinkedHashMap<>();
      int openTotal = 0;
      int closedTotal = 0;

      for (StatusDTO st : statuses) {
        int c = client.countIssues(p.getId(), st.getId(), null, null, null, null, null, null);
        byStatus.put(st.getName(), c);
        if (closedNames.contains(st.getName().toLowerCase())) closedTotal += c; else openTotal += c;
      }

      int createdInWindow = 0;
      if (since != null || until != null) {
        createdInWindow = client.countIssues(p.getId(), null, since, until, null, null, null, null);
      }

      int overdue = 0;
      for (StatusDTO st : statuses) {
        if (closedNames.contains(st.getName().toLowerCase())) continue;
        int c = client.countIssues(p.getId(), st.getId(), null, null, null, null, LocalDate.now(), null /* hasDueDate unused */);
        overdue += c;
      }

      rows.add(new ProjectRow(p.getProjectKey(), p.getName(), byStatus, openTotal, closedTotal, createdInWindow, overdue));
    }

    return new ReportData(rows, since, until);
  }

  // ===== Thymeleaf 用の getter =====
  public static class ReportData {
    private final List<ProjectRow> rows;
    private final LocalDate since;
    private final LocalDate until;
    public ReportData(List<ProjectRow> rows, LocalDate since, LocalDate until) {
      this.rows = rows; this.since = since; this.until = until;
    }
    public List<ProjectRow> getRows() { return rows; }
    public LocalDate getSince() { return since; }
    public LocalDate getUntil() { return until; }
  }

  public static class ProjectRow {
    private final String projectKey;
    private final String projectName;
    private final Map<String,Integer> byStatus;
    private final int openTotal;
    private final int closedTotal;
    private final int createdInWindow;
    private final int overdue;
    public ProjectRow(String key, String name, Map<String,Integer> byStatus, int openTotal, int closedTotal, int createdInWindow, int overdue) {
      this.projectKey = key; this.projectName = name; this.byStatus = byStatus; this.openTotal = openTotal; this.closedTotal = closedTotal; this.createdInWindow = createdInWindow; this.overdue = overdue;
    }
    public String getProjectKey() { return projectKey; }
    public String getProjectName() { return projectName; }
    public Map<String,Integer> getByStatus() { return byStatus; }
    public int getOpenTotal() { return openTotal; }
    public int getClosedTotal() { return closedTotal; }
    public int getCreatedInWindow() { return createdInWindow; }
    public int getOverdue() { return overdue; }
  }
}
