package com.routing.route.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.routing.route.routing.AgentRoutingViaOrTools;
import com.routing.route.routing.ORToolsAssignmentRequest;
import com.routing.route.routing.ORToolsAssignmentRequest.AgentRequest;
import com.routing.route.routing.ORToolsAssignmentRequest.TaskRequest;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/tasks")
@RequiredArgsConstructor
@Log4j2
public class TaskAssignmentController {

  private final AgentRoutingViaOrTools routingService;
  private final ObjectMapper mapper;

  @PostMapping("/assign-tasks")
  public ResponseEntity<Map<String, Object>> assignTasks(
      @RequestBody ORToolsAssignmentRequest request) {

    try {
      // Extract tasks and agents
      List<TaskRequest> tasks = request.getTasks();
      List<AgentRequest> agents = request.getAgents();

      // Convert DTOs to Map format for OR-Tools
      List<Map<String, Object>> taskMaps =
          tasks.stream()
              .map(
                  task ->
                      Map.of(
                          "id", (Object) task.getId(),
                          "skill", (Object) task.getSkill(),
                          "location", (Object) task.getLocation(),
                          "pincode", (Object) task.getPincode(),
                          "duration", (Object) task.getDuration()))
              .collect(Collectors.toList());

      List<Map<String, Object>> agentMaps =
          agents.stream()
              .map(
                  agent ->
                      Map.of(
                          "id", agent.getId(),
                          "skills", agent.getSkills(),
                          "location", agent.getLocation(),
                          "availability", agent.getAvailability(),
                          "allowed_locations", agent.getAllowedLocations()))
              .collect(Collectors.toList());

      // Call the service
      Map<String, Object> result = routingService.solveTaskAssignment(taskMaps, agentMaps);

      // Return the response
      return ResponseEntity.ok(result);

    } catch (Exception e) {
      log.error("Error during task assignment: ", e);
      return null;
    }
  }
}
