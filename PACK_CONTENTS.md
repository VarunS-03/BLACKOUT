# BLACKOUT — Pack Contents

## 1. Project Overview

**BLACKOUT** is a Java-based Data Center Failure and Recovery Simulator developed as a collaborative Data Structures and Algorithms (DSA) case study.

The project demonstrates how data structures can be applied to infrastructure simulation, dependency analysis, recovery task prioritization, operational data management, and record-integrity verification.

This document describes the project organization, the responsibilities assigned to each team member, and the purpose of the major source-code components.

---

## 2. Team Members and Responsibilities

### Member 1 — Core Data Structures

**GitHub Username:** `karunyabommu`

**Assigned Components:**

* Linked List
* Queue
* Stack
* Hash Table

**Responsibilities:**

* Implement foundational data structures.
* Support sequential data storage and traversal.
* Provide FIFO and LIFO processing where required.
* Support key-based data lookup.
* Contribute to the organization and management of simulator data.

### Member 2 — Dependency and Connectivity Structures

**GitHub Username:** `akshayakula001-ops`

**Assigned Components:**

* Graph
* Binary Tree
* Binary Search Tree (BST)
* Disjoint Set Union (DSU)

**Responsibilities:**

* Represent relationships and dependencies between components.
* Implement hierarchical and ordered data structures.
* Support searching and traversal operations.
* Manage connectivity queries using DSU.
* Contribute to dependency representation and failure analysis.

### Member 3 — Scheduling and Operational Data Structures

**GitHub Username:** `Vinay2744`

**Assigned Components:**

* Heap
* Priority Queue
* Circular Queue
* Deque
* Segment Tree

**Responsibilities:**

* Implement priority-based data processing.
* Support efficient insertion and removal operations.
* Manage bounded sequential storage where appropriate.
* Implement double-ended operations.
* Support range queries and updates where required.
* Contribute to recovery scheduling and operational data management.

### Member 4 — Integrity, Caching, Searching, and Integration

**GitHub Username:** `VarunS-03`

**Assigned Components:**

* Merkle Tree
* LRU Cache
* Trie
* Audit Logger
* System Integration

**Responsibilities:**

* Implement the Merkle Tree for record-integrity verification.
* Implement the LRU Cache for recently accessed data.
* Implement the Trie for prefix-based searching.
* Develop or integrate audit-logging functionality.
* Coordinate integration of shared models and individual modules.
* Help maintain compatibility across the overall Java application.

---

## 3. Project Directory Structure

The project follows a Maven-style Java directory structure.

```text
BLACKOUT/
├── pom.xml
├── README.md
├── PACK_CONTENTS.md
├── docs/
│   ├── DSA_EXPLANATION.md
│   ├── MEMBER1_GUIDE.md
│   ├── MEMBER2_GUIDE.md
│   ├── MEMBER3_GUIDE.md
│   └── MEMBER4_GUIDE.md
└── src/
    ├── main/
    │   └── java/
    │       └── blackout/
    │           ├── model/
    │           ├── structures/
    │           ├── service/
    │           └── cli/
    │               └── Main.java
    └── test/
        └── java/
            └── blackout/
                └── BlackoutWorkflowTest.java
```

### Directory Descriptions

| Path               | Purpose                                                                               |
| ------------------ | ------------------------------------------------------------------------------------- |
| `pom.xml`          | Defines Maven project configuration, dependencies, and build settings.                |
| `README.md`        | Provides a quick project introduction and run instructions.                           |
| `PACK_CONTENTS.md` | Documents the repository structure and assigned responsibilities.                     |
| `docs/`            | Contains supplementary documentation and implementation guides.                       |
| `model/`           | Contains shared models used to represent entities and records within the simulator.   |
| `structures/`      | Contains implementations of the project's data structures.                            |
| `service/`         | Contains application-level logic that coordinates relevant operations.                |
| `cli/`             | Contains the command-line interface and application entry point.                      |
| `Main.java`        | Provides the application's entry point.                                               |

**Note:** The directory tree describes the intended project organization. Individual source files should be listed below only after confirming that they exist in the repository.

---

## 4. Shared Components

Shared components provide common functionality used by multiple parts of the application.

### Model Package

The `blackout.model` package contains shared data models.

Depending on the current implementation, these may include:

* `AuditRecord` — Represents an audit record.
* `Job` — Represents a job or unit of work.
* `RecoveryTask` — Represents a recovery operation.
* `Server` — Represents a server in the simulated environment.
* `TelemetryEvent` — Represents an operational telemetry event.

These models allow different modules to exchange structured information without duplicating entity definitions.

### Service Package

The `blackout.service` package contains application-level logic.

Services coordinate data structures and models to implement relevant simulator operations, such as dependency analysis, recovery processing, or audit logging.

The exact service classes and their responsibilities should correspond to the source files present in the repository.

### Command-Line Interface

The `blackout.cli` package contains the application entry point and command-line interaction.

`Main.java` is responsible for starting the application and connecting the supported user interaction flow to the relevant application components.

---

## 5. Data Structures by Team Member

| Member   | Data Structure or Component | Primary Purpose                                            |
| -------- | --------------------------- | ---------------------------------------------------------- |
| Member 1 | Linked List                 | Dynamic sequential storage and traversal.                  |
| Member 1 | Queue                       | FIFO processing.                                           |
| Member 1 | Stack                       | LIFO processing.                                           |
| Member 1 | Hash Table                  | Key-based data retrieval.                                  |
| Member 2 | Graph                       | Representing relationships and dependencies.               |
| Member 2 | Binary Tree                 | Hierarchical data organization.                            |
| Member 2 | Binary Search Tree          | Ordered storage and searching.                             |
| Member 2 | Disjoint Set Union          | Connectivity and set-membership queries.                   |
| Member 3 | Heap                        | Maintaining partial ordering for efficient element access. |
| Member 3 | Priority Queue              | Processing elements according to priority.                 |
| Member 3 | Circular Queue              | Bounded FIFO storage with reusable positions.              |
| Member 3 | Deque                       | Insertion and removal at both ends.                        |
| Member 3 | Segment Tree                | Efficient range queries and updates.                       |
| Member 4 | Merkle Tree                 | Hash-based record-integrity verification.                  |
| Member 4 | LRU Cache                   | Caching recently accessed entries.                         |
| Member 4 | Trie                        | Prefix-based string searching.                             |
| Member 4 | Audit Logger                | Recording relevant application events.                     |
| Member 4 | System Integration          | Coordinating shared components and application modules.    |

---

## 6. Application Workflow

The user registers servers and their network links, reports an outage, dispatches the incident into the recovery priority queue, and processes a recovery task. Separate menu choices demonstrate routine jobs, configuration undo, telemetry and load analysis, failover planning, and audit verification.

---

## 7. Build and Execution

The project uses Maven for Java project management and compilation.

### Prerequisites

* A compatible Java Development Kit (JDK).
* Apache Maven.
* Access to the BLACKOUT project source code.

### Compile the Project

From the project root directory, run:

```bash
mvn test
```

This compiles the project and runs the JUnit tests.

### Run the Application

Build and run the terminal application with:

```bash
mvn -q -DskipTests package
java -cp target/classes blackout.cli.Main
```

Use the numbered menu to interact with the data-center scenario. Enter `0` in a submenu to return to the main menu, or at the main menu to exit. See `docs/DSA_EXPLANATION.md` for the implementation guide and demonstration sequence.

---

## 8. Version Control and Collaboration

The project is maintained using Git and GitHub.

Each team member is responsible for their assigned components and should contribute through the agreed branch and pull-request workflow.

The collaborative workflow consists of:

1. Working on the assigned components.
2. Committing changes to the appropriate feature branch.
3. Pushing the branch to the remote repository.
4. Opening a pull request for review.
5. Checking compatibility with the shared models and existing modules.
6. Merging approved changes into the main branch.

Shared files should be modified carefully to avoid conflicting implementations or breaking compatibility between team members' modules.

---

## 9. Implementation and Documentation Guidelines

* Keep data-structure implementations organized within the appropriate package.
* Reuse shared models instead of introducing duplicate definitions.
* Maintain compatibility with the project's Maven configuration.
* Document public methods and important algorithmic decisions where appropriate.
* Test relevant operations and edge cases.
* Update this document when source files, responsibilities, or directory organization change.
* Describe functionality according to the actual implementation rather than planned features.

---

## 10. Academic Purpose

BLACKOUT demonstrates the application of Data Structures and Algorithms to a simulated infrastructure reliability problem.

The project emphasizes:

* Data organization and retrieval.
* Tree traversal and ordered searching.
* Graph-based relationship representation.
* Priority-based processing.
* Queue and deque operations.
* Range-based data processing.
* Caching and prefix searching.
* Hash-based record-integrity verification.
* Collaborative software development and version control.

The individual contributions document each member's assigned responsibilities, while the shared project structure supports integration into a single Java application.
