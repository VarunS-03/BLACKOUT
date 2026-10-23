package blackout.cli;

import blackout.model.AuditRecord;
import blackout.model.RecoveryTask;
import blackout.model.Server;
import blackout.service.RecoveryOrchestrator;
import blackout.structures.LRUCache;
import blackout.structures.MerkleTree;
import blackout.structures.Trie;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** Runs deterministic demonstrations and keeps their narration separate from simulator services. */
public class StoryRunner {
    private final PrintStream output;

    public StoryRunner(PrintStream output) {
        this.output = Objects.requireNonNull(output);
    }

    public record ScenarioResult(
            String name,
            boolean passed,
            Map<String, Boolean> serverOnline,
            List<String> impactedServers,
            List<String> recoveryOrder,
            int pendingTasks,
            int auditRecordCount) {
        public ScenarioResult {
            serverOnline = Collections.unmodifiableMap(new LinkedHashMap<>(serverOnline));
            impactedServers = List.copyOf(impactedServers);
            recoveryOrder = List.copyOf(recoveryOrder);
        }
    }

    private record ServerProfile(Server server, String role) { }
    private record Connection(int firstId, int secondId) { }
    private record Center(RecoveryOrchestrator simulator, List<ServerProfile> servers,
                          List<Connection> connections) { }
    private record IntegrityResult(boolean cleanRecordAccepted, boolean alteredRecordRejected) { }

    public List<ScenarioResult> runAll() {
        List<ScenarioResult> results = new ArrayList<>();
        results.add(runHealthyDataCenter());
        results.add(runCriticalOutage());
        results.add(runMultipleIncidents());
        results.addAll(runEdgeCases());
        results.add(runIntegrity());
        results.add(runCacheAndDiscovery());

        int passed = 0;
        for (ScenarioResult result : results) if (result.passed()) passed++;
        output.println("\n============================================================");
        output.println("STORY SUMMARY");
        output.println("Scenarios verified: " + passed + " of " + results.size());
        output.println("These demonstrations model explicit failures and simulated recovery state changes.");
        output.println("They do not model automatic detection, directed dependencies, or real repair work.");
        return List.copyOf(results);
    }

    public ScenarioResult runHealthyDataCenter() {
        begin("Scenario A - A healthy data center");
        Center center = createCenter(201);
        output.println("The network starts in normal operation. Each connection is an undirected reachability link.");
        printServers(center.servers());
        printConnections(center);
        boolean allOnline = allOnline(center.servers());
        boolean noPendingTasks = center.simulator().pendingRecoveries() == 0;
        output.println("No incident is active; the recovery queue is empty.");
        ScenarioResult result = resultForCenter("healthy", allOnline && noPendingTasks, center,
                List.of(), List.of());
        report(result);
        return result;
    }

    public ScenarioResult runCriticalOutage() {
        begin("Scenario B - Critical customer database outage");
        Center center = createCenter(201);
        printServers(center.servers());
        printConnections(center);

        ServerProfile failed = center.servers().get(1);
        output.println("\n1. Incident detected");
        output.println(failed.server().getName() + " (" + failed.role() + ") is reported failed by the operator.");
        output.println("The simulator records an explicit failure request; it does not detect failures automatically.");

        List<Integer> impactedIds = center.simulator().simulateFailure(failed.server().getId(), 1, 1000);
        List<String> impactedNames = namesForIds(center.servers(), impactedIds);
        boolean onlyTargetOffline = !failed.server().isOnline();
        for (ServerProfile profile : center.servers()) {
            if (profile.server() != failed.server() && !profile.server().isOnline()) onlyTargetOffline = false;
        }
        output.println("\n2. Impact assessment");
        output.println("Graph-reachable impact context: " + String.join(", ", impactedNames) + ".");
        output.println("Reachable means connected in the graph, not confirmed failed. Only "
                + failed.server().getName() + " is marked offline.");
        output.println("Observed state matches that distinction: " + yesNo(onlyTargetOffline) + ".");
        printStates(center.servers());

        output.println("\n3. Recovery plan");
        output.println("Task severity: 1 (the scheduler treats lower numbers as more urgent).");
        output.println("Deadline value: 1000. The project does not define its unit; it breaks ties after severity.");
        output.println("This is the only pending task, so it is selected next. The custom min-heap compares severity, then deadline, then task ID.");

        output.println("\n4. Recovery execution");
        RecoveryTask task = center.simulator().processNextRecovery();
        boolean recoveredCorrectServer = task != null && task.getServerId() == failed.server().getId()
                && failed.server().isOnline();
        output.println(recoveredCorrectServer
                ? failed.server().getName() + " is online again after its scheduled task was processed."
                : "Recovery did not restore the expected server.");
        printStates(center.servers());

        output.println("\n5. Audit integrity");
        IntegrityResult integrity = verifyAudit(center.simulator());
        printIntegrity(integrity);

        ScenarioResult result = resultForCenter("critical outage",
                onlyTargetOffline && recoveredCorrectServer && allOnline(center.servers())
                        && integrity.cleanRecordAccepted() && integrity.alteredRecordRejected(),
                center, impactedNames, task == null ? List.of() : List.of(failed.server().getName()));
        report(result);
        return result;
    }

    public ScenarioResult runMultipleIncidents() {
        begin("Scenario C - Multiple incidents and scheduler ordering");
        Center center = createCenter(211);
        printServers(center.servers());
        printConnections(center);
        output.println("Four independent failure requests are entered. Connected servers are impact context only.");

        int[] severities = {2, 1, 2, 2};
        long[] deadlines = {500, 900, 300, 300};
        for (int i = 0; i < center.servers().size(); i++) {
            Server server = center.servers().get(i).server();
            List<Integer> impactedIds = center.simulator().simulateFailure(
                    server.getId(), severities[i], deadlines[i]);
            output.println("Scheduled " + server.getName() + ": severity " + severities[i]
                    + ", deadline value " + deadlines[i] + ". Reachable impact context: "
                    + String.join(", ", namesForIds(center.servers(), impactedIds)) + ".");
        }
        boolean allRequestedServersOffline = true;
        for (ServerProfile profile : center.servers()) {
            if (profile.server().isOnline()) allRequestedServersOffline = false;
        }

        output.println("\nThe scheduler chooses lower severity numbers first. For equal severity it chooses the lower deadline value, then the lower task ID.");
        List<String> recoveryOrder = new ArrayList<>();
        while (center.simulator().pendingRecoveries() > 0) {
            RecoveryTask task = center.simulator().processNextRecovery();
            ServerProfile profile = profileForId(center.servers(), task.getServerId());
            recoveryOrder.add(profile.server().getName());
            output.println("Processed task " + task.getId() + " for " + profile.server().getName()
                    + " (severity " + task.getSeverity() + ", deadline " + task.getDeadline() + ").");
        }
        List<String> expectedOrder = List.of("Customer database", "Authentication service",
                "Analytics platform", "Banking API");
        boolean taskOrderValid = recoveryOrder.equals(expectedOrder);
        boolean restored = allOnline(center.servers());
        output.println("All four requested targets were offline before processing: "
                + yesNo(allRequestedServersOffline) + ".");
        output.println("\nScheduler order matches the documented comparison rules: " + yesNo(taskOrderValid) + ".");
        output.println("All four explicitly failed servers returned online: " + yesNo(restored) + ".");
        IntegrityResult integrity = verifyAudit(center.simulator());
        printIntegrity(integrity);

        ScenarioResult result = resultForCenter("multiple incidents",
                allRequestedServersOffline && taskOrderValid && restored && integrity.cleanRecordAccepted()
                        && integrity.alteredRecordRejected(), center, List.of(), recoveryOrder);
        report(result);
        return result;
    }

    public List<ScenarioResult> runEdgeCases() {
        List<ScenarioResult> results = new ArrayList<>();
        results.add(runIsolatedServer());
        results.add(runUnknownServerIncident());
        results.add(runDuplicateIncident());
        results.add(runEmptyRecoveryQueue());
        return List.copyOf(results);
    }

    public ScenarioResult runIsolatedServer() {
        begin("Scenario D - An isolated server");
        RecoveryOrchestrator simulator = new RecoveryOrchestrator();
        Server isolated = new Server(301, "Isolated reporting node");
        simulator.registerServer(isolated);
        output.println("The server has no graph connections. Its only reachable node should be itself.");
        List<Integer> impactedIds = simulator.simulateFailure(isolated.getId(), 2, 400);
        List<String> impactedNames = List.of(isolated.getName());
        boolean impactIsOnlyServer = impactedIds.equals(List.of(isolated.getId()));
        boolean isolatedServerWentOffline = !isolated.isOnline();
        output.println("Impact assessment: " + String.join(", ", impactedNames) + ".");
        output.println("Isolated impact result is correct: " + yesNo(impactIsOnlyServer) + ".");
        RecoveryTask task = simulator.processNextRecovery();
        boolean recovered = task != null && isolated.isOnline();
        output.println("The isolated server returned online after its own task: " + yesNo(recovered) + ".");
        ScenarioResult result = resultForServers("isolated server",
                impactIsOnlyServer && isolatedServerWentOffline && recovered,
                simulator, List.of(isolated), impactedNames, List.of(isolated.getName()));
        report(result);
        return result;
    }

    public ScenarioResult runUnknownServerIncident() {
        begin("Scenario E - An invalid incident");
        RecoveryOrchestrator simulator = new RecoveryOrchestrator();
        int auditBefore = simulator.auditRecords().size();
        boolean rejected = false;
        try {
            simulator.simulateFailure(999, 1, 100);
        } catch (IllegalArgumentException expected) {
            rejected = true;
        }
        boolean unchanged = simulator.pendingRecoveries() == 0
                && simulator.auditRecords().size() == auditBefore;
        output.println("No servers are registered. An unknown server ID is rejected as expected.");
        output.println("No recovery task or audit event was created: " + yesNo(unchanged) + ".");
        ScenarioResult result = resultForServers("unknown server", rejected && unchanged,
                simulator, List.of(), List.of(), List.of());
        report(result);
        return result;
    }

    public ScenarioResult runDuplicateIncident() {
        begin("Scenario F - A duplicate incident");
        RecoveryOrchestrator simulator = new RecoveryOrchestrator();
        Server server = new Server(302, "Notification service");
        simulator.registerServer(server);
        output.println("Initial state: " + server.getName() + " is ONLINE with no recovery task pending.");
        simulator.simulateFailure(server.getId(), 1, 200);
        int tasksBefore = simulator.pendingRecoveries();
        int recordsBefore = simulator.auditRecords().size();
        boolean rejected = false;
        try {
            simulator.simulateFailure(server.getId(), 1, 200);
        } catch (IllegalStateException expected) {
            rejected = true;
        }
        boolean unchanged = simulator.pendingRecoveries() == tasksBefore
                && simulator.auditRecords().size() == recordsBefore;
        output.println("The already-offline server rejected a duplicate failure request.");
        output.println("Existing task retained; no duplicate task or audit event added: " + yesNo(unchanged) + ".");
        RecoveryTask task = simulator.processNextRecovery();
        boolean recovered = task != null && server.isOnline();
        ScenarioResult result = resultForServers("duplicate incident", rejected && unchanged && recovered,
                simulator, List.of(server), List.of(server.getName()), List.of(server.getName()));
        report(result);
        return result;
    }

    public ScenarioResult runEmptyRecoveryQueue() {
        begin("Scenario G - An empty recovery queue");
        RecoveryOrchestrator simulator = new RecoveryOrchestrator();
        RecoveryTask task = simulator.processNextRecovery();
        boolean emptyHandled = task == null && simulator.pendingRecoveries() == 0;
        output.println("No task was pending. The scheduler returned no work without an error.");
        ScenarioResult result = resultForServers("empty recovery queue", emptyHandled,
                simulator, List.of(), List.of(), List.of());
        report(result);
        return result;
    }

    public List<ScenarioResult> runIntegrityAndDiscovery() {
        return List.of(runIntegrity(), runCacheAndDiscovery());
    }

    public ScenarioResult runIntegrity() {
        begin("Scenario H - Audit integrity check");
        Center center = createCenter(401);
        output.println("A legitimate failure and recovery create audit records from real state transitions.");
        center.simulator().simulateFailure(center.servers().get(0).server().getId(), 1, 50);
        center.simulator().processNextRecovery();

        IntegrityResult integrity = verifyAudit(center.simulator());
        printIntegrity(integrity);
        output.println("The changed text is tested against the original trusted root; the stored audit history is not modified.");

        ScenarioResult result = resultForCenter("audit integrity",
                integrity.cleanRecordAccepted() && integrity.alteredRecordRejected(),
                center, List.of(), List.of());
        report(result);
        return result;
    }

    public ScenarioResult runCacheAndDiscovery() {
        begin("Scenario I - Cache and service discovery");
        LRUCache<String, String> cache = new LRUCache<>(2);
        output.println("A two-entry cache starts with customer database and authentication service lookups.");
        cache.put("Customer database", "connection details");
        cache.put("Authentication service", "connection details");
        cache.get("Customer database");
        output.println("The customer database entry is accessed again; analytics is then added while the cache is full.");
        cache.put("Analytics platform", "connection details");
        boolean leastRecentEvicted = !cache.containsKey("Authentication service")
                && cache.containsKey("Customer database") && cache.containsKey("Analytics platform");
        output.println("The cache reached capacity. After the database was accessed again, the older authentication entry was evicted to make room for analytics.");
        output.println("LRU eviction behaved as expected: " + yesNo(leastRecentEvicted) + ".");

        output.println("\nService-name search");
        Trie services = new Trie();
        services.insert("banking-api");
        services.insert("customer-database");
        services.insert("authentication-service");
        services.insert("analytics-platform");
        List<String> authMatches = services.wordsWithPrefix("auth");
        List<String> missingMatches = services.wordsWithPrefix("billing");
        List<String> allServices = services.wordsWithPrefix("");
        boolean trieResultsValid = authMatches.equals(List.of("authentication-service"))
                && missingMatches.isEmpty() && allServices.size() == 4;
        output.println("Prefix 'auth' finds: " + String.join(", ", authMatches) + ".");
        output.println("Prefix 'billing' finds no services: " + yesNo(missingMatches.isEmpty()) + ". An empty prefix lists all "
                + allServices.size() + " registered names.");

        ScenarioResult result = resultForServers("cache and discovery",
                leastRecentEvicted && trieResultsValid, new RecoveryOrchestrator(),
                List.of(), List.of(), List.of());
        report(result);
        return result;
    }

    private Center createCenter(int firstId) {
        RecoveryOrchestrator simulator = new RecoveryOrchestrator();
        List<ServerProfile> servers = List.of(
                new ServerProfile(new Server(firstId, "Banking API"), "payment requests"),
                new ServerProfile(new Server(firstId + 1, "Customer database"), "customer records"),
                new ServerProfile(new Server(firstId + 2, "Authentication service"), "sign-in requests"),
                new ServerProfile(new Server(firstId + 3, "Analytics platform"), "reporting workloads"));
        for (ServerProfile profile : servers) simulator.registerServer(profile.server());

        List<Connection> connections = List.of(
                new Connection(firstId, firstId + 1),
                new Connection(firstId, firstId + 2),
                new Connection(firstId + 1, firstId + 3));
        for (Connection connection : connections) {
            simulator.connectServers(connection.firstId(), connection.secondId());
        }
        return new Center(simulator, servers, connections);
    }

    private IntegrityResult verifyAudit(RecoveryOrchestrator simulator) {
        List<AuditRecord> auditRecords = simulator.auditRecords();
        List<String> canonicalRecords = new ArrayList<>();
        for (AuditRecord record : auditRecords) canonicalRecords.add(record.canonicalForm());
        MerkleTree tree = new MerkleTree(canonicalRecords);
        int index = canonicalRecords.size() - 1;
        String trustedRoot = simulator.auditMerkleRoot();
        boolean cleanRecordAccepted = tree.verify(canonicalRecords.get(index), index,
                tree.proof(index), trustedRoot);
        boolean alteredRecordRejected = !tree.verify(canonicalRecords.get(index) + "|changed",
                index, tree.proof(index), trustedRoot);
        return new IntegrityResult(cleanRecordAccepted, alteredRecordRejected);
    }

    private void printIntegrity(IntegrityResult result) {
        output.println("Audit integrity: " + (result.cleanRecordAccepted() ? "VERIFIED" : "FAILED")
                + " - unchanged recorded events match the trusted snapshot.");
        output.println("Controlled alteration rejected: " + yesNo(result.alteredRecordRejected()) + ".");
        output.println("The Merkle tree helps detect changes only when checked against a trusted root.");
    }

    private ScenarioResult resultForCenter(String name, boolean passed, Center center,
                                           List<String> impacted, List<String> recoveryOrder) {
        return resultForProfiles(name, passed, center.simulator(), center.servers(), impacted, recoveryOrder);
    }

    private ScenarioResult resultForProfiles(String name, boolean passed, RecoveryOrchestrator simulator,
                                             List<ServerProfile> servers, List<String> impacted,
                                             List<String> recoveryOrder) {
        List<Server> serverObjects = new ArrayList<>();
        for (ServerProfile profile : servers) serverObjects.add(profile.server());
        return resultForServers(name, passed, simulator, serverObjects, impacted, recoveryOrder);
    }

    private ScenarioResult resultForServers(String name, boolean passed, RecoveryOrchestrator simulator,
                                            List<Server> servers, List<String> impacted,
                                            List<String> recoveryOrder) {
        Map<String, Boolean> states = new LinkedHashMap<>();
        for (Server server : servers) states.put(server.getName(), server.isOnline());
        return new ScenarioResult(name, passed, states, impacted, recoveryOrder,
                simulator.pendingRecoveries(), simulator.auditRecords().size());
    }

    private void begin(String title) {
        output.println("\n============================================================");
        output.println(title.toUpperCase());
        output.println("============================================================");
    }

    private void printServers(List<ServerProfile> servers) {
        output.println("\nNormal operations - registered servers:");
        for (ServerProfile profile : servers) {
            output.println("  " + profile.server().getName() + " [" + profile.server().getId() + "] - "
                    + profile.role() + " - " + status(profile.server()));
        }
    }

    private void printConnections(Center center) {
        output.println("Connections (undirected graph links):");
        for (Connection connection : center.connections()) {
            ServerProfile first = profileForId(center.servers(), connection.firstId());
            ServerProfile second = profileForId(center.servers(), connection.secondId());
            output.println("  " + first.server().getName() + " <-> " + second.server().getName());
        }
    }

    private void printStates(List<ServerProfile> servers) {
        output.println("Current server states:");
        for (ServerProfile profile : servers) {
            output.println("  " + profile.server().getName() + " - " + status(profile.server()));
        }
    }

    private void report(ScenarioResult result) {
        output.println("\nFinal report: " + (result.passed() ? "PASS" : "NEEDS REVIEW"));
        if (!result.serverOnline().isEmpty()) {
            output.println("Server state:");
            for (Map.Entry<String, Boolean> state : result.serverOnline().entrySet()) {
                output.println("  " + state.getKey() + " - " + (state.getValue() ? "ONLINE" : "OFFLINE"));
            }
        }
        if (!result.impactedServers().isEmpty()) {
            output.println("Impact context: " + String.join(", ", result.impactedServers()));
        }
        if (!result.recoveryOrder().isEmpty()) {
            output.println("Recovery order: " + String.join(" -> ", result.recoveryOrder()));
        }
        output.println("Pending tasks: " + result.pendingTasks() + "; audit records: " + result.auditRecordCount());
    }

    private List<String> namesForIds(List<ServerProfile> servers, List<Integer> ids) {
        List<String> names = new ArrayList<>();
        for (int id : ids) names.add(profileForId(servers, id).server().getName());
        return names;
    }

    private ServerProfile profileForId(List<ServerProfile> servers, int id) {
        for (ServerProfile profile : servers) {
            if (profile.server().getId() == id) return profile;
        }
        throw new IllegalArgumentException("No display profile for server " + id);
    }

    private boolean allOnline(List<ServerProfile> servers) {
        for (ServerProfile profile : servers) if (!profile.server().isOnline()) return false;
        return true;
    }

    private String status(Server server) {
        return server.isOnline() ? "ONLINE" : "OFFLINE";
    }

    private String yesNo(boolean value) {
        return value ? "yes" : "no";
    }
}
