package blackout.cli;

import blackout.model.AuditRecord;
import blackout.model.Server;
import blackout.model.TelemetryEvent;
import blackout.service.AuditLogger;
import blackout.service.DataCenterManager;
import blackout.service.FailureAnalysisService;
import blackout.service.RecoveryScheduler;
import blackout.service.TelemetryManager;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.PrintStream;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class MainConsoleTest {
    @Test
    void demoEnvironmentInitializesConsistentSampleData() {
        DataCenterManager dataCenter = new DataCenterManager();
        FailureAnalysisService failureAnalysis = new FailureAnalysisService();
        RecoveryScheduler scheduler = new RecoveryScheduler();
        TelemetryManager telemetry = new TelemetryManager(5);
        AuditLogger audit = new AuditLogger();

        int nextJobId = DemoEnvironment.initialize(dataCenter, failureAnalysis, scheduler, telemetry, audit);

        assertEquals(6, nextJobId);
        assertEquals(8, dataCenter.serverCount());
        assertEquals(List.of("Edge Gateway", "API Cluster A", "API Cluster B", "Customer Database",
                        "Cache Cluster", "Authentication Service", "Analytics Worker", "Monitoring Node"),
                dataCenter.registeredServers().stream().map(Server::getName).toList());
        assertTrue(dataCenter.registeredServers().stream().allMatch(Server::isOnline));
        assertEquals(8, failureAnalysis.serverIds().size());
        assertTrue(failureAnalysis.areConnected(101, 108));
        assertTrue(failureAnalysis.areConnected(104, 107));
        assertEquals(List.of(101, 102, 103, 108, 104, 105, 106, 107),
                failureAnalysis.affectedServers(101));
        assertEquals(5, dataCenter.queuedJobs());
        assertEquals(3, telemetry.bufferedEvents());
        assertEquals(5, telemetry.eventCapacity());
        assertArrayEquals(new int[]{34, 48, 52, 39, 61, 44}, telemetry.loadSamples());
        assertEquals(List.of(104, 102, 108), dataCenter.failoverPlanPreorder());
        assertEquals("Standard", dataCenter.currentConfiguration());
        assertEquals(0, scheduler.pendingIncidents());
        assertEquals(0, scheduler.pendingTasks());
        assertEquals(24, audit.records().size());
        assertEquals(8, auditCount(audit, "SERVER_REGISTERED"));
        assertEquals(7, auditCount(audit, "SERVERS_CONNECTED"));
        assertEquals(5, auditCount(audit, "JOB_SUBMITTED"));
        assertEquals(3, auditCount(audit, "TELEMETRY_RECORDED"));
        assertEquals(1, auditCount(audit, "FAILOVER_PLAN_SET"));

        TelemetryEvent cpu = telemetry.readNext();
        assertEquals(101, cpu.getServerId());
        assertEquals("cpu", cpu.getMetric());
        assertEquals(0.42, cpu.getValue());
        assertEquals(1_700_000_000_000L, cpu.getTimestamp());
    }

    @Test
    void normalStartupDisplaysSeededDashboardAndEmptyConstructionRemainsAvailable() {
        String startup = runConsole(true, true, "0");
        assertTrue(startup.contains("BLACKOUT"));
        assertTrue(startup.contains("DATA CENTER OPERATIONS"));
        assertTrue(startup.contains("Environment: DEMO"));
        assertTrue(startup.contains("Deterministic sample data - not live infrastructure monitoring"));
        assertTrue(startup.contains("SYSTEM STATUS: OPERATIONAL"));
        assertTrue(startup.contains("8 total"));
        assertTrue(startup.contains("8 online"));
        assertTrue(startup.contains("0 offline"));
        assertTrue(startup.contains("Connected"));
        assertTrue(startup.contains("8 nodes"));
        assertTrue(startup.contains("7 links"));
        assertTrue(startup.contains("5 waiting"));
        assertTrue(startup.contains("3/5 events buffered"));
        assertTrue(startup.contains("6 recorded"));
        assertTrue(startup.contains("24 entries"));
        assertTrue(startup.contains("Configuration       Standard"));
        for (int option = 1; option <= 12; option++) {
            assertTrue(startup.contains("[" + option + "]"), "Missing main-menu option " + option);
        }

        String empty = runConsole(false, false, "0");
        assertTrue(empty.contains("SYSTEM STATUS: NO DATA"));
        assertTrue(empty.contains("0 total"));
        assertTrue(empty.contains("0 online"));
        assertTrue(empty.contains("0 offline"));
        assertTrue(empty.contains("Disconnected"));
        assertTrue(empty.contains("0 nodes"));
        assertTrue(empty.contains("0/5 events buffered"));
        assertTrue(empty.contains("Configuration       Standard"));
    }

    @Test
    void repeatedDemoInitializationFailsBeforeChangingExistingData() {
        DataCenterManager dataCenter = new DataCenterManager();
        FailureAnalysisService failureAnalysis = new FailureAnalysisService();
        RecoveryScheduler scheduler = new RecoveryScheduler();
        TelemetryManager telemetry = new TelemetryManager(5);
        AuditLogger audit = new AuditLogger();
        DemoEnvironment.initialize(dataCenter, failureAnalysis, scheduler, telemetry, audit);

        assertThrows(IllegalStateException.class,
                () -> DemoEnvironment.initialize(dataCenter, failureAnalysis, scheduler, telemetry, audit));

        assertEquals(8, dataCenter.serverCount());
        assertEquals(5, dataCenter.queuedJobs());
        assertEquals(3, telemetry.bufferedEvents());
        assertEquals(24, audit.records().size());
        assertEquals(0, scheduler.pendingIncidents());
        assertEquals(0, scheduler.pendingTasks());
    }

    @Test
    void demoInitializationRejectsStandardConfigurationWithSavedHistoryBeforeSeeding() {
        DataCenterManager dataCenter = new DataCenterManager();
        dataCenter.changeConfiguration("Maintenance");
        dataCenter.changeConfiguration("Standard");
        assertEquals("Standard", dataCenter.currentConfiguration());
        assertTrue(dataCenter.hasConfigurationHistory());
        assertEquals("Maintenance", dataCenter.rollbackConfiguration());
        assertTrue(dataCenter.hasConfigurationHistory());
        assertEquals("Standard", dataCenter.rollbackConfiguration());
        assertFalse(dataCenter.hasConfigurationHistory());
        dataCenter.changeConfiguration("Maintenance");
        dataCenter.changeConfiguration("Standard");

        FailureAnalysisService network = new FailureAnalysisService();
        RecoveryScheduler scheduler = new RecoveryScheduler();
        TelemetryManager telemetry = new TelemetryManager(5);
        AuditLogger audit = new AuditLogger();
        assertThrows(IllegalStateException.class,
                () -> DemoEnvironment.initialize(dataCenter, network, scheduler, telemetry, audit));
        assertEquals(0, dataCenter.serverCount());
        assertEquals(0, dataCenter.queuedJobs());
        assertTrue(dataCenter.failoverPlanPreorder().isEmpty());
        assertTrue(network.serverIds().isEmpty());
        assertEquals(0, network.connectionCount());
        assertEquals(0, telemetry.bufferedEvents());
        assertEquals(0, telemetry.loadSamples().length);
        assertTrue(audit.records().isEmpty());
        assertEquals(0, scheduler.pendingIncidents());
        assertEquals(0, scheduler.pendingTasks());
    }

    @Test
    void emptyDashboardAndInvalidMenuInputLeaveStateUnchanged() {
        String output = runConsole("not-a-number", "0");

        assertTrue(output.contains("0 total"));
        assertTrue(output.contains("0 online"));
        assertTrue(output.contains("0 offline"));
        assertTrue(output.contains("SYSTEM STATUS: NO DATA"));
        assertTrue(output.contains("Incidents           0 pending"));
        assertTrue(output.contains("Routine jobs        0 waiting"));
        assertTrue(output.contains("Telemetry           0/5 events buffered"));
        assertTrue(output.contains("Audit records       0 entries"));
        assertTrue(output.contains("[WARN] Enter a whole number. This operation was cancelled."));
        assertTrue(output.contains("[12] Guided simulation"));
        assertTrue(output.contains("BLACKOUT closed."));
        assertTrue(occurrences(output, "SYSTEM STATUS: NO DATA") == 2,
                "The dashboard should display the same empty state after invalid input.");
    }

    @Test
    void exhaustedInputAtMainMenuExitsWithoutAnException() {
        String output = runConsoleRaw("");

        assertTrue(output.contains("Enter your choice:"));
        assertTrue(output.contains("BLACKOUT closed."));
        assertFalse(output.contains("Exception"));
        assertEquals(1, occurrences(output, "SYSTEM STATUS:"));
    }

    @Test
    void exhaustedInputInsideSubmenuExitsWithoutRepeatingThePrompt() {
        String output = runConsoleRaw("1\n");

        assertTrue(output.contains("SERVER MANAGEMENT"));
        assertTrue(output.contains("BLACKOUT closed."));
        assertFalse(output.contains("Exception"));
        assertEquals(1, occurrences(output, "Choose an operation:"));
        assertEquals(1, occurrences(output, "SYSTEM STATUS:"));
    }

    @Test
    void exhaustedInputDuringRangeQueryDoesNotPerformAnOperationOrCrash() {
        String output = runConsoleRaw("8\n3\n2\n10\n20\n4\n");

        assertTrue(output.contains("Enter the first sample index (0-based):"));
        assertTrue(output.contains("BLACKOUT closed."));
        assertFalse(output.contains("Sum of samples"));
        assertFalse(output.contains("Exception"));
        assertEquals(1, occurrences(output, "SYSTEM STATUS:"));
    }

    @Test
    void operationMessagesDistinguishSuccessWarningsAndEmptyStates() {
        String output = runConsole(
                "1", "1", "1", "Node A", "1", "1", "Duplicate", "2", "0",
                "3", "1", "99", "2", "Ghost", "3", "0",
                "2", "1", "1", "99", "2", "1", "99", "3", "1", "0",
                "4", "99", "5", "6", "7", "2", "0", "10", "2", "0",
                "8", "2",
                "1", "1", "cpu", "0.1", "1", "1", "cpu", "0.2",
                "1", "1", "cpu", "0.3", "1", "1", "cpu", "0.4",
                "1", "1", "cpu", "0.5", "1", "1", "cpu", "0.6",
                "2", "3", "2", "5", "6", "4", "1", "0", "5", "3", "20", "0",
                "11", "1", "99", "0", "0", "0",
                "9", "2", "99", "0");

        assertTrue(output.contains("[OK] Registered server 1 - Node A (ONLINE)."));
        assertTrue(output.contains("| ONLINE |"));
        assertTrue(output.contains("[WARN] Server ID 1 is already registered."));
        assertTrue(output.contains("[WARN] No server has ID 99."));
        assertTrue(output.contains("[INFO] No server names start with \"Ghost\"."));
        assertTrue(output.contains("[OK] Server IDs in ascending order:"));
        assertTrue(output.contains("| 1         |"));
        assertTrue(output.contains("[WARN] Both server IDs must be registered first."));
        assertTrue(output.contains("[INFO] No reported incidents are waiting for dispatch."));
        assertTrue(output.contains("[INFO] No dispatched recovery tasks are waiting."));
        assertTrue(output.contains("[INFO] No routine jobs are waiting."));
        assertTrue(output.contains("[INFO] No saved configuration change is available to undo."));
        assertTrue(output.contains("[INFO] No telemetry events are buffered."));
        assertTrue(output.contains("[WARN] Telemetry buffer is full."));
        assertTrue(output.contains("[WARN] Enter a valid range from 0 to 1."));
        assertTrue(output.contains("[WARN] Enter an index from 0 to 1."));
        assertTrue(output.contains("[WARN] The primary and backup IDs must be registered servers."));
        assertTrue(output.contains("[WARN] Enter a record number from 1 to 6."));
        assertFalse(output.contains("[FAIL]"), "Recoverable validation and empty states are not failures.");
    }

    @Test
    void emptyListingsAndSearchesShowInformativeMessages() {
        String output = runConsole(false, false,
                "1", "2", "0",
                "3", "2", "Missing", "0",
                "3", "3", "0",
                "11", "2", "0",
                "9", "1", "0",
                "0");

        assertTrue(output.contains("[INFO] No servers are registered."));
        assertTrue(output.contains("[INFO] No server names start with \"Missing\"."));
        assertTrue(output.contains("[INFO] No failover plan has been created."));
        assertTrue(output.contains("[INFO] The audit log is empty."));
    }

    @Test
    void longTableValuesAreBoundedAndStoredValuesRemainUnchanged() throws Exception {
        String longServerName = "Primary-Server-" + "N".repeat(70);
        String longMetric = "temperature-" + "sensor-".repeat(12);
        String longConfiguration = "Configuration-" + "baseline-".repeat(10);
        ConsoleRun run = runConsoleWithMain(false,
                "1", "1", "2147483647", longServerName, "2", "0",
                "2", "3", "2147483647", "0",
                "8", "1", "2147483647", longMetric, "0.75", "2", "0",
                "10", "1", longConfiguration, "0",
                "9", "1", "0",
                "0");

        assertTrue(run.output.lines().anyMatch(line -> line.contains("SERVER ID") && line.contains("SERVER")));
        assertTrue(run.output.contains("2147483647"));
        assertTrue(run.output.contains("ONLINE"));
        assertTrue(run.output.contains("BFS traversal order"));
        assertTrue(run.output.contains("| METRIC"));
        assertTrue(run.output.contains("..."));
        assertTrue(run.output.contains("Values ending in \"...\" are shortened for display"));
        assertTrue(run.output.lines()
                .filter(line -> line.startsWith("+") || line.startsWith("|"))
                .allMatch(line -> line.length() <= 90), "Table lines should fit a typical terminal width.");

        Field dataCenterField = Main.class.getDeclaredField("dataCenter");
        dataCenterField.setAccessible(true);
        DataCenterManager dataCenter = (DataCenterManager) dataCenterField.get(run.main);
        assertEquals(longServerName, dataCenter.findServer(2147483647).getName());

        Field auditLoggerField = Main.class.getDeclaredField("auditLogger");
        auditLoggerField.setAccessible(true);
        AuditLogger auditLogger = (AuditLogger) auditLoggerField.get(run.main);
        assertTrue(auditLogger.records().stream()
                .anyMatch(record -> record.getDetails().equals("id=2147483647, name=" + longServerName)));
        assertTrue(auditLogger.records().stream()
                .anyMatch(record -> record.getDetails().equals(longConfiguration)));
        assertTrue(auditLogger.records().stream()
                .anyMatch(record -> record.getDetails().equals("server=2147483647, metric=" + longMetric)));
    }

    @Test
    void topologyShowsEverySeededNodeAndUndirectedConnectionOnce() {
        String output = runConsole(true, false, "2", "4", "0", "0");

        assertTrue(output.contains("Connectivity: CONNECTED"));
        assertTrue(output.contains("Network nodes: 8 | Connections: 7"));
        assertTrue(output.contains("Registered servers: 8 ONLINE | 0 OFFLINE"));
        assertTrue(output.contains("This view reports the current graph and does not run a failure simulation or BFS."));

        List<String> expectedNames = List.of("Edge Gateway", "API Cluster A", "API Cluster B",
                "Customer Database", "Cache Cluster", "Authentication Service", "Analytics Worker",
                "Monitoring Node");
        List<String> nodeRows = topologyNodeRows(output);
        assertEquals(8, nodeRows.size());
        List<String> displayedNames = nodeRows.stream().map(MainConsoleTest::tableCells)
                .map(cells -> cells[2]).toList();
        assertTrue(displayedNames.containsAll(expectedNames));

        List<String> expectedConnections = List.of(
                "101-102", "101-103", "101-108", "102-104", "102-105", "103-106", "103-107");
        List<String> displayedConnections = topologyConnectionRows(output).stream()
                .map(MainConsoleTest::tableCells)
                .map(cells -> Integer.parseInt(cells[1]) + "-" + Integer.parseInt(cells[3]))
                .toList();
        assertEquals(expectedConnections, displayedConnections);
        assertEquals(7, displayedConnections.size());
    }

    @Test
    void repeatedTopologyViewsDoNotChangeTheGraph() {
        String output = runConsole(true, false, "2", "4", "4", "0", "0");

        assertEquals(2, occurrences(output, "Network nodes: 8 | Connections: 7"));
        assertEquals(2, occurrences(output, "[OK] Network connections:"));
        assertEquals(14, allTopologyConnectionRows(output).size());
    }

    @Test
    void topologyReflectsNewDisconnectedConnectedAndOfflineServers() {
        String output = runConsole(true, false,
                "1", "1", "109", "Isolated Worker", "0",
                "2", "4", "1", "109", "101", "4", "0",
                "4", "109", "2", "55", "repair worker",
                "2", "4", "0", "0");

        assertTrue(output.contains("Network nodes: 9 | Connections: 7"));
        assertTrue(output.contains("Connectivity: DISCONNECTED"));
        assertTrue(output.contains("Network nodes: 9 | Connections: 8"));
        assertTrue(output.contains("Connectivity: CONNECTED"));
        assertTrue(output.contains("Registered servers: 8 ONLINE | 1 OFFLINE"));
        assertTrue(topologyNodeRows(output).stream()
                .map(MainConsoleTest::tableCells)
                .anyMatch(cells -> cells[1].equals("109")
                        && cells[2].equals("Isolated Worker")
                        && cells[4].equals("(none)")));
        assertTrue(allTopologyNodeRows(output).stream()
                .map(MainConsoleTest::tableCells)
                .anyMatch(cells -> cells[1].equals("109") && cells[3].equals("OFFLINE")));
        assertTrue(topologyConnectionRows(output).stream()
                .map(MainConsoleTest::tableCells)
                .anyMatch(cells -> cells[1].equals("101") && cells[3].equals("109")));
    }

    @Test
    void emptyTopologyReportsNoNodesOrConnections() {
        String output = runConsole(false, false, "2", "4", "0", "0");

        assertTrue(output.contains("Connectivity: DISCONNECTED"));
        assertTrue(output.contains("Network nodes: 0 | Connections: 0"));
        assertTrue(output.contains("Registered servers: 0 ONLINE | 0 OFFLINE"));
        assertTrue(output.contains("[INFO] No network nodes are registered."));
        assertTrue(output.contains("[INFO] No network connections are present."));
    }

    @Test
    void longTopologyNamesStayBoundedAndRemainStored() throws Exception {
        String longName = "Long-Server-" + "X".repeat(80);
        ConsoleRun run = runConsoleWithMain(false,
                "1", "1", "600", longName, "1", "601", "Neighbor", "0",
                "2", "1", "600", "601", "4", "0", "0");

        assertTrue(run.output.contains("Network nodes: 2 | Connections: 1"));
        assertTrue(run.output.contains("..."));
        assertTrue(run.output.lines()
                .filter(line -> line.startsWith("+") || line.startsWith("|"))
                .allMatch(line -> line.length() <= 90), "Topology table lines should fit a typical terminal width.");

        Field dataCenterField = Main.class.getDeclaredField("dataCenter");
        dataCenterField.setAccessible(true);
        DataCenterManager dataCenter = (DataCenterManager) dataCenterField.get(run.main);
        assertEquals(longName, dataCenter.findServer(600).getName());
    }

    @Test
    void longNeighborListsShowAnExplicitMarkerWithoutSplittingIds() {
        List<String> input = new java.util.ArrayList<>();
        input.addAll(List.of("1", "1", "1000", "Hub"));
        for (int serverId = 1001; serverId <= 1006; serverId++) {
            input.addAll(List.of("1", String.valueOf(serverId), "Node " + serverId));
        }
        input.addAll(List.of("0", "2"));
        for (int serverId = 1001; serverId <= 1006; serverId++) {
            input.addAll(List.of("1", "1000", String.valueOf(serverId)));
        }
        input.addAll(List.of("4", "0", "0"));

        String output = runConsole(false, false, input.toArray(String[]::new));

        assertTrue(output.contains("Network nodes: 7 | Connections: 6"));
        assertTrue(output.contains("1001, 1002, 1003, 1004, ..."));
        assertTrue(output.contains("[INFO] Neighbor IDs followed by \"...\" have additional connections not shown."));
        assertFalse(output.contains("1001, 1002, 1003, 1004, 1005"),
                "The omitted list must not look complete or split a later ID.");
    }

    @Test
    void menuNavigationShowsLiveMetricsAndAlignedLists() {
        String output = runConsole(
                "1", "1", "7", "Database", "1", "12", "API", "2", "0",
                "2", "1", "7", "12", "2", "7", "12", "3", "7", "0",
                "3", "1", "7", "2", "API", "3", "0",
                "4", "7", "2", "30", "repair database",
                "5", "6",
                "7", "1", "rotate logs", "3", "2", "0",
                "8", "1", "7", "cpu", "0.5", "2", "1", "7", "memory", "0.7",
                "3", "2", "10", "20", "4", "0", "1", "5", "1", "15", "0",
                "9", "1", "2", "1", "", "", "0",
                "10", "1", "High Availability", "0", "10", "2", "0",
                "11", "1", "7", "12", "0", "2", "0",
                "12", "0");

        assertTrue(output.contains("2 total"));
        assertTrue(output.contains("2 online"));
        assertTrue(output.contains("1 offline"));
        assertTrue(output.contains("1 links"));
        assertTrue(output.contains("Disconnected"));
        assertTrue(output.contains("0 links"), "Registering servers must not add network connections.");
        assertTrue(output.contains("SYSTEM STATUS: DEGRADED"));
        assertTrue(output.contains("Status details: 1 server(s) offline"));
        assertTrue(output.contains("Incidents           1 pending"));
        assertTrue(output.contains("Recovery tasks      1 dispatched"));
        assertTrue(output.contains("Routine jobs        0 waiting"));
        assertTrue(output.contains("Telemetry           1/5 events buffered"));
        assertTrue(output.contains("Load samples        2 recorded"));
        assertTrue(output.contains("Audit records       10 entries"));
        assertTrue(output.contains("Configuration       High Availability"));
        assertTrue(output.contains("Configuration       Standard"));
        assertTrue(output.contains("SYSTEM STATUS: OPERATIONAL"));
        assertTrue(output.contains("| SERVER ID | SERVER   | STATUS |"));
        assertTrue(output.contains("BFS traversal order"));
        assertTrue(output.indexOf("Database | ONLINE") < output.indexOf("API      | ONLINE"),
                "Reachability rows should follow the graph's BFS order.");
        assertTrue(output.contains("| SEQ | ACTION"));
        assertTrue(output.contains("SERVER_REGISTERED"));
        assertTrue(output.contains("[OK] Found server 7 - Database"));
        assertTrue(output.contains("[OK] Matching server names:"));
        assertTrue(output.contains("| api"));
        assertTrue(output.contains("[OK] Server IDs in ascending order:"));
        assertTrue(output.contains("| 7         |"));
        assertTrue(output.contains("| 12        |"));
        assertTrue(output.contains("[OK] Connected server 7 and server 12."));
        assertTrue(output.contains("[OK] Servers 7 and 12 are connected."));
        assertTrue(output.contains("[OK] Dispatched incident 1 for server 7 (severity 2, deadline 30)."));
        assertTrue(output.contains("[INFO] Reachable network context (only server 7 was marked OFFLINE):"));
        assertTrue(output.contains("[OK] Job 1 added to the routine queue: rotate logs"));
        assertTrue(output.contains("[OK] Completed job 1: rotate logs"));
        assertTrue(output.contains("[OK] Telemetry recorded: server 7, cpu = 0.5."));
        assertTrue(output.contains("[OK] Oldest telemetry event:"));
        assertTrue(output.contains("| 7         | cpu"));
        assertTrue(output.contains("[OK] Sum of load samples 0 through 1: 30"));
        assertTrue(output.contains("[OK] Updated load sample 1."));
        assertTrue(output.contains("[OK] Record matches the current in-memory Merkle root."));
        assertTrue(output.contains("[OK] Current in-memory Merkle root:"));
        assertFalse(output.contains("trusted Merkle root"));
        assertTrue(output.contains("| PRIMARY | 7         | Database | ONLINE"));
        assertTrue(output.contains("| BACKUP  | 12        | API      | ONLINE"));
        assertTrue(output.contains("Preorder: [7, 12]"));
        assertTrue(output.contains("Enter the new configuration name:"));
        assertTrue(output.contains("[OK] Configuration changed to \"High Availability\"."));
        assertTrue(output.contains("[OK] Restored configuration: \"Standard\"."));
        assertTrue(output.contains("SERVER MANAGEMENT"));
        assertTrue(output.contains("INVENTORY SEARCH"));
        assertTrue(output.contains("Register a server  [Linked List, Hash Table, BST, Trie, Graph]"));
        assertTrue(output.contains("Find a server by ID  [Hash Table]"));
        assertTrue(output.contains("Find server names by prefix  [Trie]"));
        assertTrue(output.contains("ascending order  [BST]"));
        assertTrue(output.contains("Connect two servers  [Graph, DSU]"));
        assertTrue(output.contains("List reachable servers  [BFS]"));
        assertTrue(output.contains("Process the next job  [FIFO queue]"));
        assertTrue(output.contains("Undo the most recent change  [Stack]"));
        assertTrue(output.contains("Record telemetry  [Circular queue]"));
        assertTrue(output.contains("Sum a range of load samples  [Segment tree]"));
        assertTrue(output.contains("Create or replace a plan  [Binary tree]"));
        assertTrue(output.contains("Verify a record  [Merkle proof]"));
        assertTrue(output.contains("DSA: indexes update and the server becomes a graph node, with no links added."));
        assertTrue(output.contains("[OK] Server 7 is now OFFLINE; incident"));
        assertTrue(output.contains("[OK] Recovery task 1 completed: server 7 is ONLINE"));
        assertTrue(output.contains("DSA: the graph stores the link; DSU updates connectivity groups."));
        assertTrue(output.contains("DSA: DSU checks whether both IDs share a connectivity set."));
        assertTrue(output.contains("DSA: BFS shows graph reachability; severity 1 enters the deque front"));
        assertTrue(output.contains("DSA: dispatch removes the deque front and inserts that task into the min-heap."));
        assertTrue(output.contains("DSA: the min-heap orders lower severity, earlier deadlines, then lower task IDs."));
        assertTrue(output.contains("DSA: the queue preserves FIFO processing order."));
        assertTrue(output.contains("DSA: the stack restores the most recent saved value first (LIFO)."));
        assertTrue(output.contains("DSA: the segment tree answers this range-sum query."));
        assertTrue(output.contains("DSA: binary tree preorder visits the primary before its backup nodes."));
        assertTrue(output.contains("DSA: a Merkle proof checks the record against the current in-memory log root."));
        assertTrue(output.contains("BLACKOUT closed."));
    }

    @Test
    void guidedDemonstrationReportsOutageAndReturnsWithoutChangingConsoleState() {
        String output = runConsole("1", "1", "7", "Operator server", "0", "12", "1", "0");

        assertTrue(output.contains("Deterministic simulation using separate state"));
        assertTrue(output.contains("Your current console servers, queues, telemetry, and audit log are not changed."));
        assertTrue(output.contains("CRITICAL CUSTOMER DATABASE OUTAGE"));
        assertTrue(output.contains("Customer database (customer records) is reported failed by the operator."));
        assertTrue(output.contains("Reachable means connected in the graph, not confirmed failed."));
        assertTrue(output.contains("Only Customer database is marked offline."));
        assertTrue(output.contains("custom min-heap compares severity, then deadline, then task ID"));
        assertTrue(output.contains("Final report: PASS"));
        assertTrue(output.contains("Customer database - ONLINE"));
        assertTrue(output.contains("Enter your choice:"));
        assertTrue(output.contains("BLACKOUT closed."));

        assertTrue(occurrences(output, "1 total") >= 2
                        && occurrences(output, "1 online") >= 2
                        && occurrences(output, "0 offline") >= 2,
                "The console server should remain registered and online before and after the isolated story.");
        assertTrue(occurrences(output, "Audit records       1 entries") == 2,
                "The guided story should not append events to the console audit log.");
    }

    @Test
    void incidentSchedulingDemonstrationIsIsolatedAndReturnsToMainMenu() {
        String output = runConsole(true, false, "12", "2", "0");

        assertTrue(output.contains("Critical customer database outage"));
        assertTrue(output.contains("Incident deque and recovery heap"));
        assertTrue(output.contains("Deterministic simulation using separate state"));
        assertTrue(output.contains("Your current console servers, queues, telemetry, and audit log are not changed."));
        assertTrue(output.contains("Observed deque dispatch order: Customer database -> Banking API -> Authentication service -> Analytics platform"));
        assertTrue(output.contains("Observed heap processing order: Customer database -> Authentication service -> Analytics platform -> Banking API"));
        assertTrue(output.contains("Processed heap-selected task 3 for Authentication service"));
        assertTrue(output.contains("Processed heap-selected task 4 for Analytics platform"));
        assertTrue(output.contains("All processed recoveries succeeded: yes."));
        assertTrue(output.contains("Final report: PASS"));
        assertTrue(output.contains("Enter your choice:"));
        assertTrue(output.contains("BLACKOUT closed."));

        assertEquals(2, occurrences(output, "SYSTEM STATUS: OPERATIONAL"));
        assertEquals(2, occurrences(output, "8 total"));
        assertEquals(2, occurrences(output, "Audit records       24 entries"));
        assertEquals(2, occurrences(output, "5 waiting"));
    }

    private String runConsole(String... inputLines) {
        return runConsole(false, false, inputLines);
    }

    private String runConsole(boolean initializeDemo, boolean useDefaultConstructor, String... inputLines) {
        InputStream originalInput = System.in;
        PrintStream originalOutput = System.out;
        ByteArrayOutputStream capturedOutput = new ByteArrayOutputStream();
        String input = String.join("\n", inputLines) + "\n";

        try (PrintStream replacementOutput = new PrintStream(capturedOutput, true, StandardCharsets.UTF_8)) {
            System.setIn(new ByteArrayInputStream(input.getBytes(StandardCharsets.UTF_8)));
            System.setOut(replacementOutput);
            if (useDefaultConstructor) {
                Main.main(new String[0]);
            } else {
                new Main(initializeDemo).run();
            }
        } finally {
            System.setIn(originalInput);
            System.setOut(originalOutput);
        }
        return capturedOutput.toString(StandardCharsets.UTF_8);
    }

    private String runConsoleRaw(String input) {
        InputStream originalInput = System.in;
        PrintStream originalOutput = System.out;
        ByteArrayOutputStream capturedOutput = new ByteArrayOutputStream();

        try (PrintStream replacementOutput = new PrintStream(capturedOutput, true, StandardCharsets.UTF_8)) {
            System.setIn(new ByteArrayInputStream(input.getBytes(StandardCharsets.UTF_8)));
            System.setOut(replacementOutput);
            new Main(false).run();
        } finally {
            System.setIn(originalInput);
            System.setOut(originalOutput);
        }
        return capturedOutput.toString(StandardCharsets.UTF_8);
    }

    private ConsoleRun runConsoleWithMain(boolean initializeDemo, String... inputLines) {
        InputStream originalInput = System.in;
        PrintStream originalOutput = System.out;
        ByteArrayOutputStream capturedOutput = new ByteArrayOutputStream();
        String input = String.join("\n", inputLines) + "\n";
        Main main;

        try (PrintStream replacementOutput = new PrintStream(capturedOutput, true, StandardCharsets.UTF_8)) {
            System.setIn(new ByteArrayInputStream(input.getBytes(StandardCharsets.UTF_8)));
            System.setOut(replacementOutput);
            main = new Main(initializeDemo);
            main.run();
        } finally {
            System.setIn(originalInput);
            System.setOut(originalOutput);
        }
        return new ConsoleRun(main, capturedOutput.toString(StandardCharsets.UTF_8));
    }

    private static class ConsoleRun {
        private final Main main;
        private final String output;

        private ConsoleRun(Main main, String output) {
            this.main = main;
            this.output = output;
        }
    }

    private long auditCount(AuditLogger audit, String action) {
        return audit.records().stream().map(AuditRecord::getAction).filter(action::equals).count();
    }

    private int occurrences(String text, String value) {
        int count = 0;
        int position = 0;
        while ((position = text.indexOf(value, position)) >= 0) {
            count++;
            position += value.length();
        }
        return count;
    }

    private static List<String> topologyNodeRows(String output) {
        List<String> rows = new java.util.ArrayList<>();
        boolean inNodeTable = false;
        for (String line : output.lines().toList()) {
            if (line.startsWith("[OK] Network nodes:")) {
                inNodeTable = true;
            } else if (line.startsWith("[OK] Network connections:")
                    || line.startsWith("[INFO] No network connections are present.")) {
                inNodeTable = false;
            } else if (inNodeTable && line.startsWith("|") && !line.contains("SERVER ID")) {
                rows.add(line);
            }
        }
        return rows;
    }

    private static List<String> allTopologyNodeRows(String output) {
        return topologyNodeRows(output);
    }

    private static List<String> topologyConnectionRows(String output) {
        List<String> rows = new java.util.ArrayList<>();
        boolean inConnectionTable = false;
        for (String line : output.lines().toList()) {
            if (line.startsWith("[OK] Network connections:")) {
                inConnectionTable = true;
            } else if (line.startsWith("[OK] Network nodes:")
                    || line.startsWith("[INFO] No network connections are present.")
                    || line.startsWith("DSA: The graph stores")) {
                inConnectionTable = false;
            } else if (inConnectionTable && line.startsWith("|") && !line.contains("SERVER A ID")) {
                rows.add(line);
            }
        }
        return rows;
    }

    private static List<String> allTopologyConnectionRows(String output) {
        return topologyConnectionRows(output);
    }

    private static String[] tableCells(String row) {
        return java.util.Arrays.stream(row.split("\\|"))
                .map(String::trim)
                .toArray(String[]::new);
    }
}
