package blackout.service;

import blackout.model.AuditRecord;
import blackout.model.Job;
import blackout.model.RecoveryTask;
import blackout.model.Server;
import blackout.model.TelemetryEvent;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SimulatorServiceTest {
    @Test
    void managerProcessesJobsAndFindsRegisteredServers() {
        DataCenterManager manager = new DataCenterManager();
        Server server = new Server(12, "db-12");
        assertTrue(manager.registerServer(server));
        assertSame(server, manager.findServer(12));
        assertEquals(1, manager.serverCount());
        assertFalse(manager.registerServer(new Server(12, "duplicate")));

        Job first = new Job(1, "first");
        Job second = new Job(2, "second");
        manager.submitJob(first);
        manager.submitJob(second);
        assertSame(first, manager.processNextJob());
        assertTrue(first.isCompleted());
        assertSame(second, manager.processNextJob());
        assertNull(manager.processNextJob());
        manager.changeConfiguration("before");
        assertEquals("Standard", manager.rollbackConfiguration());
        assertEquals("Standard", manager.currentConfiguration());
        assertNull(manager.rollbackConfiguration());
    }

    @Test
    void supportedFailureSchedulingTelemetryAndAuditOperationsAgree() {
        FailureAnalysisService failure = new FailureAnalysisService();
        assertTrue(failure.addServer(10));
        assertTrue(failure.addServer(11));
        assertTrue(failure.addServer(12));
        assertTrue(failure.connectServers(10, 11));
        assertTrue(failure.connectServers(11, 12));
        assertEquals(java.util.List.of(10, 11, 12), failure.affectedServers(10));
        assertTrue(failure.areConnected(10, 12));

        RecoveryScheduler scheduler = new RecoveryScheduler();
        scheduler.schedule(new RecoveryTask(1, 10, 2, 100, "normal"));
        scheduler.schedule(new RecoveryTask(2, 11, 1, 200, "critical"));
        assertEquals(2, scheduler.nextTask().getId());
        assertEquals(1, scheduler.nextTask().getId());
        assertEquals(0, scheduler.pendingTasks());

        TelemetryManager telemetry = new TelemetryManager(1);
        TelemetryEvent event = new TelemetryEvent(123, 10, "cpu", 0.8);
        assertTrue(telemetry.record(event));
        assertFalse(telemetry.record(new TelemetryEvent(124, 10, "cpu", 0.9)));
        assertSame(event, telemetry.readNext());
        assertEquals(0, telemetry.bufferedEvents());

        AuditLogger audit = new AuditLogger();
        AuditRecord record = audit.log("SERVER_DOWN", "server=10");
        String trustedRoot = audit.merkleRoot();
        assertTrue(audit.verifyRecord(0, record.canonicalForm(), trustedRoot));
        assertFalse(audit.verifyRecord(0, "changed", trustedRoot));
        assertEquals(1, audit.records().size());
    }
}
