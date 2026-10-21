package blackout.model;
public class AuditRecord {
    private final long sequence;
    private final String action;
    private final String details;
    public AuditRecord(long sequence, String action, String details) {
        this.sequence = sequence; this.action = action; this.details = details;
    }
    public long getSequence() { return sequence; }
    public String getAction() { return action; }
    public String getDetails() { return details; }
    public String canonicalForm() {
        return sequence + "\u001f" + safe(action) + "\u001f" + safe(details);
    }
    private static String safe(String s) { return s == null ? "" : s.replace("\\", "\\\\").replace("\u001f", "\\u001f"); }
    @Override public String toString() { return canonicalForm(); }
}
