# BLACKOUT — Data Center Failure and Recovery Simulator

## Overview

**BLACKOUT** is a Java-based Data Center Failure and Recovery Simulator designed to model server failures, analyze service dependencies, prioritize recovery operations, and maintain the integrity of recovery records.

Modern data centers contain interconnected servers and services. When a server fails, multiple dependent services may become unavailable, requiring recovery operations to be prioritized and executed efficiently.

BLACKOUT demonstrates how fundamental and advanced data structures can be applied to these problems through a modular simulation.

## Problem Statement

A data center must respond to server failures while minimizing service disruption and ensuring that recovery operations are handled in an appropriate order.

The simulator addresses the following challenges:

* Representing relationships between servers and dependent services.
* Identifying services affected by a failure.
* Scheduling recovery tasks according to priority.
* Managing server information and operational records efficiently.
* Tracking recent telemetry and operational activity.
* Maintaining the integrity and verifiability of recovery records.

## Key Features

* **Server Failure Simulation:** Models server failures and their potential impact on the data center.
* **Dependency Analysis:** Examines relationships between services to identify affected components.
* **Priority-Based Recovery:** Organizes recovery tasks according to their importance.
* **Server and Service Management:** Supports structured storage and retrieval of operational information.
* **Telemetry Management:** Maintains operational measurements and recent activity.
* **Recovery Record Integrity:** Uses a Merkle Tree to support verification of recorded events.
* **Data-Structure Demonstration:** Applies multiple data structures to different parts of a simulated infrastructure management problem.

> The availability of individual features depends on their implementation in the current codebase.

## How the System Works

The simulator follows a failure-analysis-and-recovery workflow.

### 1. Initialize the Data Center

The system maintains information about servers, services, dependencies, and relevant operational records.

### 2. Simulate a Failure

A server or infrastructure component is marked as failed to initiate the failure-handling process.

### 3. Analyze Dependencies

The system examines the relationships between components to determine which services may be affected by the failure.

### 4. Prioritize Recovery Tasks

Recovery operations are organized according to their assigned priority so that critical tasks can be considered first.

### 5. Execute or Simulate Recovery

The simulator processes the recovery operations supported by the implementation and updates the corresponding server or service state.

### 6. Record and Verify Events

Relevant operational or recovery events can be recorded and processed through the integrity-verification mechanism.

The Merkle Tree allows records to be organized into a hash-based structure, supporting verification that recorded data has not changed relative to a trusted root hash.

## Data Structures and Their Applications

BLACKOUT demonstrates how different data structures address different operational requirements.

| Data Structure           | Application in BLACKOUT                                                                                  |
| ------------------------ | -------------------------------------------------------------------------------------------------------- |
| Linked List              | Maintains collections of records or entities when sequential traversal and dynamic insertion are useful. |
| Queue                    | Processes operations in first-in, first-out order where required.                                        |
| Stack                    | Supports last-in, first-out processing for suitable operations or histories.                             |
| Binary Tree              | Demonstrates hierarchical organization and tree traversal.                                               |
| Binary Search Tree (BST) | Supports ordered storage and searching of suitable records.                                              |
| Heap                     | Maintains elements according to a priority or ordering rule.                                             |
| Priority Queue           | Selects recovery tasks according to their assigned priority.                                             |
| Graph                    | Represents dependencies between servers, services, or infrastructure components.                         |
| Hash Table               | Supports efficient lookup of records using keys.                                                         |
| Trie                     | Supports prefix-based lookup of string identifiers where applicable.                                     |
| Disjoint Set Union (DSU) | Supports grouping and connectivity queries where applicable.                                             |
| Circular Queue           | Manages bounded, continuously updated telemetry or event data where implemented.                         |
| Deque                    | Supports insertion and removal at both ends for applicable workflows.                                    |
| Segment Tree             | Supports efficient range-based queries and updates for suitable numerical data.                          |
| LRU Cache                | Retains recently accessed information and evicts less recently used entries when capacity is reached.    |
| Merkle Tree              | Organizes cryptographic hashes to support integrity verification of records.                             |

**Implementation note:** This table describes the intended applications of the data structures. Each entry should be retained in the final README only when the corresponding implementation can be identified in the source code.

## Merkle Tree and Record Integrity

The Merkle Tree is BLACKOUT's distinctive data structure.

A Merkle Tree organizes data by hashing individual records and then combining child hashes to calculate parent hashes. This process continues until a single root hash represents the tree.

Its principal components are:

* **Leaf Nodes:** Represent hashes of individual records.
* **Internal Nodes:** Represent hashes calculated from their child hashes.
* **Root Hash:** Represents the combined hash structure.
* **Verification:** Compares the relevant hash information to detect inconsistencies in recorded data.

This provides a foundation for checking the integrity of recovery records and identifying modifications to data covered by the tree.

A Merkle Tree does not independently prevent record modification. Its integrity guarantees depend on correct hash construction and comparison against a trusted root or equivalent trusted reference.

## Technologies Used

* **Programming Language:** Java
* **Core Concepts:** Data structures, algorithms, searching, traversal, scheduling, and hashing.
* **Version Control:** Git and GitHub

Additional libraries or tools should be listed here if they are actually used by the project.

## Individual Contributions

### Member 1: GitHub Username — [@karunyabommu](https://github.com/karunyabommu)

**Assigned Components:**

* Linked List
* Queue
* Stack
* Hash Table

**Individual Contribution:**

Responsible for implementing foundational data structures used for sequential data management, FIFO processing, LIFO operations, and key-based data retrieval. Contributed to organizing and managing operational records and supporting efficient data access within the simulator.

### Member 2: GitHub Username — [@akshayakula001-ops](https://github.com/akshayakula001-ops)

**Assigned Components:**

* Graph
* Binary Tree
* Binary Search Tree (BST)
* Disjoint Set Union (DSU)

**Individual Contribution:**

Responsible for implementing data structures used to represent relationships, organize hierarchical information, support ordered searching, and manage connectivity between components. Contributed to the representation of dependencies and connectivity analysis within the simulated data center.

### Member 3: GitHub Username — [@Vinay2744](https://github.com/Vinay2744)

**Assigned Components:**

* Heap
* Priority Queue
* Circular Queue
* Deque
* Segment Tree

**Individual Contribution:**

Responsible for implementing data structures that support priority-based processing, efficient insertion and removal, bounded sequential storage, double-ended operations, and range-based queries and updates. Contributed to recovery task prioritization and efficient management of operational data.

### Member 4: GitHub Username — [@VarunS-03](https://github.com/VarunS-03)

**Assigned Components:**

* Merkle Tree
* LRU Cache
* Trie
* Audit Logger
* System Integration

**Individual Contribution:**

Responsible for implementing the Merkle Tree for record-integrity verification, the LRU Cache for managing recently accessed data, and the Trie for prefix-based searching. Contributed to audit logging, integration of shared models and project modules, and coordination of the individual data structures within the overall simulator.


## How to Run

1. Clone or download the BLACKOUT repository.
2. Open the project in a Java-compatible IDE or terminal.
3. Ensure that a compatible Java Development Kit (JDK) is installed.
4. Compile the project using the build instructions appropriate to its current directory structure.
5. Run the application's entry-point class.
6. Follow the application's prompts or workflow to interact with the simulator.

Exact commands and the entry-point class should be added after confirming the repository's actual project structure.

## Future Improvements

Potential improvements include:

* More detailed failure and recovery scenarios.
* Expanded dependency analysis.
* Additional recovery scheduling policies.
* More extensive integrity-verification tests.
* Improved visualization of server dependencies and recovery progress.

## Academic Context

BLACKOUT is a Data Structures and Algorithms case study demonstrating how data structures can be applied to a simulated infrastructure reliability and recovery problem.
