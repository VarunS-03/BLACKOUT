# Member 2 — Dependency Analysis

## Implemented structures
- **Graph**: adjacency-list server network with BFS and DFS.
- **BinaryTree**: generic tree with preorder, inorder, and postorder traversals.
- **BinarySearchTree**: ordered server-ID lookup and sorted inorder traversal.
- **DisjointSetUnion**: path compression plus union by rank.

## Application behavior
`Main` registers each server with `FailureAnalysisService.addServer`. The service owns the network graph, adds links with `connectServers`, and reports servers reachable from a selected ID with `affectedServers` (BFS). `areConnected` maps IDs to DSU indices and checks whether both IDs belong to the same connected group.

## Complexity
BFS/DFS: O(V+E). BST search: O(h), average O(log n) for a reasonably shaped tree but O(n) worst case. DSU operations are amortized O(alpha(V)) with path compression and union by rank. Because DSU uses fixed-size arrays, the service rebuilds its sets after adding a server or link; that rebuild is O(V + E alpha(V)).

## Important limitation
DSU is not designed to split a component after an edge deletion. The current menu does not delete network links.