# Member 3 — Scheduling and Telemetry

## Implemented structures
- **BinaryHeap**: generic array-backed min-heap.
- **CustomPriorityQueue**: orders recovery by severity (1 is highest), then earliest deadline, then lowest task ID.
- **CircularQueue**: fixed-capacity telemetry buffer; `offer` returns false when full rather than silently overwriting data.
- **CustomDeque**: supports insertion/removal at both ends.
- **SegmentTree**: range-sum queries and point updates for load samples.

## Complexity
Heap add/remove O(log n), peek O(1). Circular queue offer/poll O(1). Segment-tree build O(n), range sum and update O(log n).
