# BLACKOUT

BLACKOUT is a Java 17 Data Structures and Algorithms case study that simulates data-center failure analysis and recovery. Its deterministic sample environment is for demonstration and does not represent live infrastructure.

## Features

- Register servers and search inventory by ID, name prefix, registration order, or sorted ID.
- Model undirected network links with an adjacency-list graph; use BFS for reachability and DSU for connectivity checks.
- Report incidents through a deque and dispatch recovery tasks through a min-heap priority queue.
- Process routine jobs FIFO, undo configuration changes with a stack, buffer telemetry in a circular queue, and query/update load samples with a segment tree.
- Store and display a primary-plus-backups failover plan; the plan does not automatically switch servers.
- Inspect audit records and verify candidates against a Merkle root rebuilt from the current in-memory log.
- View the network topology and run two isolated guided simulations from the console.

- 
[![Architecture diagram of varuns-03/blackout](https://gitdiagram.com/varuns-03/blackout/diagram.png)](https://gitdiagram.com/varuns-03/blackout?utm_source=readme&utm_medium=picture)
## Requirements and console

Requires JDK 17 or newer and Apache Maven. From the project root:

```text
mvn test
mvn -q -DskipTests package
java -cp target/classes blackout.cli.Main
```

The console starts with sample data. Its dashboard is simulated state, not live monitoring. Use the numbered menus; `0` returns from a submenu or exits from the main menu.

## Optional browser scenario launcher

Build the JAR and start the JDK HTTP server:

```text
mvn -q -DskipTests package
java -cp target/blackout-simulator-1.0.0.jar blackout.web.WebMain
```

Open the local URL printed by the process (default `http://127.0.0.1:8765`). This browser interface launches predefined scenarios using separate state; it does not show the console's current seeded state or live infrastructure.

See [docs/DSA_EXPLANATION.md](docs/DSA_EXPLANATION.md) for implementation details, complexity, and demonstrations.
