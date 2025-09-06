package com.example.backlogreport.dto;

public class StatusDTO {
  private long id;
  private long projectId;
  private String name;

  public long getId() { return id; }
  public void setId(long id) { this.id = id; }
  public long getProjectId() { return projectId; }
  public void setProjectId(long projectId) { this.projectId = projectId; }
  public String getName() { return name; }
  public void setName(String name) { this.name = name; }
}