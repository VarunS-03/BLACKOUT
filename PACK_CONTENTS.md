# BLACKOUT — Pack Contents

BLACKOUT is a Java 17 Data Structures and Algorithms case study for simulated data-center failure analysis and recovery. The console starts with deterministic sample data; it is not live infrastructure monitoring. The optional browser app launches isolated scenarios and does not show the console's current state.

## Project structure

```text
BLACKOUT/
├── pom.xml
├── README.md
├── PACK_CONTENTS.md
├── docs/
│   ├── DSA_EXPLANATION.md
│   └── MEMBER1_GUIDE.md ... MEMBER4_GUIDE.md
└── src/
    ├── main/
    │   ├── java/blackout/
    │   │   ├── cli/ (Main, DemoEnvironment, StoryRunner)
    │   │   ├── model/ (AuditRecord, Job, RecoveryTask, Server, TelemetryEvent)
    │   │   ├── service/ (AuditLogger, DataCenterManager, FailureAnalysisService,
    │   │   │            RecoveryOrchestrator, RecoveryScheduler, TelemetryManager)
    │   │   ├── structures/ (BinaryHeap, BinarySearchTree, BinaryTree, CircularQueue,
    │   │   │               CustomDeque, CustomHashTable, CustomLinkedList, CustomPriorityQueue,
    │   │   │               CustomQueue, CustomStack, DisjointSetUnion, Graph, LRUCache,
    │   │   │               MerkleTree, SegmentTree, Trie)
    │   │   └── web/ (WebMain, WebDashboardServer)
    │   └── resources/web/ (index.html, app.js, style.css)
    └── test/java/blackout/
        ├── BlackoutWorkflowTest.java
        ├── cli/ (MainConsoleTest, StoryRunnerTest)
        ├── service/ (RecoveryOrchestratorTest, SimulatorServiceTest)
        ├── structures/ (StructureBehaviorTest)
        └── web/ (WebDashboardServerTest)
```

## Main components

| Package or file | Responsibility |
|---|---|
| `blackout.cli.Main` | Interactive console, dashboard, menus, and operations. |
| `DemoEnvironment` | Loads the deterministic healthy sample environment through service APIs. |
| `StoryRunner` | Runs isolated guided scenarios, including critical outage and multi-incident scheduling. |
| `blackout.web.WebMain` | Starts the optional local browser scenario launcher. |
| `WebDashboardServer` and `src/main/resources/web/` | JDK HTTP server and browser assets; scenarios use isolated state. |
| `blackout.model` | Server, job, recovery task, telemetry event, and audit record models. |
| `blackout.service` | Inventory, network/failure analysis, scheduling, telemetry, and audit coordination. |
| `blackout.structures` | The project's implemented lists, queues, trees, graph, DSU, heap, cache, Trie, segment tree, and Merkle tree. |

The test tree contains seven JUnit test classes covering workflows, console interaction, guided scenarios, services, data structures, and browser routes.

## DSA workflow

Server inventory uses a linked list, hash table, BST, and Trie. Network links use an undirected adjacency-list graph; BFS reports reachability and DSU answers connectivity queries. Reported incidents enter a deque; dispatch transfers its front item to a min-heap priority queue ordered by severity, deadline, and task ID. Routine jobs use FIFO queue order. Configuration undo uses a stack, telemetry a bounded circular queue, and load queries a segment tree. Failover plans are represented and displayed as a binary tree; the application does not execute automatic failover. Audit verification rebuilds a Merkle tree from the in-memory log and has no external trusted anchor.

## Requirements and run commands

Requires JDK 17 or newer and Apache Maven. From the project root, run the tests and console with:

```text
mvn test
mvn -q -DskipTests package
java -cp target/classes blackout.cli.Main
```

To start the optional browser scenario launcher:

```text
mvn -q -DskipTests package
java -cp target/blackout-simulator-1.0.0.jar blackout.web.WebMain
```

Open the local URL printed by the process (default `http://127.0.0.1:8765`). The project uses standard JDK runtime functionality; JUnit 5 is test-scoped.

The member guides retain their assigned areas: core management (Member 1), dependency analysis (Member 2), scheduling and telemetry (Member 3), and integrity, caching, searching, and integration (Member 4). See `docs/DSA_EXPLANATION.md` for the detailed implementation guide and demonstration.
