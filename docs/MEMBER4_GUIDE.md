# Member 4 — Integrity and Audit

## Implemented structures
- **MerkleTree**: SHA-256 leaf/internal hashes, deterministic record text, domain labels (`leaf:` and `node:`), duplicate-last rule for odd levels, root and inclusion proofs.
- **LRUCache**: bounded least-recently-used cache using Java's access-ordered LinkedHashMap.
- **Trie**: case-insensitive service-name prefix lookup.
- **AuditLogger**: assigns monotonic sequence numbers and computes a Merkle root over audit records.

## Integrity demo
Run `blackout.cli.Main`. Verify a record against the trusted root, then show that a modified record fails verification.

## Complexity
Merkle tree construction O(n) hashes; proof length O(log n); verification O(log n). Trie operations are O(L) plus output size, where L is input length. LRU get/put are expected O(1).
