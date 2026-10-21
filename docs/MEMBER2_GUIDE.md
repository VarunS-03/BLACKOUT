# Member 2 — Dependency Analysis

## Implemented structures
- **Graph**: adjacency-list server network with BFS and DFS.
- **BinaryTree**: generic tree with preorder, inorder, and postorder traversals.
- **BinarySearchTree**: ordered server-ID lookup and sorted inorder traversal.
- **DisjointSetUnion**: path compression plus union by rank.

## Application behavior
`FailureAnalysisService` exposes the network and reports the component reachable from a selected server.

## Complexity
BFS/DFS: O(V+E). BST search: O(h), average O(log n) if balanced but O(n) worst case. DSU operations are near O(1) amortized (inverse-Ackermann bound).

## Important limitation
DSU is not designed to split a component after an edge deletion. For changing network links, rebuild the DSU from the remaining links.