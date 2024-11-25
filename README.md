🚀 **Turbo Octo Adventure: Agent Routing via OR-Tools**
=======================================================

**Overview**
------------

Welcome to **Turbo Octo Adventure**, a proof-of-concept (POC) demonstrating the power of **Google OR-Tools** for solving complex **routing** and **task assignment** problems. This POC efficiently allocates tasks to agents by considering **skills**, **location**, and **availability** to ensure optimal task assignments with minimal travel.

Additionally, the system tracks **CPU** and **memory usage**, providing valuable insights into performance during the routing process. It is more than just a routing tool---it's a showcase of how **AI-driven optimization** can enhance operational efficiency.

* * * * *

**Key Feature**
----------------

-   **Smart Task Assignment**: Automatically assigns tasks based on agent skills, locations, and availability, minimizing travel distance.
-   **Unassigned Task Tracking**: Identifies tasks that couldn't be assigned, highlighting any gaps in your routing logic.
-   **System Performance Monitoring**: Tracks and logs CPU and memory usage during the routing process for better resource management.
-   **Scalable Architecture**: Supports multithreading for faster computation and scalable execution, perfect for growing needs.

* * * * *

**How It Works**
----------------

### **Input Data**

-   **Agents**: A list of agents with details such as skills, location, availability, and task authorization.
-   **Tasks**: A list of tasks requiring specific skills, location, and estimated duration.

### **Processing Steps**

1.  **Distance Matrix Calculation**: Computes the travel distances between agents and tasks using the **Haversine formula**, ensuring accurate estimates.
2.  **Routing Optimization**: Uses Google OR-Tools to:
    -   Minimize travel distances while factoring in agent skills, availability, and task requirements.
    -   Assign tasks to the most suitable agents.
3.  **Performance Monitoring**: Logs CPU and memory usage to track system health during execution.

### **Output Data**

-   **Agent Assignments**: Details of tasks assigned to each agent, including total distance covered and final task locations.
-   **Unassigned Tasks**: A list of tasks that couldn't be assigned due to constraints.
-   **System Usage Logs**: Logs of CPU and memory usage during execution for performance analysis.

* * * * *

**Project Structure**
---------------------

```
src/
├── main/
│   ├── java/
│   │   └── com.routing.route.routing/
│   │       └── AgentRoutingViaOrTools.java  # Core task routing logic
│   └── resources/
│       └── application.properties           # Configuration settings

```

* * * * *

**Setup Instructions**
----------------------

### **Prerequisites**

1.  **Java Development Kit (JDK) 11+**
2.  **Maven** for building the project.
3.  **Podman** (or Docker) for containerized execution.

### **Clone the Repository**

Clone the project to your local machine:

```
git clone https://github.com/adityasharmaacko/turbo-octo-adventure.git
cd turbo-octo-adventure

```

### **Configure the Application**

Update the `application.properties` file with your routing settings:

```
routing.penalty=10000
routing.time_limit_seconds=5
routing.thread_pool_size=4

```

### **Build the Project**

Compile the project using Maven:

```
mvn clean install

```

### **Run the Application**

Start the application:

```
java -jar target/route-0.0.1-SNAPSHOT.jar

```

* * * * *

**Run with Podman**
-------------------

### **Install Podman**

Install Podman following the [official guide](https://podman.io/getting-started/installation).

### **Build the Docker Image**

Build the container image for the app:

```
podman build -t route-app:latest .

```

### **Run the Container**

Run the containerized application:

```
podman run -d --name route-app -p 8080:8080 route-app:latest

```

* * * * *

**Example Input**
-----------------

### **Agents**

```
[
    {
      "id": 0,
      "skills": ["driver"],
      "location": [12.914142, 74.856033],
      "availability": 120,
      "allowedLocations": [560001, 560002]
    },
    {
      "id": 1,
      "skills": ["inspection"],
      "location": [12.914142, 74.856033],
      "availability": 150,
      "allowedLocations": [560002]
    }
]

```

### **Tasks**

```
[
    {
      "id": 0,
      "skill": "driver",
      "location": [12.971598, 77.594566],
      "pincode": 560001,
      "duration": 30
    },
    {
      "id": 1,
      "skill": "driver",
      "location": [12.295810, 76.639381],
      "pincode": 560002,
      "duration": 45
    },
    {
      "id": 2,
      "skill": "inspection",
      "location": [13.082680, 80.270721],
      "pincode": 560002,
      "duration": 60
    }
]

```

* * * * *

**Example Output**
------------------

### **Response**

```
{
    "total_distance_covered": 920.33,
    "agent_assignments": [
        {
            "total_distance": 333.38,
            "last_location": [12.971598, 77.594566],
            "tasks": [1, 0],
            "agent_id": 0
        },
        {
            "total_distance": 586.95,
            "last_location": [13.08268, 80.270721],
            "tasks": [2],
            "agent_id": 1
        }
    ],
    "unassigned_tasks": []
}

```

### **Logs**

```
INFO: Validating input data...
INFO: Building distance matrix...
INFO: Distance matrix built successfully.
INFO: Starting task assignment...
INFO: Agent 1 assigned tasks: [101] with total distance: 1.23
INFO: Unassigned tasks: []
INFO: CPU Load: 25%, Free Memory: 512 MB

```

* * * * *

**Resources**
-------------

-   [Google OR-Tools Documentation](https://developers.google.com/optimization)
-   [Spring Framework Documentation](https://spring.io/projects/spring-framework)
-   [Haversine Formula](https://en.wikipedia.org/wiki/Haversine_formula)

* * * * *

**Why This POC Matters**
------------------------

1.  **Practical Use Cases**: Ideal for delivery routing, field worker assignments, and real-time task allocation.
2.  **Optimization with AI**: Demonstrates how AI can optimize routing and task assignments for increased efficiency.
3.  **Real-Time Monitoring**: Tracks system performance, allowing for real-time adjustments during execution.
4.  **Scalability**: Built to handle large-scale routing problems, making it ready for production environments.

Ready to optimize your task assignments and improve operational efficiency? 🚀 Check out the [Turbo Octo Adventure repo](https://github.com/adityasharmaacko/turbo-octo-adventure) and get started today!