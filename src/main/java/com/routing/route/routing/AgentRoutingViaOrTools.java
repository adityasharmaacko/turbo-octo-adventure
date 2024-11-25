package com.routing.route.routing;

import com.google.ortools.Loader;
import com.google.ortools.constraintsolver.*;
import com.google.protobuf.Duration;
import java.lang.management.ManagementFactory;
import java.lang.management.OperatingSystemMXBean;
import java.util.*;
import java.util.concurrent.*;
import javax.annotation.PostConstruct;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@Log4j2
public class AgentRoutingViaOrTools {
  
  @Value("${routing.penalty:10000}")
  private int penalty;
  
  @Value("${routing.time_limit_seconds:5}")
  private int timeLimitSeconds;
  
  @Value("${routing.thread_pool_size:4}")
  private int threadPoolSize;
  
  private ExecutorService executorService;
  
  @PostConstruct
  public void init() {
    log.info("Initializing OR-Tools service...");
    this.executorService = Executors.newFixedThreadPool(threadPoolSize);
    Loader.loadNativeLibraries();
    log.info("OR-Tools native libraries successfully loaded.");
  }
  
  public void shutdownExecutorService() {
    if (executorService != null) {
      log.info("Shutting down executor service...");
      executorService.shutdown();
    }
  }
  
  public void validateInput(List<Map<String, Object>> tasks, List<Map<String, Object>> agents) {
    log.info("Validating input data...");
    if (tasks == null || tasks.isEmpty() || agents == null || agents.isEmpty()) {
      throw new IllegalArgumentException("Tasks and agents cannot be empty or null.");
    }
    
    Set<String> requiredTaskKeys = Set.of("id", "skill", "location", "pincode", "duration");
    Set<String> requiredAgentKeys =
        Set.of("id", "skills", "location", "availability", "allowed_locations");
    
    tasks.forEach(
        task -> {
          if (!task.keySet().containsAll(requiredTaskKeys)) {
            throw new IllegalArgumentException("Task " + task + " is missing required fields.");
          }
        });
    
    agents.forEach(
        agent -> {
          if (!agent.keySet().containsAll(requiredAgentKeys)) {
            throw new IllegalArgumentException("Agent " + agent + " is missing required fields.");
          }
        });
    log.info("Input data validation completed successfully.");
  }
  
  public double haversineDistance(double[] loc1, double[] loc2) {
    final double R = 6371.0; // Earth radius in kilometers
    double dLat = Math.toRadians(loc2[0] - loc1[0]);
    double dLon = Math.toRadians(loc2[1] - loc1[1]);
    double a =
        Math.sin(dLat / 2) * Math.sin(dLat / 2)
            + Math.cos(Math.toRadians(loc1[0]))
                  * Math.cos(Math.toRadians(loc2[0]))
                  * Math.sin(dLon / 2)
                  * Math.sin(dLon / 2);
    double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    return R * c;
  }
  
  public double[][] buildDistanceMatrix(List<double[]> locations)
      throws InterruptedException, ExecutionException {
    log.info("Building distance matrix...");
    int size = locations.size();
    List<Future<double[]>> futures = new ArrayList<>();
    
    for (int i = 0; i < size; i++) {
      final int index = i;
      futures.add(
          executorService.submit(
              () -> {
                double[] row = new double[size];
                for (int j = 0; j < size; j++) {
                  row[j] = haversineDistance(locations.get(index), locations.get(j));
                }
                return row;
              }));
    }
    
    double[][] matrix = new double[size][size];
    for (int i = 0; i < size; i++) {
      matrix[i] = futures.get(i).get();
    }
    log.info("Distance matrix built successfully.");
    return matrix;
  }
  
  public Map<String, Object> solveTaskAssignment(
      List<Map<String, Object>> tasks, List<Map<String, Object>> agents)
      throws InterruptedException, ExecutionException {
    
    log.info("Starting task assignment...");
    validateInput(tasks, agents);
    
    List<double[]> locations = new ArrayList<>();
    agents.forEach(agent -> locations.add((double[]) agent.get("location")));
    tasks.forEach(task -> locations.add((double[]) task.get("location")));
    
    double[][] distanceMatrix = buildDistanceMatrix(locations);
    
    int numAgents = agents.size();
    int numLocations = locations.size();
    
    log.info("Initializing OR-Tools model...");
    int[] startNodes = new int[numAgents];
    int[] endNodes = new int[numAgents];
    for (int i = 0; i < numAgents; i++) {
      startNodes[i] = i;
      endNodes[i] = i;
    }
    
    RoutingIndexManager manager =
        new RoutingIndexManager(numLocations, numAgents, startNodes, endNodes);
    RoutingModel routing = new RoutingModel(manager);
    
    routing.setArcCostEvaluatorOfAllVehicles(
        routing.registerTransitCallback(
            (fromIndex, toIndex) -> {
              int fromNode = manager.indexToNode(fromIndex);
              int toNode = manager.indexToNode(toIndex);
              return (int) Math.round(distanceMatrix[fromNode][toNode]);
            }));
    
    int durationCallbackIndex =
        routing.registerUnaryTransitCallback(
            fromIndex -> {
              int fromNode = manager.indexToNode(fromIndex);
              return (fromNode >= numAgents)
                         ? (int) tasks.get(fromNode - numAgents).get("duration")
                         : 0;
            });
    
    long[] availabilities =
        agents.stream().mapToLong(agent -> (int) agent.get("availability")).toArray();
    routing.addDimensionWithVehicleCapacity(
        durationCallbackIndex, 0, availabilities, true, "Availability");
    
    for (int taskIndex = numAgents; taskIndex < numLocations; taskIndex++) {
      Map<String, Object> task = tasks.get(taskIndex - numAgents);
      String taskSkill = (String) task.get("skill");
      int taskPincode = (int) task.get("pincode");
      
      for (int agentIndex = 0; agentIndex < numAgents; agentIndex++) {
        Map<String, Object> agent = agents.get(agentIndex);
        Set<String> agentSkills = (Set<String>) agent.get("skills");
        List<Integer> allowedLocations = (List<Integer>) agent.get("allowed_locations");
        
        if (!agentSkills.contains(taskSkill) || !allowedLocations.contains(taskPincode)) {
          routing.vehicleVar(manager.nodeToIndex(taskIndex)).removeValue(agentIndex);
        }
      }
    }
    
    for (int taskIndex = numAgents; taskIndex < numLocations; taskIndex++) {
      routing.addDisjunction(new long[] {manager.nodeToIndex(taskIndex)}, penalty);
    }
    
    RoutingSearchParameters searchParameters =
        main.defaultRoutingSearchParameters().toBuilder()
            .setFirstSolutionStrategy(FirstSolutionStrategy.Value.PATH_CHEAPEST_ARC)
            .setLocalSearchMetaheuristic(LocalSearchMetaheuristic.Value.GUIDED_LOCAL_SEARCH)
            .setTimeLimit(Duration.newBuilder().setSeconds(timeLimitSeconds).build())
            .build();
    
    // Monitor System Usage During Algorithm Execution
    Thread monitorThread = startMonitoring();
    
    Assignment solution = null;
    try {
      solution = routing.solveWithParameters(searchParameters);
    } finally {
      stopMonitoring(monitorThread);
    }
    
    if (solution == null) {
      log.warn("No solution found.");
      return Map.of(
          "message", "No solution found",
          "agent_assignments", Collections.emptyList(),
          "unassigned_tasks", Collections.emptyList());
    }
    
    List<Map<String, Object>> agentAssignments = new ArrayList<>();
    List<Integer> unassignedTasks = new ArrayList<>();
    double totalDistance = 0;
    
    for (int agentIndex = 0; agentIndex < numAgents; agentIndex++) {
      long index = routing.start(agentIndex);
      List<Integer> assignedTasks = new ArrayList<>();
      double agentDistance = 0;
      double[] lastLocation = null;
      
      while (!routing.isEnd(index)) {
        int node = manager.indexToNode((int) index);
        if (node >= numAgents) {
          assignedTasks.add((int) tasks.get(node - numAgents).get("id"));
          lastLocation = locations.get(node);
        }
        long previousIndex = index;
        index = solution.value(routing.nextVar((int) index));
        if (!routing.isEnd(index)) {
          agentDistance +=
              distanceMatrix[manager.indexToNode((int) previousIndex)][manager.indexToNode((int) index)];
        }
      }
      totalDistance += agentDistance;
      agentAssignments.add(Map.of(
          "agent_id", agentIndex,
          "tasks", assignedTasks,
          "total_distance", agentDistance,
          "last_location", lastLocation));
      log.info("Agent {} assigned tasks: {} with total distance: {}", agentIndex, assignedTasks, agentDistance);
    }
    
    for (int taskIndex = numAgents; taskIndex < numLocations; taskIndex++) {
      if (solution.value(routing.nextVar(manager.nodeToIndex(taskIndex)))
              == manager.nodeToIndex(taskIndex)) {
        unassignedTasks.add((int) tasks.get(taskIndex - numAgents).get("id"));
      }
    }
    
    log.info("Unassigned tasks: {}", unassignedTasks);
    return Map.of(
        "agent_assignments", agentAssignments,
        "unassigned_tasks", unassignedTasks,
        "total_distance_covered", totalDistance);
  }
  
  private Thread startMonitoring() {
    Thread monitorThread = new Thread(() -> {
      OperatingSystemMXBean osBean = ManagementFactory.getOperatingSystemMXBean();
      log.info("Starting system usage monitoring...");
      try {
        while (!Thread.currentThread().isInterrupted()) {
          if (osBean instanceof com.sun.management.OperatingSystemMXBean) {
            com.sun.management.OperatingSystemMXBean extendedOsBean =
                (com.sun.management.OperatingSystemMXBean) osBean;
            log.info(
                "CPU Load: {}%, Free Memory: {} MB",
                Math.round(extendedOsBean.getSystemCpuLoad() * 100),
                extendedOsBean.getFreePhysicalMemorySize() / (1024 * 1024));
          }
          Thread.sleep(1000);
        }
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
      }
    });
    monitorThread.start();
    return monitorThread;
  }
  
  private void stopMonitoring(Thread monitorThread) {
    if (monitorThread != null) {
      monitorThread.interrupt();
      log.info("System usage monitoring stopped.");
    }
  }
}
