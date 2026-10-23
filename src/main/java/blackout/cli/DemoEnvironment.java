package blackout.cli;

import blackout.model.Job;
import blackout.model.Server;
import blackout.model.TelemetryEvent;
import blackout.service.AuditLogger;
import blackout.service.DataCenterManager;
import blackout.service.FailureAnalysisService;
import blackout.service.RecoveryScheduler;
import blackout.service.TelemetryManager;

/** Loads the deterministic, healthy sample data used by the console application. */
public final class DemoEnvironment {
    private static final String[][] SERVERS = {
            {"101", "Edge Gateway"},
            {"102", "API Cluster A"},
            {"103", "API Cluster B"},
            {"104", "Customer Database"},
            {"105", "Cache Cluster"},
            {"106", "Authentication Service"},
            {"107", "Analytics Worker"},
            {"108", "Monitoring Node"}
    };

    private static final int[][] CONNECTIONS = {
            {101, 102}, {101, 103}, {102, 104}, {102, 105},
            {103, 106}, {103, 107}, {101, 108}
    };

    private static final String[] JOBS = {
            "Rotate access logs",
            "Verify database backups",
            "Refresh service health checks",
            "Rebuild search index",
            "Review capacity alerts"
    };

    private DemoEnvironment() { }

    /** Loads sample data into empty services and returns the next routine-job ID. */
    public static int initialize(DataCenterManager dataCenter,
                                 FailureAnalysisService failureAnalysis,
                                 RecoveryScheduler recoveryScheduler,
                                 TelemetryManager telemetryManager,
                                 AuditLogger auditLogger) {
        validateEmpty(dataCenter, failureAnalysis, recoveryScheduler, telemetryManager, auditLogger);
        if (telemetryManager.eventCapacity() < 3) {
            throw new IllegalStateException("The demo environment requires telemetry capacity of at least 3.");
        }

        for (String[] serverData : SERVERS) {
            Server server = new Server(Integer.parseInt(serverData[0]), serverData[1]);
            if (!dataCenter.registerServer(server)) {
                throw new IllegalStateException("Could not register demo server " + server.getId());
            }
            if (!failureAnalysis.addServer(server.getId())) {
                throw new IllegalStateException("Could not add demo server to network " + server.getId());
            }
            auditLogger.log("SERVER_REGISTERED", "id=" + server.getId() + ", name=" + server.getName());
        }

        for (int[] connection : CONNECTIONS) {
            if (!failureAnalysis.connectServers(connection[0], connection[1])) {
                throw new IllegalStateException("Could not connect demo servers "
                        + connection[0] + " and " + connection[1]);
            }
            auditLogger.log("SERVERS_CONNECTED", connection[0] + "<->" + connection[1]);
        }

        int nextJobId = 1;
        for (String description : JOBS) {
            Job job = new Job(nextJobId++, description);
            dataCenter.submitJob(job);
            auditLogger.log("JOB_SUBMITTED", "job=" + job.getId());
        }

        recordTelemetry(telemetryManager, auditLogger,
                new TelemetryEvent(1_700_000_000_000L, 101, "cpu", 0.42));
        recordTelemetry(telemetryManager, auditLogger,
                new TelemetryEvent(1_700_000_001_000L, 104, "memory", 0.68));
        recordTelemetry(telemetryManager, auditLogger,
                new TelemetryEvent(1_700_000_002_000L, 107, "request_rate", 120.0));

        telemetryManager.setLoadSamples(new int[]{34, 48, 52, 39, 61, 44});
        dataCenter.setFailoverPlan(104, 102, 108);
        auditLogger.log("FAILOVER_PLAN_SET", "primary=104");
        return nextJobId;
    }

    private static void validateEmpty(DataCenterManager dataCenter,
                                      FailureAnalysisService failureAnalysis,
                                      RecoveryScheduler recoveryScheduler,
                                      TelemetryManager telemetryManager,
                                      AuditLogger auditLogger) {
        if (dataCenter.serverCount() != 0
                || dataCenter.queuedJobs() != 0
                || !"Standard".equals(dataCenter.currentConfiguration())
                || dataCenter.hasConfigurationHistory()
                || !dataCenter.failoverPlanPreorder().isEmpty()
                || !failureAnalysis.serverIds().isEmpty()
                || recoveryScheduler.pendingIncidents() != 0
                || recoveryScheduler.pendingTasks() != 0
                || telemetryManager.bufferedEvents() != 0
                || telemetryManager.loadSamples().length != 0
                || !auditLogger.records().isEmpty()) {
            throw new IllegalStateException("Demo environment can only initialize empty services.");
        }
    }

    private static void recordTelemetry(TelemetryManager telemetryManager, AuditLogger auditLogger,
                                        TelemetryEvent event) {
        if (!telemetryManager.record(event)) {
            throw new IllegalStateException("Could not record demo telemetry for server " + event.getServerId());
        }
        auditLogger.log("TELEMETRY_RECORDED",
                "server=" + event.getServerId() + ", metric=" + event.getMetric());
    }
}
