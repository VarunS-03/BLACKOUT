package blackout.cli;

import blackout.model.AuditRecord;
import blackout.model.Job;
import blackout.model.RecoveryTask;
import blackout.model.Server;
import blackout.model.TelemetryEvent;
import blackout.service.AuditLogger;
import blackout.service.DataCenterManager;
import blackout.service.FailureAnalysisService;
import blackout.service.RecoveryScheduler;
import blackout.service.TelemetryManager;

import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class Main {
    private final Scanner input = new Scanner(System.in);
    private final DataCenterManager dataCenter = new DataCenterManager();
    private final FailureAnalysisService failureAnalysis = new FailureAnalysisService();
    private final RecoveryScheduler recoveryScheduler = new RecoveryScheduler();
    private final TelemetryManager telemetryManager = new TelemetryManager(5);
    private final AuditLogger auditLogger = new AuditLogger();

    private int nextJobId = 1;
    private int nextRecoveryTaskId = 1;
    private boolean inputClosed;

    Main() {
        this(true);
    }

    Main(boolean initializeDemoData) {
        if (initializeDemoData) {
            nextJobId = DemoEnvironment.initialize(
                    dataCenter, failureAnalysis, recoveryScheduler, telemetryManager, auditLogger);
        }
    }

    public static void main(String[] args) {
        new Main().run();
    }

    void run() {
        boolean running = true;
        while (running && !inputClosed) {
            printMainMenu();
            Integer choice = readInt("Enter your choice: ");
            if (choice == null) {
                if (inputClosed) {
                    break;
                }
                continue;
            }

            switch (choice) {
                case 1 -> manageServers();
                case 2 -> manageNetwork();
                case 3 -> manageInventory();
                case 4 -> reportOutage();
                case 5 -> dispatchNextIncident();
                case 6 -> processNextRecovery();
                case 7 -> manageRoutineJobs();
                case 8 -> manageTelemetry();
                case 9 -> manageAuditLog();
                case 10 -> manageConfiguration();
                case 11 -> manageFailoverPlan();
                case 12 -> runGuidedDemonstration();
                case 0 -> {
                    running = false;
                    System.out.println("BLACKOUT closed.");
                }
                default -> System.out.println("[WARN] Choose a number from 0 to 12.");
            }
        }
        if (inputClosed) {
            System.out.println("BLACKOUT closed.");
        }
    }

    private void printMainMenu() {
        System.out.println();
        System.out.println("============================================================================");
        System.out.printf(" %-43s %s%n", "BLACKOUT", "DATA CENTER OPERATIONS");
        System.out.printf(" %-43s %s%n", "Failure & Recovery Simulator", "Environment: DEMO");
        System.out.println("============================================================================");
        System.out.println(" Deterministic sample data - not live infrastructure monitoring");
        printDashboard();
        System.out.println("----------------------------------------------------------------------------");
        System.out.println("  INFRASTRUCTURE       INCIDENTS & RECOVERY       MONITORING");
        System.out.println("  [1] Servers           [4] Report an outage       [8] Telemetry & load");
        System.out.println("  [2] Network           [5] Dispatch an incident   [9] Audit & integrity");
        System.out.println("  [3] Inventory         [6] Process a recovery");
        System.out.println("                       [7] Routine jobs");
        System.out.println();
        System.out.println("  CONFIGURATION & PLANNING");
        System.out.println("  [10] Configuration   [11] Failover planning");
        System.out.println("  [12] Guided simulation");
        System.out.println();
        System.out.println("  [0] Exit");
        System.out.println("----------------------------------------------------------------------------");
    }

    private void runGuidedDemonstration() {
        boolean selecting = true;
        while (selecting && !inputClosed) {
            printSection("GUIDED SIMULATIONS");
            System.out.println("Deterministic demonstrations use separate state; they are not live monitoring.");
            printMenuOption(1, "Critical customer database outage");
            printMenuOption(2, "Incident deque and recovery heap");
            printMenuOption(0, "Return to the main menu");

            Integer choice = readInt("Choose a demonstration: ");
            if (choice == null) {
                if (inputClosed) return;
                continue;
            }
            switch (choice) {
                case 1 -> {
                    runGuidedScenario("Critical-outage demonstration", false);
                    selecting = false;
                }
                case 2 -> {
                    runGuidedScenario("Incident scheduling demonstration", true);
                    selecting = false;
                }
                case 0 -> selecting = false;
                default -> System.out.println("[WARN] Choose a number from 0 to 2.");
            }
        }
    }

    private void runGuidedScenario(String title, boolean incidentScheduling) {
        printSection(title);
        System.out.println("Deterministic simulation using separate state; this is not live monitoring.");
        System.out.println("Your current console servers, queues, telemetry, and audit log are not changed.");
        try {
            StoryRunner runner = new StoryRunner(System.out);
            StoryRunner.ScenarioResult result = incidentScheduling
                    ? runner.runMultipleIncidents() : runner.runCriticalOutage();
            if (!result.passed()) {
                System.out.println("[WARN] The demonstration completed with a result that needs review.");
            }
        } catch (RuntimeException exception) {
            System.out.println("[FAIL] The demonstration could not complete: " + exception.getMessage());
        }
    }

    private void printDashboard() {
        int onlineServers = 0;
        for (Server server : dataCenter.registeredServers()) {
            if (server.isOnline()) {
                onlineServers++;
            }
        }
        int registeredServers = dataCenter.serverCount();
        int offlineServers = registeredServers - onlineServers;

        int networkNodes = failureAnalysis.serverIds().size();
        int incidents = recoveryScheduler.pendingIncidents();
        int recoveryTasks = recoveryScheduler.pendingTasks();
        boolean networkConnected = networkNodes == registeredServers
                && failureAnalysis.isNetworkConnected();
        String systemStatus = registeredServers == 0 ? "NO DATA"
                : onlineServers == registeredServers && networkConnected
                && incidents == 0 && recoveryTasks == 0 ? "OPERATIONAL" : "DEGRADED";

        System.out.println(" SYSTEM STATUS: " + systemStatus);
        if ("DEGRADED".equals(systemStatus)) {
            List<String> reasons = new ArrayList<>();
            if (offlineServers > 0) {
                reasons.add(offlineServers + " server(s) offline");
            }
            if (networkNodes != registeredServers) {
                reasons.add("network nodes (" + networkNodes + ") differ from registered servers ("
                        + registeredServers + ")");
            } else if (!failureAnalysis.isNetworkConnected()) {
                reasons.add("network has disconnected components");
            }
            if (incidents > 0) {
                reasons.add(incidents + " pending incident(s)");
            }
            if (recoveryTasks > 0) {
                reasons.add(recoveryTasks + " dispatched recovery task(s)");
            }
            System.out.println(" Status details: " + String.join("; ", reasons));
        }
        System.out.println();
        System.out.println(" INFRASTRUCTURE");
        System.out.printf(" Servers             %-10s %d online     %d offline%n",
                registeredServers + " total", onlineServers, offlineServers);
        System.out.printf(" Network             %-10s %d nodes      %d links%n",
                networkConnected ? "Connected" : "Disconnected", networkNodes,
                failureAnalysis.connectionCount());
        System.out.println();
        System.out.println(" OPERATIONS");
        System.out.printf(" Incidents           %d pending%n", incidents);
        System.out.printf(" Recovery tasks      %d dispatched%n", recoveryTasks);
        System.out.printf(" Routine jobs        %d waiting%n", dataCenter.queuedJobs());
        System.out.println();
        System.out.println(" MONITORING");
        System.out.printf(" Telemetry           %d/%d events buffered%n",
                telemetryManager.bufferedEvents(), telemetryManager.eventCapacity());
        System.out.printf(" Load samples        %d recorded%n", telemetryManager.loadSamples().length);
        System.out.printf(" Audit records       %d entries%n", auditLogger.records().size());
        System.out.printf(" Configuration       %s%n", dataCenter.currentConfiguration());
    }

    private void printSection(String title) {
        System.out.println();
        System.out.println("====================================================================");
        System.out.println("  " + title);
        System.out.println("====================================================================");
    }

    private void printMenuOption(int option, String description) {
        System.out.printf("  %2d  %s%n", option, description);
    }

    private void printTable(String[] headings, List<String[]> rows) {
        int[] widths = new int[headings.length];
        String[] displayHeadings = new String[headings.length];
        boolean shortened = false;
        for (int column = 0; column < headings.length; column++) {
            displayHeadings[column] = tableValue(headings[column]);
            widths[column] = Math.min(displayHeadings[column].length(), tableColumnLimit(headings[column]));
            for (String[] row : rows) {
                String value = tableValue(row[column]);
                widths[column] = Math.max(widths[column], Math.min(value.length(), tableColumnLimit(headings[column])));
            }
        }

        printTableRule(widths);
        printTableRow(displayHeadings, widths);
        printTableRule(widths);
        for (String[] row : rows) {
            String[] displayRow = new String[row.length];
            for (int column = 0; column < row.length; column++) {
                String value = tableValue(row[column]);
                displayRow[column] = fitTableValue(value, widths[column]);
                shortened |= displayRow[column].length() < value.length();
            }
            printTableRow(displayRow, widths);
        }
        printTableRule(widths);
        if (shortened) {
            System.out.println("[INFO] Values ending in \"...\" are shortened for display; stored values are unchanged.");
        }
    }

    private int tableColumnLimit(String heading) {
        return switch (heading) {
            case "SERVER ID" -> 11;
            case "SERVER A ID", "SERVER B ID" -> 11;
            case "ORDER" -> 10;
            case "SEQ" -> 20;
            case "STATUS" -> 8;
            case "ACTION" -> 22;
            case "DETAILS" -> 38;
            case "SERVER A", "SERVER B" -> 25;
            case "NEIGHBORS" -> 27;
            case "SERVER", "SERVER NAME", "METRIC" -> 30;
            case "VALUE" -> 12;
            case "ROLE" -> 15;
            default -> Math.max(heading.length(), 30);
        };
    }

    private String tableValue(String value) {
        if (value == null || value.isBlank()) {
            return "(empty)";
        }
        StringBuilder readable = new StringBuilder(value.length());
        for (int index = 0; index < value.length(); index++) {
            char character = value.charAt(index);
            readable.append(Character.isISOControl(character) ? ' ' : character);
        }
        return readable.toString();
    }

    private String fitTableValue(String value, int width) {
        if (value.length() <= width) {
            return value;
        }
        if (width <= 3) {
            return value.substring(0, width);
        }
        return value.substring(0, width - 3) + "...";
    }

    private void printTableRow(String[] values, int[] widths) {
        for (int column = 0; column < values.length; column++) {
            System.out.printf("| %-" + widths[column] + "s ", values[column]);
        }
        System.out.println("|");
    }

    private void printTableRule(int[] widths) {
        for (int width : widths) {
            System.out.print("+-" + "-".repeat(width) + "-");
        }
        System.out.println("+");
    }

    private void manageServers() {
        boolean inMenu = true;
        while (inMenu && !inputClosed) {
            printSection("SERVER MANAGEMENT");
            printMenuOption(1, "Register a server  [Linked List, Hash Table, BST, Trie, Graph]");
            printMenuOption(2, "List in registration order  [Linked List]");
            printMenuOption(0, "Return to the main menu");

            Integer choice = readInt("Choose an operation: ");
            if (choice == null) {
                if (inputClosed) return;
                continue;
            }
            switch (choice) {
                case 1 -> registerServer();
                case 2 -> listServers();
                case 0 -> inMenu = false;
                default -> System.out.println("[WARN] Choose a number from 0 to 2.");
            }
        }
    }

    private void manageInventory() {
        boolean inMenu = true;
        while (inMenu && !inputClosed) {
            printSection("INVENTORY SEARCH");
            printMenuOption(1, "Find a server by ID  [Hash Table]");
            printMenuOption(2, "Find server names by prefix  [Trie]");
            printMenuOption(3, "List server IDs in ascending order  [BST]");
            printMenuOption(0, "Return to the main menu");

            Integer choice = readInt("Choose an operation: ");
            if (choice == null) {
                if (inputClosed) return;
                continue;
            }
            switch (choice) {
                case 1 -> findServer();
                case 2 -> findServerNames();
                case 3 -> listSortedServerIds();
                case 0 -> inMenu = false;
                default -> System.out.println("[WARN] Choose a number from 0 to 3.");
            }
        }
    }

    private void registerServer() {
        Integer id = readInt("Enter the new server ID: ");
        if (id == null) {
            return;
        }
        String name = readNonBlankLine("Enter the server name: ");
        if (name == null) {
            return;
        }

        Server server = new Server(id, name);
        if (!dataCenter.registerServer(server)) {
        System.out.println("[WARN] Server ID " + id + " is already registered.");
            return;
        }
        failureAnalysis.addServer(id);
        auditLogger.log("SERVER_REGISTERED", "id=" + id + ", name=" + name);
        System.out.println("[OK] Registered server " + id + " - " + name + " (ONLINE).");
        System.out.println("DSA: indexes update and the server becomes a graph node, with no links added.");
    }

    private void findServer() {
        Integer id = readInt("Enter the server ID to find: ");
        if (id == null) {
            return;
        }

        Server server = dataCenter.findServer(id);
        if (server == null) {
            System.out.println("[WARN] No server has ID " + id + ".");
            return;
        }
        System.out.println("[OK] Found server " + server.getId() + " - " + server.getName()
                + " (" + onlineStatus(server) + ")");
        System.out.println("DSA: custom hash table searches by server ID.");
    }

    private void findServerNames() {
        String prefix = readNonBlankLine("Enter the beginning of a server name: ");
        if (prefix == null) {
            return;
        }

        List<String> names = dataCenter.serverNamesWithPrefix(prefix);
        if (names.isEmpty()) {
            System.out.println("[INFO] No server names start with \"" + prefix + "\".");
            return;
        }
        System.out.println("[OK] Matching server names:");
        List<String[]> rows = new ArrayList<>();
        for (String name : names) {
            rows.add(new String[]{name});
        }
        printTable(new String[]{"SERVER NAME"}, rows);
        System.out.println("DSA: Trie follows the entered name prefix.");
    }

    private void listServers() {
        List<Server> servers = dataCenter.registeredServers();
        if (servers.isEmpty()) {
            System.out.println("[INFO] No servers are registered.");
            return;
        }
        System.out.println("[OK] Servers in registration order:");
        List<String[]> rows = new ArrayList<>();
        for (Server server : servers) {
            rows.add(new String[]{String.valueOf(server.getId()), server.getName(), onlineStatus(server)});
        }
        printTable(new String[]{"SERVER ID", "SERVER", "STATUS"}, rows);
        System.out.println("DSA: the custom linked list preserves registration order.");
    }

    private void listSortedServerIds() {
        List<Integer> serverIds = dataCenter.serverIdsInOrder();
        if (serverIds.isEmpty()) {
            System.out.println("[INFO] No servers are registered.");
            return;
        }
        System.out.println("[OK] Server IDs in ascending order:");
        List<String[]> rows = new ArrayList<>();
        for (int serverId : serverIds) {
            rows.add(new String[]{String.valueOf(serverId)});
        }
        printTable(new String[]{"SERVER ID"}, rows);
        System.out.println("DSA: inorder traversal of the binary search tree.");
    }

    private void manageNetwork() {
        boolean inMenu = true;
        while (inMenu && !inputClosed) {
            printSection("NETWORK MANAGEMENT");
            printMenuOption(1, "Connect two servers  [Graph, DSU]");
            printMenuOption(2, "Check connectivity  [DSU]");
            printMenuOption(3, "List reachable servers  [BFS]");
            printMenuOption(4, "View network topology  [Graph]");
            printMenuOption(0, "Return to the main menu");

            Integer choice = readInt("Choose an operation: ");
            if (choice == null) {
                if (inputClosed) return;
                continue;
            }
            switch (choice) {
                case 1 -> connectServers();
                case 2 -> checkConnection();
                case 3 -> showReachableServers();
                case 4 -> showNetworkTopology();
                case 0 -> inMenu = false;
                default -> System.out.println("[WARN] Choose a number from 0 to 4.");
            }
        }
    }

    private void showNetworkTopology() {
        printSection("NETWORK TOPOLOGY");

        List<Integer> networkServerIds = new ArrayList<>(failureAnalysis.serverIds());
        networkServerIds.sort(Integer::compareTo);
        int onlineServers = 0;
        int offlineServers = 0;
        for (Server server : dataCenter.registeredServers()) {
            if (server.isOnline()) {
                onlineServers++;
            } else {
                offlineServers++;
            }
        }

        System.out.println("[OK] Current graph state:");
        System.out.println("  Connectivity: "
                + (failureAnalysis.isNetworkConnected() ? "CONNECTED" : "DISCONNECTED"));
        System.out.println("  Network nodes: " + networkServerIds.size()
                + " | Connections: " + failureAnalysis.connectionCount());
        System.out.println("  Registered servers: " + onlineServers + " ONLINE | "
                + offlineServers + " OFFLINE");
        System.out.println("  Graph connectivity and server availability are separate measures.");

        if (networkServerIds.isEmpty()) {
            System.out.println("[INFO] No network nodes are registered.");
        } else {
            System.out.println("[OK] Network nodes:");
            List<String[]> nodeRows = new ArrayList<>();
            for (int serverId : networkServerIds) {
                Server server = dataCenter.findServer(serverId);
                List<Integer> neighborIds = new ArrayList<>(failureAnalysis.neighbors(serverId));
                neighborIds.sort(Integer::compareTo);
                nodeRows.add(new String[]{String.valueOf(serverId), server.getName(),
                        onlineStatus(server), formatNeighborIds(neighborIds)});
            }
            printTable(new String[]{"SERVER ID", "SERVER", "STATUS", "NEIGHBORS"}, nodeRows);
            if (nodeRows.stream().anyMatch(row -> row[3].endsWith("..."))) {
                System.out.println("[INFO] Neighbor IDs followed by \"...\" have additional connections not shown.");
            }
        }

        List<String[]> connectionRows = new ArrayList<>();
        for (int firstServerId : networkServerIds) {
            List<Integer> neighborIds = new ArrayList<>(failureAnalysis.neighbors(firstServerId));
            neighborIds.sort(Integer::compareTo);
            for (int secondServerId : neighborIds) {
                if (firstServerId < secondServerId) {
                    Server firstServer = dataCenter.findServer(firstServerId);
                    Server secondServer = dataCenter.findServer(secondServerId);
                    connectionRows.add(new String[]{String.valueOf(firstServerId), firstServer.getName(),
                            String.valueOf(secondServerId), secondServer.getName()});
                }
            }
        }
        if (connectionRows.isEmpty()) {
            System.out.println("[INFO] No network connections are present.");
        } else {
            System.out.println("[OK] Network connections:");
            printTable(new String[]{"SERVER A ID", "SERVER A", "SERVER B ID", "SERVER B"}, connectionRows);
        }

        System.out.println("DSA: The graph stores server connections; DSU supplies connectivity status.");
        System.out.println("This view reports the current graph and does not run a failure simulation or BFS.");
    }

    private String formatNeighborIds(List<Integer> neighborIds) {
        if (neighborIds.isEmpty()) {
            return "(none)";
        }

        List<String> displayedIds = new ArrayList<>();
        for (int neighborId : neighborIds) {
            displayedIds.add(String.valueOf(neighborId));
            if (String.join(", ", displayedIds).length() > 27) {
                displayedIds.remove(displayedIds.size() - 1);
                break;
            }
        }
        if (displayedIds.size() == neighborIds.size()) {
            return String.join(", ", displayedIds);
        }
        while (!displayedIds.isEmpty()
                && String.join(", ", displayedIds).length() + 5 > 27) {
            displayedIds.remove(displayedIds.size() - 1);
        }
        return displayedIds.isEmpty() ? "..." : String.join(", ", displayedIds) + ", ...";
    }

    private void connectServers() {
        Integer firstId = readInt("Enter the first server ID: ");
        if (firstId == null) {
            return;
        }
        Integer secondId = readInt("Enter the second server ID: ");
        if (secondId == null) {
            return;
        }
        if (dataCenter.findServer(firstId) == null || dataCenter.findServer(secondId) == null) {
            System.out.println("[WARN] Both server IDs must be registered first.");
            return;
        }
        if (!failureAnalysis.connectServers(firstId, secondId)) {
            System.out.println("[WARN] Link not added. IDs must differ and the link must not already exist.");
            return;
        }

        auditLogger.log("SERVERS_CONNECTED", firstId + "<->" + secondId);
        System.out.println("[OK] Connected server " + firstId + " and server " + secondId + ".");
        System.out.println("DSA: the graph stores the link; DSU updates connectivity groups.");
    }

    private void checkConnection() {
        Integer firstId = readInt("Enter the first server ID: ");
        if (firstId == null) {
            return;
        }
        Integer secondId = readInt("Enter the second server ID: ");
        if (secondId == null) {
            return;
        }
        if (dataCenter.findServer(firstId) == null || dataCenter.findServer(secondId) == null) {
            System.out.println("[WARN] Both server IDs must be registered first.");
            return;
        }

        boolean connected = failureAnalysis.areConnected(firstId, secondId);
        System.out.println(connected
                ? "[OK] Servers " + firstId + " and " + secondId + " are connected."
                : "[INFO] Servers " + firstId + " and " + secondId + " are not connected.");
        System.out.println("DSA: DSU checks whether both IDs share a connectivity set.");
    }

    private void showReachableServers() {
        Integer serverId = readInt("Enter a server ID to inspect: ");
        if (serverId == null) {
            return;
        }
        if (dataCenter.findServer(serverId) == null) {
            System.out.println("[WARN] No server has ID " + serverId + ".");
            return;
        }

        List<Integer> reachableIds = failureAnalysis.affectedServers(serverId);
        List<String[]> rows = new ArrayList<>();
        for (int index = 0; index < reachableIds.size(); index++) {
            int reachableId = reachableIds.get(index);
            Server reachableServer = dataCenter.findServer(reachableId);
            rows.add(new String[]{String.valueOf(index + 1), String.valueOf(reachableId),
                    reachableServer.getName(), onlineStatus(reachableServer)});
        }
        System.out.println("[OK] Servers reachable from " + serverId + " (BFS traversal order):");
        printTable(new String[]{"ORDER", "SERVER ID", "SERVER", "STATUS"}, rows);
        System.out.println("DSA: BFS visits graph nodes level by level; recent results may use the LRU cache.");
    }

    private void reportOutage() {
        printSection("OUTAGE REPORT");
        Integer serverId = readInt("Enter the failed server ID: ");
        if (serverId == null) {
            return;
        }
        Server server = dataCenter.findServer(serverId);
        if (server == null) {
            System.out.println("[WARN] No server has ID " + serverId + ".");
            return;
        }
        if (!server.isOnline()) {
            System.out.println("[INFO] Server " + serverId + " is already OFFLINE and awaiting recovery.");
            return;
        }

        Integer severity = readInt("Enter severity (1 is most urgent, 5 is least urgent): ");
        if (severity == null) {
            return;
        }
        if (severity < 1 || severity > 5) {
            System.out.println("[WARN] Severity must be from 1 to 5.");
            return;
        }
        Long deadline = readLong("Enter a deadline number (smaller numbers are sooner): ");
        if (deadline == null) {
            return;
        }
        if (deadline < 0) {
            System.out.println("[WARN] The deadline number cannot be negative.");
            return;
        }
        String description = readNonBlankLine("Describe the recovery briefly: ");
        if (description == null) {
            return;
        }

        server.setOnline(false);
        RecoveryTask task = new RecoveryTask(
                nextRecoveryTaskId++, serverId, severity, deadline, description);
        recoveryScheduler.reportIncident(task, severity == 1);
        auditLogger.log("OUTAGE_REPORTED", "server=" + serverId + ", severity=" + severity);

        System.out.println("[OK] Server " + serverId + " is now OFFLINE; incident " + task.getId()
                + " was reported (severity " + severity + ", deadline " + deadline + ").");
        System.out.println("[INFO] Reachable network context (only server " + serverId + " was marked OFFLINE): "
                + failureAnalysis.affectedServers(serverId));
        System.out.println(severity == 1
                ? "[OK] Incident entered at the front of the incident deque."
                : "[OK] Incident entered at the back of the incident deque.");
        System.out.println("DSA: BFS shows graph reachability; severity 1 enters the deque front, other incidents the back.");
    }

    private void dispatchNextIncident() {
        printSection("INCIDENT DISPATCH");
        RecoveryTask task = recoveryScheduler.dispatchNextIncident();
        if (task == null) {
            System.out.println("[INFO] No reported incidents are waiting for dispatch.");
            return;
        }
        auditLogger.log("INCIDENT_DISPATCHED", "task=" + task.getId());
        System.out.println("[OK] Dispatched incident " + task.getId() + " for server " + task.getServerId()
                + " (severity " + task.getSeverity() + ", deadline " + task.getDeadline() + ").");
        System.out.println("Incidents remaining: " + recoveryScheduler.pendingIncidents()
                + " | recovery tasks dispatched: " + recoveryScheduler.pendingTasks());
        System.out.println("DSA: dispatch removes the deque front and inserts that task into the min-heap.");
    }

    private void processNextRecovery() {
        printSection("RECOVERY PROCESSING");
        RecoveryTask task = recoveryScheduler.nextTask();
        if (task == null) {
            System.out.println("[INFO] No dispatched recovery tasks are waiting.");
            return;
        }

        Server server = dataCenter.findServer(task.getServerId());
        if (server == null) {
            System.out.println("[FAIL] Recovery task " + task.getId()
                    + " refers to unregistered server " + task.getServerId() + ".");
            return;
        }
        server.setOnline(true);
        auditLogger.log("RECOVERY_COMPLETED", "task=" + task.getId() + ", server=" + task.getServerId());
        System.out.println("[OK] Recovery task " + task.getId() + " completed: server "
                + task.getServerId() + " is ONLINE (" + task.getDescription() + ").");
        System.out.println("DSA: the min-heap orders lower severity, earlier deadlines, then lower task IDs.");
    }

    private void manageRoutineJobs() {
        boolean inMenu = true;
        while (inMenu && !inputClosed) {
            printSection("ROUTINE JOBS");
            printMenuOption(1, "Submit a routine job  [FIFO queue]");
            printMenuOption(2, "Process the next job  [FIFO queue]");
            printMenuOption(3, "View the number of waiting jobs");
            printMenuOption(0, "Return to the main menu");

            Integer choice = readInt("Choose an operation: ");
            if (choice == null) {
                if (inputClosed) return;
                continue;
            }
            switch (choice) {
                case 1 -> submitJob();
                case 2 -> processNextJob();
                case 3 -> System.out.println("[INFO] Waiting routine jobs: " + dataCenter.queuedJobs());
                case 0 -> inMenu = false;
                default -> System.out.println("[WARN] Choose a number from 0 to 3.");
            }
        }
    }

    private void submitJob() {
        String description = readNonBlankLine("Describe the routine job: ");
        if (description == null) {
            return;
        }
        Job job = new Job(nextJobId++, description);
        dataCenter.submitJob(job);
        auditLogger.log("JOB_SUBMITTED", "job=" + job.getId());
        System.out.println("[OK] Job " + job.getId() + " added to the routine queue: " + description);
        System.out.println("DSA: the queue preserves FIFO processing order.");
    }

    private void processNextJob() {
        Job job = dataCenter.processNextJob();
        if (job == null) {
            System.out.println("[INFO] No routine jobs are waiting.");
            return;
        }
        auditLogger.log("JOB_COMPLETED", "job=" + job.getId());
        System.out.println("[OK] Completed job " + job.getId() + ": " + job.getDescription());
        System.out.println("Waiting jobs: " + dataCenter.queuedJobs());
        System.out.println("DSA: the queue removes the oldest submitted job first (FIFO).");
    }

    private void manageConfiguration() {
        boolean inMenu = true;
        while (inMenu && !inputClosed) {
            printSection("CONFIGURATION");
            System.out.println("Current configuration: " + dataCenter.currentConfiguration());
            printMenuOption(1, "Change configuration");
            printMenuOption(2, "Undo the most recent change  [Stack]");
            printMenuOption(0, "Return to the main menu");

            Integer choice = readInt("Choose an operation: ");
            if (choice == null) {
                if (inputClosed) return;
                continue;
            }
            switch (choice) {
                case 1 -> changeConfiguration();
                case 2 -> rollbackConfiguration();
                case 0 -> inMenu = false;
                default -> System.out.println("[WARN] Choose a number from 0 to 2.");
            }
        }
    }

    private void changeConfiguration() {
        String newConfiguration = readNonBlankLine("Enter the new configuration name: ");
        if (newConfiguration == null) {
            return;
        }
        dataCenter.changeConfiguration(newConfiguration);
        auditLogger.log("CONFIGURATION_CHANGED", newConfiguration);
        System.out.println("[OK] Configuration changed to \"" + newConfiguration + "\".");
        System.out.println("DSA: the previous value is stored on a stack for undo.");
    }

    private void rollbackConfiguration() {
        String restoredConfiguration = dataCenter.rollbackConfiguration();
        if (restoredConfiguration == null) {
            System.out.println("[INFO] No saved configuration change is available to undo.");
            return;
        }
        auditLogger.log("CONFIGURATION_UNDONE", restoredConfiguration);
        System.out.println("[OK] Restored configuration: \"" + restoredConfiguration + "\".");
        System.out.println("DSA: the stack restores the most recent saved value first (LIFO).");
    }

    private void manageTelemetry() {
        boolean inMenu = true;
        while (inMenu && !inputClosed) {
            printSection("TELEMETRY & LOAD");
            System.out.println("Buffered events: " + telemetryManager.bufferedEvents()
                    + "/" + telemetryManager.eventCapacity());
            System.out.println("Load samples: " + formatSamples(telemetryManager.loadSamples()));
            printMenuOption(1, "Record telemetry  [Circular queue]");
            printMenuOption(2, "Read oldest telemetry  [Circular queue]");
            printMenuOption(3, "Enter or replace load samples  [Segment tree]");
            printMenuOption(4, "Sum a range of load samples  [Segment tree]");
            printMenuOption(5, "Update one load sample  [Segment tree]");
            printMenuOption(0, "Return to the main menu");

            Integer choice = readInt("Choose an operation: ");
            if (choice == null) {
                if (inputClosed) return;
                continue;
            }
            switch (choice) {
                case 1 -> recordTelemetry();
                case 2 -> readTelemetry();
                case 3 -> enterLoadSamples();
                case 4 -> sumLoadSamples();
                case 5 -> updateLoadSample();
                case 0 -> inMenu = false;
                default -> System.out.println("[WARN] Choose a number from 0 to 5.");
            }
        }
    }

    private void recordTelemetry() {
        Integer serverId = readInt("Enter the server ID: ");
        if (serverId == null) {
            return;
        }
        if (dataCenter.findServer(serverId) == null) {
            System.out.println("[WARN] No server has ID " + serverId + ".");
            return;
        }
        String metric = readNonBlankLine("Enter the metric name: ");
        if (metric == null) {
            return;
        }
        Double value = readDouble("Enter the metric value: ");
        if (value == null) {
            return;
        }
        if (!Double.isFinite(value)) {
            System.out.println("[WARN] Enter a finite numeric value.");
            return;
        }

        TelemetryEvent event = new TelemetryEvent(System.currentTimeMillis(), serverId, metric, value);
        if (!telemetryManager.record(event)) {
            System.out.println("[WARN] Telemetry buffer is full. Read an event before recording another.");
            return;
        }
        auditLogger.log("TELEMETRY_RECORDED", "server=" + serverId + ", metric=" + metric);
        System.out.println("[OK] Telemetry recorded: server " + serverId + ", " + metric + " = " + value + ".");
        System.out.println("DSA: the fixed-capacity circular queue reuses slots after reads.");
    }

    private void readTelemetry() {
        if (telemetryManager.bufferedEvents() == 0) {
            System.out.println("[INFO] No telemetry events are buffered.");
            return;
        }
        TelemetryEvent event = telemetryManager.readNext();
        System.out.println("[OK] Oldest telemetry event:");
        printTable(new String[]{"SERVER ID", "METRIC", "VALUE"}, List.<String[]>of(new String[]{
                String.valueOf(event.getServerId()), event.getMetric(), String.valueOf(event.getValue())}));
        System.out.println("DSA: the circular queue reads buffered events in FIFO order.");
    }

    private void enterLoadSamples() {
        Integer count = readInt("How many load samples (1-50)? ");
        if (count == null) {
            return;
        }
        if (count < 1 || count > 50) {
            System.out.println("[WARN] Enter a sample count from 1 to 50.");
            return;
        }

        int[] samples = new int[count];
        for (int index = 0; index < count; index++) {
            Integer sample = readInt("Enter load sample " + index + " (0-100): ");
            if (sample == null) {
                return;
            }
            if (sample < 0 || sample > 100) {
                System.out.println("[WARN] Load samples must be from 0 to 100. No samples were changed.");
                return;
            }
            samples[index] = sample;
        }
        telemetryManager.setLoadSamples(samples);
        System.out.println("[OK] Stored " + count + " load samples: " + formatSamples(samples));
        System.out.println("DSA: the segment tree is built for range sums and point updates.");
    }

    private void sumLoadSamples() {
        int sampleCount = telemetryManager.loadSamples().length;
        if (sampleCount == 0) {
            System.out.println("[INFO] Enter load samples before requesting a range sum.");
            return;
        }
        Integer firstIndex = readInt("Enter the first sample index (0-based): ");
        if (firstIndex == null) {
            return;
        }
        Integer lastIndex = readInt("Enter the last sample index (inclusive): ");
        if (lastIndex == null) {
            return;
        }
        if (firstIndex < 0 || lastIndex >= sampleCount || firstIndex > lastIndex) {
            System.out.println("[WARN] Enter a valid range from 0 to " + (sampleCount - 1) + ".");
            return;
        }
        System.out.println("[OK] Sum of load samples " + firstIndex + " through " + lastIndex + ": "
                + telemetryManager.totalLoad(firstIndex, lastIndex));
        System.out.println("DSA: the segment tree answers this range-sum query.");
    }

    private void updateLoadSample() {
        int sampleCount = telemetryManager.loadSamples().length;
        if (sampleCount == 0) {
            System.out.println("[INFO] Enter load samples before updating one.");
            return;
        }
        Integer index = readInt("Enter the sample index to update (0-based): ");
        if (index == null) {
            return;
        }
        if (index < 0 || index >= sampleCount) {
            System.out.println("[WARN] Enter an index from 0 to " + (sampleCount - 1) + ".");
            return;
        }
        Integer value = readInt("Enter the new load value (0-100): ");
        if (value == null) {
            return;
        }
        if (value < 0 || value > 100) {
            System.out.println("[WARN] Load samples must be from 0 to 100.");
            return;
        }
        telemetryManager.updateLoad(index, value);
        System.out.println("[OK] Updated load sample " + index + ". Current samples: "
                + formatSamples(telemetryManager.loadSamples()));
        System.out.println("DSA: segment tree point update.");
    }

    private void manageFailoverPlan() {
        boolean inMenu = true;
        while (inMenu && !inputClosed) {
            printSection("FAILOVER PLANNING");
            printMenuOption(1, "Create or replace a plan  [Binary tree]");
            printMenuOption(2, "View the current plan  [Preorder traversal]");
            printMenuOption(0, "Return to the main menu");

            Integer choice = readInt("Choose an operation: ");
            if (choice == null) {
                if (inputClosed) return;
                continue;
            }
            switch (choice) {
                case 1 -> createFailoverPlan();
                case 2 -> showFailoverPlan();
                case 0 -> inMenu = false;
                default -> System.out.println("[WARN] Choose a number from 0 to 2.");
            }
        }
    }

    private void createFailoverPlan() {
        Integer primaryId = readInt("Enter the primary server ID: ");
        if (primaryId == null) {
            return;
        }
        Integer firstBackupId = readInt("Enter the first backup ID (0 for none): ");
        if (firstBackupId == null) {
            return;
        }
        Integer secondBackupId = readInt("Enter the second backup ID (0 for none): ");
        if (secondBackupId == null) {
            return;
        }

        Integer firstBackup = firstBackupId == 0 ? null : firstBackupId;
        Integer secondBackup = secondBackupId == 0 ? null : secondBackupId;
        if (!isRegistered(primaryId)
                || (firstBackup != null && !isRegistered(firstBackup))
                || (secondBackup != null && !isRegistered(secondBackup))) {
            System.out.println("[WARN] The primary and backup IDs must be registered servers.");
            return;
        }
        if ((firstBackup != null && firstBackup.equals(primaryId))
                || (secondBackup != null && secondBackup.equals(primaryId))
                || (firstBackup != null && firstBackup.equals(secondBackup))) {
            System.out.println("[WARN] Each server can appear only once in the plan.");
            return;
        }

        dataCenter.setFailoverPlan(primaryId, firstBackup, secondBackup);
        auditLogger.log("FAILOVER_PLAN_SET", "primary=" + primaryId);
        System.out.println("[OK] Failover plan saved.");
        showFailoverPlan();
    }

    private void showFailoverPlan() {
        List<Integer> plan = dataCenter.failoverPlanPreorder();
        if (plan.isEmpty()) {
            System.out.println("[INFO] No failover plan has been created.");
            return;
        }
        System.out.println("[OK] Current failover plan:");
        List<String[]> rows = new ArrayList<>();
        for (int index = 0; index < plan.size(); index++) {
            int serverId = plan.get(index);
            Server server = dataCenter.findServer(serverId);
            String role = index == 0 ? "PRIMARY" : "BACKUP";
            rows.add(new String[]{role, String.valueOf(serverId), server.getName(), onlineStatus(server)});
        }
        printTable(new String[]{"ROLE", "SERVER ID", "SERVER", "STATUS"}, rows);
        System.out.println("  Preorder: " + plan);
        System.out.println("DSA: binary tree preorder visits the primary before its backup nodes.");
    }

    private void manageAuditLog() {
        boolean inMenu = true;
        while (inMenu && !inputClosed) {
            printSection("AUDIT & INTEGRITY");
            printMenuOption(1, "View records and the Merkle root  [Merkle tree]");
            printMenuOption(2, "Verify a record  [Merkle proof]");
            printMenuOption(0, "Return to the main menu");

            Integer choice = readInt("Choose an operation: ");
            if (choice == null) {
                if (inputClosed) return;
                continue;
            }
            switch (choice) {
                case 1 -> showAuditLog();
                case 2 -> verifyAuditRecord();
                case 0 -> inMenu = false;
                default -> System.out.println("[WARN] Choose a number from 0 to 2.");
            }
        }
    }

    private void showAuditLog() {
        List<AuditRecord> records = auditLogger.records();
        if (records.isEmpty()) {
            System.out.println("[INFO] The audit log is empty.");
            return;
        }
        List<String[]> rows = new ArrayList<>();
        for (AuditRecord record : records) {
            rows.add(new String[]{String.valueOf(record.getSequence()), record.getAction(), record.getDetails()});
        }
        printTable(new String[]{"SEQ", "ACTION", "DETAILS"}, rows);
        System.out.println("[OK] Current in-memory Merkle root: " + auditLogger.merkleRoot());
    }

    private void verifyAuditRecord() {
        List<AuditRecord> records = auditLogger.records();
        if (records.isEmpty()) {
            System.out.println("[INFO] There are no audit records to verify.");
            return;
        }
        showAuditLog();
        Integer sequence = readInt("Enter the record number to verify: ");
        if (sequence == null) {
            return;
        }
        if (sequence < 1 || sequence > records.size()) {
            System.out.println("[WARN] Enter a record number from 1 to " + records.size() + ".");
            return;
        }

        AuditRecord original = records.get(sequence - 1);
        System.out.println("Original action: " + original.getAction());
        String action = readLine("Candidate action (Enter keeps the original): ");
        if (action == null) {
            return;
        }
        if (action.isEmpty()) {
            action = original.getAction();
        }
        System.out.println("Original details: " + original.getDetails());
        String details = readLine("Candidate details (Enter keeps the original): ");
        if (details == null) {
            return;
        }
        if (details.isEmpty()) {
            details = original.getDetails();
        }

        AuditRecord candidate = new AuditRecord(original.getSequence(), action, details);
        boolean valid = auditLogger.verifyRecord(
                sequence - 1, candidate.canonicalForm(), auditLogger.merkleRoot());
        System.out.println("DSA: a Merkle proof checks the record against the current in-memory log root.");
        System.out.println(valid
                ? "[OK] Record matches the current in-memory Merkle root."
                : "[WARN] Record does not match the current in-memory Merkle root.");
    }

    private boolean isRegistered(int serverId) {
        return dataCenter.findServer(serverId) != null;
    }

    private static String onlineStatus(Server server) {
        return server.isOnline() ? "ONLINE" : "OFFLINE";
    }

    private static String formatSamples(int[] samples) {
        if (samples.length == 0) {
            return "(none)";
        }
        StringBuilder result = new StringBuilder("[");
        for (int index = 0; index < samples.length; index++) {
            if (index > 0) {
                result.append(", ");
            }
            result.append(samples[index]);
        }
        return result.append(']').toString();
    }

    private Integer readInt(String prompt) {
        String value = readLine(prompt);
        if (value == null) {
            return null;
        }
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException exception) {
            System.out.println("[WARN] Enter a whole number. This operation was cancelled.");
            return null;
        }
    }

    private Long readLong(String prompt) {
        String value = readLine(prompt);
        if (value == null) {
            return null;
        }
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException exception) {
            System.out.println("[WARN] Enter a whole number. This operation was cancelled.");
            return null;
        }
    }

    private Double readDouble(String prompt) {
        String value = readLine(prompt);
        if (value == null) {
            return null;
        }
        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException exception) {
            System.out.println("[WARN] Enter a numeric value. This operation was cancelled.");
            return null;
        }
    }

    private String readNonBlankLine(String prompt) {
        String value = readLine(prompt);
        if (value == null) {
            return null;
        }
        if (value.isEmpty()) {
            System.out.println("[WARN] The value cannot be empty. This operation was cancelled.");
            return null;
        }
        return value;
    }

    private String readLine(String prompt) {
        System.out.print(prompt);
        if (!input.hasNextLine()) {
            inputClosed = true;
            System.out.println();
            return null;
        }
        return input.nextLine().trim();
    }
}
