package com.routing.route.routing;

import java.util.List;
import java.util.Set;
import lombok.Data;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Data
@RequiredArgsConstructor
public class ORToolsAssignmentRequest {

  private List<TaskRequest> tasks;
  private List<AgentRequest> agents;

  @Data
  @Getter
  public static class TaskRequest {
    private int id;
    private String skill;
    private double[] location; // [latitude, longitude]
    private int pincode;
    private int duration; // Task duration in minutes
  }

  @Data
  @Getter
  public static class AgentRequest {
    private int id;
    private Set<String> skills;
    private double[] location; // [latitude, longitude]
    private int availability; // Max minutes available
    private List<Integer> allowedLocations; // List of allowed pincodes
  }
}
