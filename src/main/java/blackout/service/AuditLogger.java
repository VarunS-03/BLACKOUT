package blackout.service;

import blackout.model.AuditRecord;
import blackout.structures.MerkleTree;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class AuditLogger {
    private final List<AuditRecord> records = new ArrayList<>();

    public AuditRecord log(String action, String details) {
        AuditRecord record = new AuditRecord(records.size() + 1L, action, details);
        records.add(record);
        return record;
    }

    public List<AuditRecord> records() {
        return Collections.unmodifiableList(records);
    }

    public String merkleRoot() {
        List<String> canonicalRecords = new ArrayList<>();
        for (AuditRecord record : records) {
            canonicalRecords.add(record.canonicalForm());
        }
        return new MerkleTree(canonicalRecords).getRoot();
    }

    public boolean verifyRecord(int zeroBasedIndex, String candidate, String trustedRoot) {
        List<String> canonicalRecords = new ArrayList<>();
        for (AuditRecord record : records) {
            canonicalRecords.add(record.canonicalForm());
        }

        MerkleTree tree = new MerkleTree(canonicalRecords);
        return tree.verify(candidate, zeroBasedIndex, tree.proof(zeroBasedIndex), trustedRoot);
    }
}
