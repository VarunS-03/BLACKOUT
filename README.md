# BLACKOUT

BLACKOUT is a terminal-based Java case study that simulates data-center outages and recovery. Users register servers, connect them, report failures, dispatch incidents, process recovery work, and inspect telemetry and audit records.

## Scenario and DSA structures

The project uses sixteen structures in real scenario operations: custom linked list, hash table, queue, stack, graph, binary tree, binary search tree, disjoint-set union, binary heap, priority queue, circular queue, deque, segment tree, LRU cache, Trie, and Merkle tree. The menu explains the structure used for relevant actions.

## Prerequisites

- JDK 17 or newer
- Apache Maven

## Build and run

From the `BLACKOUT` project directory:

```text
mvn test
mvn -q -DskipTests package
java -cp target/classes blackout.cli.Main
```

Use the numbered terminal menus to enter server IDs, names, connections, outage severity, recovery deadlines, jobs, configuration names, telemetry, load samples, and audit verification candidates. Enter `0` in a submenu to return, or `0` at the main menu to exit.

## Detailed guide

See [docs/DSA_EXPLANATION.md](docs/DSA_EXPLANATION.md) for the scenario-to-structure mapping, implementation walkthroughs, examples, complexity analysis, execution flow, and a reproducible demonstration.
