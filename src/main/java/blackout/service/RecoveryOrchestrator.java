package blackout.service;

import blackout.model.AuditRecord;
import blackout.model.RecoveryTask;
import blackout.model.Server;

import java.util.List;

/** Coordinates the existing server, dependency, recovery, and audit services. */
public class RecoveryOrchestrator {
    private final DataCenterManager dataCenter = new DataCenterManager();
    private final FailureAnalysisService failureAnalysis = new FailureAnalysisService();
    private final RecoveryScheduler recoveryScheduler = new RecoveryScheduler();
    private final AuditLogger auditLogger = new AuditLogger();
    private int nextTaskId = 1;

    public void registerServer(Server server) {
        if (!dataCenter.registerServer(server)) {
            throw new IllegalArgumentException("Server ID is already registered: " + server.getId());
        }
        failureAnalysis.addServer(server.getId());
        auditLogger.log("SERVER_REGISTERED", "server=" + server.getId());
    }

    public void connectServers(int firstServerId, int secondServerId) {
        requireServer(firstServerId);
        requireServer(secondServerId);
        if (!failureAnalysis.connectServers(firstServerId, secondServerId)) {
            throw new IllegalArgumentException("Connection could not be added: "
                    + firstServerId + "-" + secondServerId);
        }
        auditLogger.log("SERVERS_CONNECTED", firstServerId + "-" + secondServerId);
    }

    /** Marks the requested server offline and schedules its recovery. */
    public List<Integer> simulateFailure(int serverId, int severity, long deadline) {
        validateRecoveryInputs(severity, deadline);
        Server server = requireServer(serverId);
        if (!server.isOnline()) {
            throw new IllegalStateException("Server is already offline: " + serverId);
        }

        List<Integer> affectedServers = failureAnalysis.affectedServers(serverId);
        int taskId = nextTaskId++;
        server.setOnline(false);
        recoveryScheduler.schedule(new RecoveryTask(
                taskId, serverId, severity, deadline, "Recover server " + server.getName()));
        auditLogger.log("SERVER_FAILED", "server=" + serverId + ";affected=" + affectedServers);
        return affectedServers;
    }

    /** Reports an incident into the scheduler deque; severity 1 is treated as urgent. */
    public RecoveryTask reportIncident(int serverId, int severity, long deadline) {
        validateRecoveryInputs(severity, deadline);
        Server server = requireServer(serverId);
        if (!server.isOnline()) {
            throw new IllegalStateException("Server is already offline: " + serverId);
        }
        RecoveryTask task = new RecoveryTask(nextTaskId++, serverId, severity, deadline,
                "Recover server " + server.getName());
        server.setOnline(false);
        recoveryScheduler.reportIncident(task, severity == 1);
        auditLogger.log("INCIDENT_REPORTED", "task=" + task.getId() + ";server=" + serverId);
        return task;
    }

    /** Dispatches the deque's next incident into the recovery priority queue. */
    public RecoveryTask dispatchNextIncident() {
        RecoveryTask task = recoveryScheduler.dispatchNextIncident();
        if (task != null) {
            auditLogger.log("INCIDENT_DISPATCHED", "task=" + task.getId() + ";server=" + task.getServerId());
        }
        return task;
    }

    public int pendingIncidents() {
        return recoveryScheduler.pendingIncidents();
    }

    /** Executes the next scheduled task and returns null when there is no work. */
    public RecoveryTask processNextRecovery() {
        RecoveryTask task = recoveryScheduler.nextTask();
        if (task == null) return null;

        Server server = dataCenter.findServer(task.getServerId());
        if (server == null) {
            throw new IllegalStateException("Recovery task references unknown server: " + task.getServerId());
        }
        server.setOnline(true);
        auditLogger.log("RECOVERY_COMPLETED", "task=" + task.getId() + ";server=" + task.getServerId());
        return task;
    }

    public Server findServer(int serverId) {
        return dataCenter.findServer(serverId);
    }

    public int pendingRecoveries() {
        return recoveryScheduler.pendingTasks();
    }

    public List<AuditRecord> auditRecords() {
        return auditLogger.records();
    }

    public String auditMerkleRoot() {
        return auditLogger.merkleRoot();
    }

    private Server requireServer(int serverId) {
        Server server = dataCenter.findServer(serverId);
        if (server == null) throw new IllegalArgumentException("Unknown server: " + serverId);
        return server;
    }

    private void validateRecoveryInputs(int severity, long deadline) {
        if (severity < 1 || severity > 5) {
            throw new IllegalArgumentException("Severity must be from 1 to 5.");
        }
        if (deadline < 0) {
            throw new IllegalArgumentException("Deadline must be non-negative.");
        }
    }
}
