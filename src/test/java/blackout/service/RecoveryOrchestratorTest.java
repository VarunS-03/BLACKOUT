package blackout.service;

import blackout.model.RecoveryTask;
import blackout.model.Server;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class RecoveryOrchestratorTest {
    @Test
    void invalidRecoveryInputsAreRejectedBeforeChangingState() {
        for (int[] input : new int[][]{{0, 0}, {6, 0}, {1, -1}}) {
            RecoveryOrchestrator failure = new RecoveryOrchestrator();
            Server a = new Server(1, "a");
            failure.registerServer(a);
            assertThrows(IllegalArgumentException.class,
                    () -> failure.simulateFailure(1, input[0], input[1]));
            assertTrue(a.isOnline());
            assertEquals(0, failure.pendingRecoveries());
            assertEquals(1, failure.auditRecords().size());

            RecoveryOrchestrator incident = new RecoveryOrchestrator();
            Server b = new Server(2, "b");
            incident.registerServer(b);
            assertThrows(IllegalArgumentException.class,
                    () -> incident.reportIncident(2, input[0], input[1]));
            assertTrue(b.isOnline());
            assertEquals(0, incident.pendingIncidents());
            assertEquals(0, incident.pendingRecoveries());
            assertEquals(1, incident.auditRecords().size());
        }
    }

    @Test
    void recoveryInputBoundariesRemainSupportedByBothOperations() {
        for (int severity : new int[]{1, 5}) {
            RecoveryOrchestrator failure = new RecoveryOrchestrator();
            Server a = new Server(1, "a");
            failure.registerServer(a);
            failure.simulateFailure(1, severity, 0);
            assertFalse(a.isOnline());
            assertEquals(1, failure.pendingRecoveries());

            RecoveryOrchestrator incident = new RecoveryOrchestrator();
            Server b = new Server(2, "b");
            incident.registerServer(b);
            incident.reportIncident(2, severity, 0);
            assertFalse(b.isOnline());
            assertEquals(1, incident.pendingIncidents());
        }
    }

    @Test
    void failureAnalysisSchedulingRecoveryStateAndAuditStayConsistent() {
        RecoveryOrchestrator simulator = new RecoveryOrchestrator();
        Server database = new Server(10, "database");
        Server api = new Server(11, "api");
        Server cache = new Server(12, "cache");
        simulator.registerServer(database);
        simulator.registerServer(api);
        simulator.registerServer(cache);
        simulator.connectServers(10, 11);
        simulator.connectServers(11, 12);

        assertEquals(List.of(10, 11, 12), simulator.simulateFailure(10, 2, 200));
        assertFalse(database.isOnline());
        assertTrue(api.isOnline());
        assertTrue(cache.isOnline());
        assertEquals(1, simulator.pendingRecoveries());

        simulator.simulateFailure(12, 1, 300);
        assertFalse(cache.isOnline());
        assertEquals(2, simulator.pendingRecoveries());

        RecoveryTask first = simulator.processNextRecovery();
        assertEquals(12, first.getServerId());
        assertTrue(cache.isOnline());
        assertFalse(database.isOnline());
        assertEquals(1, simulator.pendingRecoveries());

        RecoveryTask second = simulator.processNextRecovery();
        assertEquals(10, second.getServerId());
        assertTrue(database.isOnline());
        assertTrue(api.isOnline());
        assertNull(simulator.processNextRecovery());
        assertEquals(0, simulator.pendingRecoveries());

        assertEquals("SERVER_FAILED", simulator.auditRecords().get(5).getAction());
        assertEquals("RECOVERY_COMPLETED", simulator.auditRecords().get(7).getAction());
        assertEquals(9, simulator.auditRecords().size());
        assertNotNull(simulator.auditMerkleRoot());
    }

    @Test
    void invalidAndRepeatedFailuresDoNotScheduleOrAuditFalseWork() {
        RecoveryOrchestrator simulator = new RecoveryOrchestrator();
        Server server = new Server(5, "worker");
        simulator.registerServer(server);

        assertThrows(IllegalArgumentException.class, () -> simulator.simulateFailure(99, 1, 10));
        assertThrows(IllegalArgumentException.class, () -> simulator.connectServers(5, 99));
        assertTrue(server.isOnline());
        assertEquals(0, simulator.pendingRecoveries());
        assertEquals(1, simulator.auditRecords().size());

        simulator.simulateFailure(5, 1, 10);
        assertThrows(IllegalStateException.class, () -> simulator.simulateFailure(5, 1, 10));
        assertFalse(server.isOnline());
        assertEquals(1, simulator.pendingRecoveries());
        assertEquals(2, simulator.auditRecords().size());

        assertEquals(5, simulator.processNextRecovery().getServerId());
        assertTrue(server.isOnline());
        assertEquals(3, simulator.auditRecords().size());
    }
}
