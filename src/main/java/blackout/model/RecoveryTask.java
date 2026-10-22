package blackout.model;

public class RecoveryTask {
    private final int id;
    private final int serverId;
    private final int severity;
    private final long deadline;
    private final String description;

    public RecoveryTask(int id, int serverId, int severity, long deadline, String description) {
        this.id = id;
        this.serverId = serverId;
        this.severity = severity;
        this.deadline = deadline;
        this.description = description;
    }

    public int getId() {
        return id;
    }

    public int getServerId() {
        return serverId;
    }

    public int getSeverity() {
        return severity;
    }

    public long getDeadline() {
        return deadline;
    }

    public String getDescription() {
        return description;
    }
}
