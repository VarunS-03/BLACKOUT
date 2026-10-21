# Member 1 — Core Management

## Implemented structures
- **CustomLinkedList**: stores registered servers; append is O(1), searching/removing by value is O(n).
- **CustomQueue**: FIFO job queue; enqueue/dequeue are O(1).
- **CustomStack**: configuration snapshots; push/pop are amortized O(1).
- **CustomHashTable**: server lookup by ID using separate chaining; expected get/put O(1), worst case O(n).

## Application behavior
`DataCenterManager` registers servers, rejects duplicate IDs, queues and processes jobs, and stores/restores configuration snapshots.

## Demo
Run `blackout.cli.Main`. Explain why FIFO is appropriate for ordinary jobs and why stack order naturally supports rollback.