# BLACKOUT: DSA and Implementation Guide

This guide describes the code that is actually in the project. BLACKOUT is an in-memory case study with an interactive console and an optional browser scenario launcher. It has no database or external runtime dependency; the browser launcher uses the JDK HTTP server.

## A. Project overview

BLACKOUT represents a small data center responding to server outages. A user can register servers, connect servers, inspect network reachability, report outages, prioritize and process recovery tasks, process routine jobs, undo configuration changes, record telemetry, inspect load totals, build a simple failover plan, and verify audit records.

These activities need different ways to store and retrieve information:

- A server directory needs insertion-order display, ID lookup, ordered IDs, and name-prefix search.
- Network links need traversal and fast connectivity checks.
- Routine jobs need first-in, first-out processing; outage incidents need both ends of a queue; recovery jobs need priority ordering.
- Telemetry needs a bounded event buffer and quick range sums.
- Configuration changes need last-in, first-out undo.
- Audit records need a compact way to check whether a candidate record matches the current records.

The console starts with deterministic sample data: eight online servers and a connected seven-link network, five routine jobs, three buffered telemetry events, six load samples, one failover plan, and 24 audit records. This is simulated state, not live infrastructure. The dashboard recomputes its counts from the services. The topology view displays graph nodes and links; it distinguishes graph connectivity from server online/offline state.

The console's guided simulations use fresh state, independent of the seeded interactive environment. The critical-outage scenario reports one selected server failed, displays its BFS-reachable context, schedules a single recovery task directly, then processes it. It does not demonstrate competing heap tasks or deque dispatch. The second scenario reports multiple incidents through the deque, dispatches them to the recovery min-heap, and shows the observed orders and recovery states. Both are deterministic demonstrations, not automatic monitoring or real repair work.

`blackout.cli.Main` reads each menu choice and the necessary values. It calls the service class or manager for the operation, which then calls the data structure. The terminal prints the result returned by that operation rather than assuming it succeeded.

## B. Scenario and data-structure mapping

| Scenario operation | User input | Data structure | Java class | Method called | Purpose |
|---|---|---|---|---|---|
| Register a server | Unique integer ID and non-empty name | Linked list, hash table, BST, Trie | `DataCenterManager` | `registerServer` | Keep insertion order, support ID lookup, keep IDs ordered, and index names by prefix |
| Add the server to network analysis | Server ID from registration | Graph and DSU | `FailureAnalysisService` | `addServer` | Add a network vertex and rebuild compact DSU indices |
| Find a server by ID | Integer ID | Custom hash table | `DataCenterManager` | `findServer` | Retrieve the server using its ID as a key |
| Find names by prefix | Name prefix | Trie | `DataCenterManager` | `serverNamesWithPrefix` | Search only names sharing the entered prefix |
| List servers in registration order | None | Custom linked list | `DataCenterManager` | `registeredServers` | Traverse the original server insertion order |
| List IDs in ascending order | None | Binary search tree | `DataCenterManager` | `serverIdsInOrder` | Use inorder traversal to produce sorted IDs |
| Connect two servers | Two registered IDs | Adjacency-list graph and DSU | `FailureAnalysisService` | `connectServers` | Store the undirected link and refresh connectivity groups |
| Check network connectivity | Two registered IDs | DSU | `FailureAnalysisService` | `areConnected` | Compare the two servers' set representatives |
| Inspect reachable servers | Starting server ID | Graph, BFS, and LRU cache | `FailureAnalysisService` | `affectedServers` | Find the connected network component and reuse recent results |
| Report an outage | Server ID, severity, deadline number, description | Deque and audit log | `RecoveryScheduler`, `AuditLogger` | `reportIncident`, `log` | Mark the server offline and place the incident at the front if most urgent, otherwise at the back |
| Dispatch an incident | None | Deque, priority queue, and binary heap | `RecoveryScheduler` | `dispatchNextIncident` | Remove the next incident and insert its task into recovery priority order |
| Process recovery | None | Priority queue and binary heap | `RecoveryScheduler` | `nextTask` | Select the task with the best severity, deadline, and ID ordering |
| Submit/process a routine job | Short job description / no input | Custom queue | `DataCenterManager` | `submitJob`, `processNextJob` | Process ordinary jobs in first-in, first-out order |
| Change/undo configuration | New configuration name / no input | Custom stack | `DataCenterManager` | `changeConfiguration`, `rollbackConfiguration` | Save the old setting and restore the latest saved setting |
| Record/read telemetry | Server ID, metric, number / no input | Circular queue | `TelemetryManager` | `record`, `readNext` | Keep a fixed-capacity first-in, first-out event buffer |
| Load or analyze samples | Sample count and values, or index range | Segment tree | `TelemetryManager` | `setLoadSamples`, `totalLoad`, `updateLoad` | Build samples, calculate a range sum, and change one sample |
| Create/view failover plan | Primary ID and up to two optional backup IDs | Binary tree | `DataCenterManager` | `setFailoverPlan`, `failoverPlanPreorder` | Represent one primary and its two possible backups |
| View/verify audit records | Record number and optional candidate action/details | Merkle tree | `AuditLogger` | `merkleRoot`, `verifyRecord` | Compare a candidate record against a root calculated from the current audit records |

The mapping is deliberately based on actual menu actions. For example, the graph is used for links and BFS traversal; the DSU is used for connectivity checks; the heap is used by the recovery priority queue. No structure is present only as an unused demonstration.

## C. Implemented data structures

### 1. Custom linked list

**What and why.** A linked list stores values in nodes connected by references. The server inventory uses it to preserve registration order and to demonstrate sequential traversal without using a Java collection for the list itself.

**Implementation.** `CustomLinkedList<T>` in `structures/CustomLinkedList.java` stores `head`, `tail`, and `size`. Each private `Node` stores a value and a reference to the next node. `add` attaches a node at the tail. `removeFirstMatch` walks from the head and repairs the preceding node's link (and the head or tail when needed). The iterator walks the same next links. The application reads the list through `DataCenterManager.registeredServers`.

**Operation example.** Register server IDs `20` then `5`. Choosing server inventory → list in registration order displays `20` before `5`; it does not sort the list.

**Complexity and space.**

| Operation | Best | Average | Worst | Reason |
|---|---:|---:|---:|---|
| `add` | O(1) | O(1) | O(1) | The tail reference identifies where to attach the node |
| `removeFirstMatch` | O(1) | O(n) | O(n) | The first node can match; otherwise nodes are checked in order |
| Full traversal | O(n) | O(n) | O(n) | Every node is visited |

The list stores O(n) nodes. Append and removal use O(1) auxiliary space. A traversal iterator uses O(1) additional space.

**Trade-off and viva.** Appending and preserving insertion order are simple, but finding a particular value requires traversal. *Why keep a tail?* It makes append constant time. *Does this list provide ID lookup?* No; ID lookup is the hash table's job.

### 2. Custom hash table

**What and why.** A hash table maps a key to a value. BLACKOUT uses server ID as the key so a server can be found without scanning every inventory entry.

**Implementation.** `CustomHashTable<K,V>` in `structures/CustomHashTable.java` has an array of buckets. Each bucket is a linked chain of `Entry` objects, so keys with the same bucket index are handled by separate chaining. `put` replaces a matching value or inserts a new entry. `get` and `containsKey` search only the selected chain. The table starts with 16 buckets and doubles its bucket count when its size exceeds twice the bucket count. `DataCenterManager.registerServer` rejects an existing ID before inserting the server; `findServer` calls `get`.

**Operation example.** Register server `20 - Database`; server inventory → find ID → enter `20` calls `findServer(20)` and displays that server.

**Complexity and space.**

| Operation | Best | Average | Worst | Reason |
|---|---:|---:|---:|---|
| `get`, `containsKey`, `put` without resize | O(1) | Expected O(1) | O(n) | Expected short chains; every key can collide into one chain |
| `put` that resizes | O(n) | O(n) | O(n) | Existing entries are reinserted into the larger bucket array |

Storage is O(n + b), where `b` is the number of buckets. Normal lookup uses O(1) auxiliary space. Key hashing and equality are assumed to take O(1) for the integer keys used here.

**Trade-off and viva.** The table gives fast average ID lookup but does not keep keys sorted and has no removal method. *What is a collision?* Two keys choose the same bucket. *How does this implementation resolve it?* It stores both entries in that bucket's linked chain.

### 3. Custom FIFO queue

**What and why.** A queue removes the earliest item first. Routine data-center jobs should not overtake earlier routine jobs, so the routine-job menu uses this queue.

**Implementation.** `CustomQueue<T>` in `structures/CustomQueue.java` uses linked nodes and `head`, `tail`, and `size`. `enqueue` attaches at the tail; `dequeue` removes the head and clears the tail when the last node is removed; `peek` reads the head. `DataCenterManager.submitJob` and `processNextJob` call `enqueue` and `dequeue`; the manager checks for an empty queue before processing.

**Operation example.** Submit `check disk` followed by `rotate logs`; process next completes `check disk` first.

**Complexity and space.**

| Operation | Best | Average | Worst | Reason |
|---|---:|---:|---:|---|
| `enqueue`, `dequeue`, `peek` | O(1) | O(1) | O(1) | The head and tail are directly referenced |

The queue stores O(n) nodes; each operation uses O(1) auxiliary space.

**Trade-off and viva.** A queue guarantees arrival order, not severity order. *How does the queue behave when empty?* The structure throws for direct `dequeue`/`peek`, while `DataCenterManager` returns `null` from its process method and the menu prints a friendly message.

### 4. Custom stack

**What and why.** A stack removes the newest item first. This is suitable for undoing configuration changes in reverse order.

**Implementation.** `CustomStack<T>` in `structures/CustomStack.java` stores values in an `ArrayList`. `push` appends; `pop` removes the last value; `peek` reads it. `DataCenterManager.changeConfiguration` pushes the current setting before replacing it. `rollbackConfiguration` pops and restores the previous setting.

**Operation example.** Change `Standard` to `Maintenance`, then to `Read-only`. Undo once restores `Maintenance`, the most recent previous setting.

**Complexity and space.**

| Operation | Best | Average | Worst | Reason |
|---|---:|---:|---:|---|
| `push` | O(1) | Amortized O(1) | O(n) on array growth | Values append to a dynamic array |
| `pop`, `peek` | O(1) | O(1) | O(1) | The final array position is accessed |

The stack stores O(n) settings. `push`, `pop`, and `peek` use O(1) auxiliary space, except an occasional array resize during `push`.

**Trade-off and viva.** Undo is naturally LIFO; it cannot jump directly to an older setting without undoing newer changes. *Which configuration is restored first?* The last one pushed.

### 5. Graph

**What and why.** A graph models entities and the links between them. Servers can have multiple connections, so the network is not restricted to a tree.

**Implementation.** `Graph` in `structures/Graph.java` stores an adjacency list: a `LinkedHashMap` maps each server ID to a `LinkedHashSet` of neighbors. `addEdge` adds both directions. `bfs` uses an `ArrayDeque` as a work queue and a visited set, so each reachable server is added once. `dfs` and `removeEdge` are also implemented; the current menu uses BFS. `FailureAnalysisService.connectServers` adds links, while `affectedServers` calls `bfs`.

**Operation example.** Connect `10` to `20`, then `20` to `30`. Network → reachable from `10` returns `[10, 20, 30]` in breadth-first visit order.

**Complexity and space.**

| Operation | Best | Average | Worst | Reason |
|---|---:|---:|---:|---|
| Add a vertex/edge | Expected O(1) | Expected O(1) | O(V) on hash collisions | The adjacency map and sets are hash-based |
| BFS or DFS | O(Vc + Ec) | O(Vc + Ec) | O(V + E) | Visits the start component (`Vc`, `Ec`); worst case is the whole graph |
| Neighbors of a vertex | O(1) | O(1) | O(1) | Returns the existing adjacency set view |

The graph stores O(V + E) data. BFS uses O(V) auxiliary space for the queue, visited set, and output. DFS uses O(V) visited/output space and up to O(V) recursive call-stack space.

**Trade-off and viva.** Adjacency lists are economical for a sparse network, but checking whether two arbitrary nodes are connected requires a traversal or a supporting DSU. *Why mark a node visited when enqueuing it?* It prevents the same node from being enqueued repeatedly through different neighbors.

### 6. Disjoint-set union (DSU)

**What and why.** DSU tracks groups that are connected by relationships. BLACKOUT uses it to answer whether two registered servers belong to the same connected network group.

**Implementation.** `DisjointSetUnion` in `structures/DisjointSetUnion.java` stores `parent` and `rank` arrays. `find` follows parent links and compresses the path. `union` joins roots, attaching the lower-rank tree below the higher-rank tree. `connected` compares roots. Server IDs can be arbitrary, so `FailureAnalysisService` maps them to compact integer positions in `indexByServerId`. Because this DSU has a fixed array size and the menu only adds links/servers, the service rebuilds the groups from the graph after each network change.

**Operation example.** After connecting `10` to `20`, network → check connectivity for `10` and `20` calls `areConnected` and reports that both IDs have the same DSU representative.

**Complexity and space.**

| Operation | Best | Average | Worst | Reason |
|---|---:|---:|---:|---|
| DSU `find`, `union`, `connected` | O(1) | Amortized O(α(V)) | O(α(V)) amortized over a sequence | Path compression and union by rank; α is the inverse Ackermann function |
| Service rebuild after a server/link change | O(V + E) | O(V + E α(V)) | O(V + E α(V)) | Re-indexes vertices and processes each undirected edge |

The arrays and ID-index map use O(V) space. A DSU operation itself uses O(1) auxiliary space; the service rebuild also creates an O(V) temporary ID list.

**Trade-off and viva.** DSU is excellent for connectivity queries, but it cannot split a set after deleting a link. This menu does not delete network links. *Why rebuild after adding a server?* The DSU arrays have a fixed size and need a position for every server.

### 7. LRU cache

**What and why.** A least-recently-used cache keeps a limited number of recent results and evicts the result that has not been accessed for the longest time. BLACKOUT caches the last ten network reachability results.

**Implementation.** `LRUCache<K,V>` in `structures/LRUCache.java` uses Java's access-ordered `LinkedHashMap`; this cache is not a hand-written map. `get` marks a found key as recently used. `put` inserts or updates and removes the first (least-recent) entry if capacity is exceeded. `FailureAnalysisService.affectedServers` checks the cache before running BFS. Server/link changes replace the cache so stale reachability results are not reused.

**Operation example.** Request reachable servers from ID `10` twice without changing links. The first call runs BFS and stores the result; the second returns the cached result.

**Complexity and space.**

| Operation | Best | Average | Worst | Reason |
|---|---:|---:|---:|---|
| `get`, `put` | O(1) | Expected O(1) | O(n) in hash-map collision behavior | Access-order map operations plus possible one-entry eviction |

The cache stores at most ten key/result pairs, so its storage is O(10 + R), where R is the total size of cached result lists; auxiliary space per operation is O(1).

**Trade-off and viva.** The cache avoids repeating a traversal but uses memory and must be invalidated when the graph changes. *What does LRU stand for?* Least recently used.

### 8. Binary search tree (BST)

**What and why.** A BST stores comparable values so smaller values go left and larger values go right. It provides an ordered view of server IDs.

**Implementation.** `BinarySearchTree<T>` in `structures/BinarySearchTree.java` stores nodes with a value, left child, and right child. `insert` recursively chooses a branch; equal values are not inserted again. `inorder` visits left subtree, node, then right subtree. `DataCenterManager.registerServer` inserts each unique ID and `serverIdsInOrder` returns the inorder traversal.

**Operation example.** Register `20`, `5`, and `12`; server inventory → sorted IDs uses inorder traversal and displays `[5, 12, 20]`.

**Complexity and space.** Let h be the tree height.

| Operation | Best | Average | Worst | Reason |
|---|---:|---:|---:|---|
| `insert`, `contains` | O(1) | O(log n) for a reasonably shaped tree | O(n) | Each comparison follows one root-to-leaf path; sorted insertion can make a chain |
| `inorder` | O(n) | O(n) | O(n) | It visits each stored node |

The BST stores O(n) nodes. Insert/search use O(h) recursion or traversal space; inorder uses O(h) recursion plus O(n) output space. This is an ordinary, unbalanced BST, not an AVL or red-black tree.

**Trade-off and viva.** Inorder traversal gives sorted IDs, but the tree does not guarantee logarithmic height. *Can we claim search is always O(log n)?* No; a skewed tree takes O(n).

### 9. Trie

**What and why.** A Trie stores words by shared character prefixes. It supports finding server names that begin with a user-entered prefix.

**Implementation.** `Trie` in `structures/Trie.java` has a root node; each node has a `TreeMap<Character, Node>` of children and a `wordEnd` flag. `insert` follows or creates one child per character. `wordsWithPrefix` finds the prefix node and recursively collects complete words below it. Words and prefixes are converted to lowercase with `Locale.ROOT`, so matching is case-insensitive and results are lowercase. `DataCenterManager.registerServer` inserts each server name.

**Operation example.** Register `Database` and `Data API`; server inventory → find names by prefix → enter `data` returns `[data api, database]`.

**Complexity and space.** Let L be the input length, σ the number of distinct child characters in a node, and S the number of nodes visited under the prefix.

| Operation | Best | Average | Worst | Reason |
|---|---:|---:|---:|---|
| `insert`, `contains` | O(L) | O(L log σ) | O(L log σ) | Each character uses a `TreeMap` operation; an absent path may create L nodes |
| `wordsWithPrefix` | O(L log σ) | O(L log σ + S + W) | O(L log σ + S + W) | Finds the prefix, visits its matching subtree, and copies matching words; W is the total length of returned words |

Storage is O(total characters across distinct names). Insertion uses O(L) new nodes in the worst case; recursive collection uses O(Lmax) call-stack space plus O(W) result-string space.

**Trade-off and viva.** Prefix search is direct but stores one node per character and returns lowercase text. *Why use a `wordEnd` flag?* A name can be both a complete word and a prefix of a longer name.

### 10. Binary tree

**What and why.** A binary tree node can have up to two children. The failover plan uses a primary server as its root and up to two backup servers as its children.

**Implementation.** `BinaryTree<T>` in `structures/BinaryTree.java` has a root reference and nodes with `value`, `left`, and `right`. `DataCenterManager.setFailoverPlan` constructs the root and optional backup nodes. `preorder` visits the primary before the backups and is used by the menu; inorder and postorder are also implemented for traversal practice.

**Operation example.** Enter primary `10`, first backup `20`, and second backup `30`. The displayed preorder is `[10, 20, 30]`.

**Complexity and space.**

| Operation | Best | Average | Worst | Reason |
|---|---:|---:|---:|---|
| Set the current plan | O(1) | O(1) | O(1) | The interface creates at most three nodes |
| Preorder/inorder/postorder | O(n) | O(n) | O(n) | Each node is visited once |

The plan stores O(n) nodes. Traversal uses O(h) recursion-stack space and O(n) output space.

**Trade-off and viva.** A binary tree clearly represents this small two-backup relationship, but it cannot express an arbitrary number of backups at one node. *Is this a BST?* No; left and right positions represent backup choices, not numeric ordering.

### 11. Custom deque

**What and why.** A deque allows insertion and removal at both ends. BLACKOUT puts severity-1 (most urgent) incidents at the front and other incidents at the back, then dispatches from the front.

**Implementation.** `CustomDeque<T>` in `structures/CustomDeque.java` is a manually implemented doubly linked list. Each node has `previous` and `next`; the deque keeps `first`, `last`, and `size`. `addFirst`, `addLast`, `removeFirst`, and `removeLast` update the end links. `RecoveryScheduler.reportIncident` chooses an end; `dispatchNextIncident` removes the front.

**Operation example.** Report a severity-3 incident, then a severity-1 incident. The severity-1 incident is dispatched first because it was added at the front.

**Complexity and space.**

| Operation | Best | Average | Worst | Reason |
|---|---:|---:|---:|---|
| Add/remove/peek at either end | O(1) | O(1) | O(1) | Each end node is stored directly |

The deque stores O(n) nodes and each operation uses O(1) auxiliary space.

**Trade-off and viva.** End operations are efficient, but finding/removing an arbitrary middle incident is not provided. *Why use a doubly linked node?* It allows the last node to be removed without scanning from the front.

### 12. Binary heap

**What and why.** A binary heap keeps the most important value at its root while only partially ordering the other values. It lets the recovery scheduler repeatedly select the best task without sorting every task.

**Implementation.** `BinaryHeap<T>` in `structures/BinaryHeap.java` stores values in an `ArrayList` representing a complete binary tree. The comparator determines priority. `add` appends and `moveUp` swaps with parents while needed. `remove` replaces the root with the final value and `moveDown` swaps with the smaller child. `CustomPriorityQueue` owns a `BinaryHeap<RecoveryTask>` and defines task ordering.

**Operation example.** After incident dispatch, recovery tasks enter the heap. Processing the next recovery removes the root; a task with severity `1` ranks before severity `3`.

**Complexity and space.**

| Operation | Best | Average | Worst | Reason |
|---|---:|---:|---:|---|
| `peek` | O(1) | O(1) | O(1) | The root is at array index 0 |
| `add`, `remove` | O(1) | O(log n) | O(log n) heap work | The value moves along at most the heap height |

Heap storage is O(n). Sifting uses O(1) auxiliary space. ArrayList growth can occasionally copy O(n) elements, so insertion is O(log n) amortized including its backing array.

**Trade-off and viva.** A heap quickly gives the next task but does not keep the whole array sorted. *Where is a node's parent?* At index `(i - 1) / 2`; children are at `2i + 1` and `2i + 2`.

### 13. Custom priority queue

**What and why.** A priority queue removes tasks according to importance rather than arrival order. Recovery tasks are ordered by severity ascending (1 is highest), then deadline ascending, then task ID ascending.

**Implementation.** `CustomPriorityQueue` in `structures/CustomPriorityQueue.java` wraps `BinaryHeap<RecoveryTask>`. Its comparator uses `RecoveryTask.getSeverity`, `getDeadline`, and `getId`, in that order. `add` inserts a task; `poll` removes the best task and returns `null` when empty; `peek` checks the best task without removing it. `RecoveryScheduler.schedule` adds to it; `dispatchNextIncident` transfers the next reported incident through `schedule`, and `nextTask` polls it.

**Operation example.** Dispatch severity-3 and severity-1 incidents. The next recovery processed is severity 1. If severity is tied, the lower deadline number is selected; if that is tied too, the lower task ID wins.

**Complexity and space.**

| Operation | Best | Average | Worst | Reason |
|---|---:|---:|---:|---|
| `add`, `poll` | O(1) | O(log n) | O(log n) heap work | Delegates insertion/removal to the binary heap |
| `peek`, `size`, `isEmpty` | O(1) | O(1) | O(1) | Reads the root or stored size |

The queue stores O(n) tasks in the heap and uses O(1) auxiliary space per operation.

**Trade-off and viva.** Priority processing can delay lower-severity work while urgent tasks keep arriving. *Does severity 5 rank above severity 1?* No; lower numeric severity is treated as more urgent.

### 14. Circular queue

**What and why.** A circular queue is a fixed-capacity FIFO queue whose indices wrap around to reuse array positions. It keeps only a bounded number of recent telemetry events and refuses new events when full rather than silently overwriting them.

**Implementation.** `CircularQueue<T>` in `structures/CircularQueue.java` stores entries in an `Object[]`, with `head`, `tail`, and `size`. `offer` writes at the tail and advances it modulo capacity. `poll` clears the head slot, advances it, and reduces size. `TelemetryManager` creates the event buffer with capacity 5 and checks its size before reading.

**Operation example.** Record events A through E; a sixth event is refused. Read A, then recording F succeeds in the newly available wrapped position.

**Complexity and space.**

| Operation | Best | Average | Worst | Reason |
|---|---:|---:|---:|---|
| `offer`, `poll`, `size` | O(1) | O(1) | O(1) | Only indices and one array slot change |

Storage is O(c), where c is the fixed capacity (5 in this application). Operations use O(1) auxiliary space.

**Trade-off and viva.** The memory use is predictable, but new input is refused while full. *Why use modulo?* It wraps an index from the end of the array back to index 0.

### 15. Segment tree

**What and why.** A segment tree stores summaries of ranges. BLACKOUT uses it to calculate sums over load samples and update a single sample without recomputing every range.

**Implementation.** `SegmentTree` in `structures/SegmentTree.java` stores sums in a `long[]` sized to four times the number of samples. The recursive `build` puts each sample at a leaf and adds child sums into parent nodes. `rangeSum` visits only the nodes needed for the inclusive query range. `update` changes one leaf and recomputes sums on the path to the root. `TelemetryManager.setLoadSamples` rebuilds the tree when the user supplies a new sample list.

**Operation example.** Enter samples `[10, 20, 30, 40]`; sum indices 1 through 3 returns `90`. Update index 2 to `5`; the same range now returns `65`.

**Complexity and space.**

| Operation | Best | Average | Worst | Reason |
|---|---:|---:|---:|---|
| Build from n samples | O(n) | O(n) | O(n) | Every tree node is initialized once |
| Range sum | O(log n) | O(log n) | O(log n) | A contiguous range intersects only a logarithmic number of segment nodes |
| Point update | O(log n) | O(log n) | O(log n) | Recomputes one root-to-leaf path |

The tree uses O(n) storage (approximately 4n longs). Build recursion uses O(log n) stack space; query and update use O(log n) stack space. `TelemetryManager` also keeps an O(n) sample array so it can display and replace the current samples.

**Trade-off and viva.** The tree is useful when several range queries follow updates, but it takes more memory and work to build than a simple array. *Are query bounds inclusive?* Yes, both supplied sample indices are included.

### 16. Merkle tree

**What and why.** A Merkle tree combines cryptographic hashes so one root represents a list of records. A candidate audit record can be checked against the root and its proof.

**Implementation.** `MerkleTree` in `structures/MerkleTree.java` hashes each record as `SHA-256("leaf:" + record)`. It hashes pairs as `SHA-256("node:" + leftHash + rightHash)`. If a level has an odd number of hashes, the last hash is paired with itself. An empty list has a fixed `empty-tree` hash. `proof` collects one sibling hash per level; `verify` recomputes a candidate's path and compares the result with the supplied root. `AuditLogger` converts its records to canonical strings and rebuilds the tree for `merkleRoot` and `verifyRecord`.

**Operation example.** Audit → view records prints the current root. Choose record 1 and press Enter to keep both its action and details: the record matches. Change its details: verification reports a mismatch.

**Complexity and space.**

| Operation | Best | Average | Worst | Reason |
|---|---:|---:|---:|---|
| Build a tree for n records | O(n) hashes | O(n) hashes | O(n) hashes | Each level has fewer nodes; all levels together contain fewer than 2n nodes |
| `proof` | O(log n) | O(log n) | O(log n) | One sibling is selected at each level |
| `verify` with a proof | O(log n) hashes | O(log n) hashes | O(log n) hashes | Recomputes one root path |
| `AuditLogger.verifyRecord` | O(n) | O(n) | O(n) | Rebuilds the complete tree and proof before checking the candidate |

The tree stores O(n) hashes across its levels. Hashing leaf records also reads their text: if C is the total number of input characters, construction is O(n + C) time (O(n) hashes). The logger creates O(n) canonical strings containing O(C) characters for a root or verification request, so its verification uses O(n + C) time and space before the proof check. The tree does not store source records beyond their leaf hashes. The current application uses the root computed from its current in-memory records; a real tamper-evident system would need to preserve the trusted root separately.

**Trade-off and viva.** A Merkle proof is much shorter than the complete record list, but this implementation rebuilds rather than incrementally updating the tree. *Does a Merkle tree encrypt the record?* No; it hashes data to support integrity checks.

### Other important classes

- `Server` stores an ID, name, and online/offline state. The manager indexes a registered `Server`; outage and recovery operations change its state.
- `Job` stores an ID, description, and completion flag. It is the item held by the routine-job queue.
- `RecoveryTask` stores an ID, server ID, severity, deadline, and description. The incident deque and heap contain these tasks.
- `TelemetryEvent` stores a timestamp, server ID, metric name, and numeric value. It is the item held by the circular queue.
- `AuditRecord` stores a sequence, action, and details. `canonicalForm` separates fields with a control character and escapes that separator and backslashes before hashing.
- `AuditLogger.log` appends sequence-numbered records to an `ArrayList`; append is amortized O(1), and record storage is O(n). `records` provides an unmodifiable view. `merkleRoot` and `verifyRecord` rebuild a `MerkleTree` from canonical forms.

## D. Complexity summary

The table uses n for the number of stored items, V for vertices, E for edges, h for BST height, and L for a string's length. Hash-table and hash-based collection averages assume a reasonable distribution of hash codes.

| Data structure / operation | Time | Auxiliary space | Important condition |
|---|---:|---:|---|
| Linked list append | O(1) | O(1) | Tail reference is maintained |
| Linked list remove first match | O(n) | O(1) | Linear search may be required |
| Hash table get/put | Expected O(1), worst O(n) | O(1) normally | Separate chaining; resize rehash is O(n) |
| FIFO queue enqueue/dequeue/peek | O(1) | O(1) | Linked nodes maintain head and tail |
| Stack push/pop/peek | Amortized O(1) | O(1) normally | ArrayList push can occasionally resize in O(n) |
| Graph BFS/DFS | O(V + E) | O(V) | Adjacency-list representation; only reached component is visited |
| DSU find/union/connected | Amortized O(α(V)) | O(1) per call | Path compression and union by rank |
| DSU service rebuild | O(V + E α(V)) | O(V) temporary | Rebuilds because DSU uses fixed-size arrays |
| LRU cache get/put | Expected O(1) | O(1) per action | Java access-ordered `LinkedHashMap`; max 10 entries |
| BST insert/contains | O(h); average O(log n), worst O(n) | O(h) for insert | Unbalanced tree; sorted insertion can skew it |
| BST inorder traversal | O(n) | O(n) output + O(h) stack | Visits each node |
| Trie insert/contains | O(L log σ) | O(L) on insertion | Children are `TreeMap`s, not arrays |
| Trie prefix lookup | O(L log σ + S + W) | O(L) stack + O(W) output | S is nodes visited below the prefix; W is the total returned-word length |
| Binary tree traversal | O(n) | O(h) stack + O(n) output | Each node visited once |
| Deque add/remove at either end | O(1) | O(1) | Manually linked doubly linked nodes |
| Binary heap add/remove | O(log n) amortized | O(1) sifting | An occasional ArrayList growth copies O(n) entries |
| Priority queue add/poll/peek | O(log n) / O(log n) / O(1) | O(1) per action | Delegates to the binary heap |
| Circular queue offer/poll | O(1) | O(1) | Fixed capacity; offer returns false when full |
| Segment tree build | O(n) | O(log n) stack | Complete sum tree represented in an array |
| Segment tree range sum/update | O(log n) | O(log n) stack | Inclusive range query; point update |
| Merkle tree build | O(n + C) time; O(n) hashes | O(n) stored hashes | C is total input text length; includes all levels; odd final hash is duplicated |
| Merkle proof/verification | O(log n) hashes, plus candidate text length | O(log n) proof | `AuditLogger.verifyRecord` also rebuilds the tree in O(n + C) |
| Audit logger append | Amortized O(1) | O(1) | `ArrayList` may occasionally grow |

The binary search tree is not balanced, the graph uses an adjacency list, the heap is array-backed, and audit operations rebuild the Merkle tree. These facts are included because they affect the actual complexity.

## E. Application execution flow

```text
User selects a scenario operation
              |
              v
Main reads the required values as complete lines
              |
              v
Main checks basic input and looks up related servers
              |
              v
Manager/service calls the relevant DSA method
              |
              v
The structure returns a result or changes its stored data
              |
              v
Main displays the real result and, when useful, names the DSA concept
```

### Main classes and important methods

| Class | Responsibility and important method flow |
|---|---|
| `blackout.cli.Main` | Owns the input scanner and the in-memory managers. `run` repeats the main menu until exit. Each menu method reads complete lines, checks values, calls the relevant service/manager, and prints the outcome. `readInt`, `readLong`, and `readDouble` catch only invalid numeric text and cancel that operation; they do not terminate the menu. |
| `DataCenterManager` | Coordinates server indexes, routine jobs, configuration history, and the failover plan. `registerServer` rejects duplicate IDs, then updates list, hash table, BST, and Trie. `registeredServers`, `serverIdsInOrder`, and `serverNamesWithPrefix` return results from their corresponding structures. `processNextJob` checks empty before dequeueing and marks the job complete. `changeConfiguration` pushes the old setting; `rollbackConfiguration` restores and returns it or returns `null` if there is no history. `setFailoverPlan` builds a root and up to two child nodes. |
| `FailureAnalysisService` | Coordinates the graph, DSU mapping, and reachability cache. `addServer` adds a vertex and rebuilds DSU groups. `connectServers` rejects unknown, duplicate, or self links; then adds the edge, rebuilds groups, and clears cached BFS results. `areConnected` maps IDs to array positions before asking DSU. `affectedServers` returns a cached result or runs BFS and stores it. |
| `RecoveryScheduler` | Holds reported incidents in a deque and dispatched tasks in a priority queue. `reportIncident` chooses the front for severity 1 and the back for other severities. `dispatchNextIncident` checks for an empty deque, removes its front, and sends it to `schedule`; `schedule` inserts it into the priority queue. `nextTask` polls the best task or returns `null` when no tasks have been dispatched. |
| `TelemetryManager` | Holds a five-entry circular queue and a segment tree of user-provided integer load samples. `record` returns whether the event fit. `readNext` removes the oldest event. `setLoadSamples` copies the input and rebuilds the tree; `totalLoad` queries it; `updateLoad` updates both the tree and displayed sample array. |
| `AuditLogger` | Appends audit records and creates a Merkle tree from their canonical forms. `verifyRecord` builds the tree, makes a proof for the selected index, and compares the candidate against the supplied root. The menu checks the index before calling it. |

The menu handlers in `Main` keep the user-facing flow direct:

| Main method(s) | Input and checks | Calls and result |
|---|---|---|
| `run`, `printMainMenu` | Repeatedly reads the main choice | Routes to one scenario menu; stops only on choice `0` |
| `manageServers`, `registerServer` | Reads a unique ID and non-empty name | Calls `DataCenterManager.registerServer`, then `FailureAnalysisService.addServer` and `AuditLogger.log` |
| `findServer`, `findServerNames`, `listServers`, `listSortedServerIds` | Reads an ID or prefix where needed | Calls the hash, Trie, linked-list, or BST-backed manager method and prints the returned value |
| `manageNetwork`, `connectServers`, `checkConnection`, `showReachableServers` | Reads registered IDs and rejects missing or duplicate links | Calls graph/DSU/cache methods on `FailureAnalysisService` |
| `reportOutage` | Checks server exists and is online; checks severity 1–5, non-negative deadline, and non-empty description before changing state | Marks the server offline, creates a `RecoveryTask`, adds it through `RecoveryScheduler.reportIncident`, queries graph reachability, and logs the action |
| `dispatchNextIncident`, `processNextRecovery` | No additional input | Transfers the next deque incident into the priority queue, then later polls the best task and marks its server online |
| `manageRoutineJobs`, `submitJob`, `processNextJob` | Reads a non-empty description when submitting | Calls the manager's queue operations and reports empty queue or completion |
| `manageConfiguration`, `changeConfiguration`, `rollbackConfiguration` | Reads a non-empty new setting for a change | Calls the manager's stack-backed save/restore methods |
| `manageTelemetry`, `recordTelemetry`, `readTelemetry` | Checks server ID, metric, numeric finite value, and event-buffer capacity | Calls the circular-queue operations and reports whether the event was stored or read |
| `enterLoadSamples`, `sumLoadSamples`, `updateLoadSample` | Checks sample count/value limits and valid zero-based indexes | Calls the segment-tree build, range-sum, or point-update method |
| `manageFailoverPlan`, `createFailoverPlan`, `showFailoverPlan` | Requires registered distinct server IDs; `0` means no backup | Builds and displays the current binary tree in preorder |
| `manageAuditLog`, `showAuditLog`, `verifyAuditRecord` | Checks the record index; blank candidate fields keep the original text | Displays canonical record fields and root, or asks `AuditLogger` to verify the candidate against the current root |
| `readLine`, `readNonBlankLine`, `readInt`, `readLong`, `readDouble` | Reads whole lines and parses requested types | Prints a simple validation message and returns `null` when a value is invalid; the calling operation then stops without changing data |

The CLI uses `Scanner.nextLine()` for every prompt, including numeric values. Parsing each entire line avoids the skipped-input problem caused by mixing `nextInt()` and `nextLine()`.

## F. Important implementation decisions

- Server IDs must be unique. `DataCenterManager.registerServer` checks the custom hash table before changing any index.
- Server names are stored in the Trie case-insensitively. Prefix results are lowercase; identical names in the Trie are represented once, even if different server IDs use that same name.
- Network connections require two registered, different IDs. A repeated link is not added. The network is undirected.
- The DSU uses compact indices instead of server IDs because IDs can be negative or widely spaced. It is rebuilt after a server or link is added; no link-removal operation exists in the menu.
- Severity is an integer from 1 to 5, with 1 most urgent. The deadline is a numeric ordering value: smaller means sooner. A task is finally ordered by ID to make ties predictable.
- Severity-1 incidents enter the deque at the front; other incidents enter at the back. Dispatching transfers one incident into the priority queue; processing is a separate operation.
- The circular event buffer has capacity five and refuses a new event when full. It does not overwrite unread events.
- The segment tree requires at least one sample for a range query or update. Input is limited to 1–50 load samples from 0 to 100; range indices are zero-based and inclusive.
- Empty jobs, incidents, recovery tasks, telemetry, failover plans, and audit logs are reported without trying to remove or index a missing item.
- Invalid menu numbers and malformed numeric input produce a short message and return to the menu. Invalid operation data cancels the current operation without partially changing the relevant collection.
- Audit verification uses the root of the current in-memory log. The project demonstrates hashing and proofs; it does not save a trusted root outside the process.
- `CustomDeque` and the core list, queue, stack, hash table, tree, heap, circular queue, Trie, and segment tree are implemented in the project. The LRU cache intentionally uses Java's access-ordered `LinkedHashMap` rather than implementing a second hash map and linked list.
- Routine jobs use a simple `Job` class and FIFO queue. Severity-based recovery uses `RecoveryTask` and the heap-based priority queue instead.

## G. Run and demonstrate the project

### Build and run

Use JDK 17+ and Maven from the `BLACKOUT` project root:

```text
mvn test
mvn -q -DskipTests package
java -cp target/classes blackout.cli.Main
```

The `pom.xml` compiles with Java release 17 and includes JUnit 5 for tests. The application itself has no third-party runtime dependencies. To start the optional browser scenario launcher, build the JAR and run:

```text
mvn -q -DskipTests package
java -cp target/blackout-simulator-1.0.0.jar blackout.web.WebMain
```

Open the local URL printed by `WebMain` (default `http://127.0.0.1:8765`). The browser launches predefined scenarios with fresh state; it does not display or change the interactive console's seeded environment.

### Seeded console demonstration

1. Start the console and inspect its dashboard: the seeded values are simulated sample state. Choose **1 → 2** to view servers in registration order; **3 → 1** searches by ID and **3 → 2** searches names by prefix.
2. Choose **2 → 4** for the graph topology. The view shows nodes and undirected links; online/offline status is separate from graph connectivity. Choose **2 → 3**, enter `104`, and observe BFS reachability order. Reachability is network context, not proof that all listed servers failed.
3. Choose **4**, enter server `104`, severity `1`, deadline `20`, and a short description. Only server 104 is marked offline; the report shows BFS context and places severity 1 at the deque front.
4. Choose **5** to dispatch the deque front into the recovery min-heap. Choose **6** to process a task; with this one task, it is selected and server 104 returns online. With multiple dispatched tasks, the heap compares severity, deadline, then task ID.
5. Choose **9 → 1** to inspect audit entries. The seeded log has 24 records; reporting, dispatching, and recovery add records. Choose **9 → 2**, enter the latest record number (27 after this sequence), and press Enter at both candidate prompts to verify the unchanged record against the current in-memory Merkle root. Editing candidate text demonstrates a mismatch; no external trusted anchor is stored.
6. Choose **12 → 2** for the separate incident deque and recovery heap demonstration. It reports multiple incidents, shows deque dispatch order and heap processing order, and reports the resulting server states. It uses isolated state and does not alter the console's dashboard or audit log.
7. Choose **0** at the main menu to exit. In any submenu, **0** returns to the main menu.

For routine-job FIFO behavior, choose **7 → 1** to submit jobs and **7 → 2** to process the oldest. Configuration undo is **10 → 1** to change and **10 → 2** to undo. Telemetry and segment-tree operations are under **8**. **11** stores and displays a failover plan as a primary with backups; it does not switch servers automatically.
