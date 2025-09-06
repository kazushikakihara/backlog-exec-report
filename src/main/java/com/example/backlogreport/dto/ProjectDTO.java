package com.example.backlogreport.dto;

public class ProjectDTO {
  private long id;
  private String projectKey;
  private String name;

  public long getId() { return id; }
  public void setId(long id) { this.id = id; }
  public String getProjectKey() { return projectKey; }
  public void setProjectKey(String projectKey) { this.projectKey = projectKey; }
  public String getName() { return name; }
  public void setName(String name) { this.name = name; }
}