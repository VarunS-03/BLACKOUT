# Member 4 — Integrity and Audit

## Implemented structures
- **MerkleTree**: SHA-256 leaf/internal hashes, deterministic record text, domain labels (`leaf:` and `node:`), duplicate-last rule for odd levels, root and inclusion proofs.
- **LRUCache**: 10-entry least-recently-used cache for network reachability results, using Java's access-ordered LinkedHashMap.
- **Trie**: case-insensitive server-name prefix lookup; results are lowercase.
- **AuditLogger**: assigns monotonic sequence numbers and computes a Merkle root over audit records.

## Integrity demo
Run `blackout.cli.Main`, register and operate on servers, then open the audit menu. Verify a record against the current Merkle root, then change its candidate details to demonstrate a failed verification. In this in-memory case study the root is recalculated from the current log; a production system would preserve its trusted root separately.

## Complexity
Merkle tree construction uses O(n) hashes plus time to read the record text; proof length is O(log n), and proof verification uses O(log n) hashes. `AuditLogger.verifyRecord` rebuilds the tree, so that application operation is O(n) hashes plus the record text length. Trie insertion/lookup is O(L log sigma), where `sigma` is the number of child characters in a node; prefix search also visits the matching subtree and copies the returned names. LRU get/put are expected O(1).
