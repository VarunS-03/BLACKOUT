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

    public static void main(String[] args) {
        new Main().run();
    }

    private void run() {
        System.out.println("BLACKOUT - Data Center Failure and Recovery");

        boolean running = true;
        while (running) {
            printMainMenu();
            Integer choice = readInt("Enter your choice: ");
            if (choice == null) {
                continue;
            }

            switch (choice) {
                case 1 -> manageServers();
                case 2 -> manageNetwork();
                case 3 -> reportOutage();
                case 4 -> dispatchNextIncident();
                case 5 -> processNextRecovery();
                case 6 -> manageRoutineJobs();
                case 7 -> manageConfiguration();
                case 8 -> manageTelemetry();
                case 9 -> manageFailoverPlan();
                case 10 -> manageAuditLog();
                case 0 -> {
                    running = false;
                    System.out.println("BLACKOUT closed.");
                }
                default -> System.out.println("Choose a number from 0 to 10.");
            }
        }
    }

    private void printMainMenu() {
        System.out.println();
        System.out.println("==========================================");
        System.out.println("                 BLACKOUT");
        System.out.println("==========================================");
        System.out.println("1. Manage server inventory");
        System.out.println("2. Manage server connections");
        System.out.println("3. Report a server outage");
        System.out.println("4. Dispatch the next incident");
        System.out.println("5. Process the highest-priority recovery");
        System.out.println("6. Manage routine jobs");
        System.out.println("7. Change or undo the system configuration");
        System.out.println("8. Manage telemetry and load samples");
        System.out.println("9. Create or view a failover plan");
        System.out.println("10. View or verify the audit log");
        System.out.println("0. Exit");
    }

    private void manageServers() {
        boolean inMenu = true;
        while (inMenu) {
            System.out.println();
            System.out.println("--- Server inventory ---");
            System.out.println("1. Register a server");
            System.out.println("2. Find a server by ID");
            System.out.println("3. Find server names by prefix");
            System.out.println("4. List servers in registration order");
            System.out.println("5. List server IDs in sorted order");
            System.out.println("0. Return to the main menu");

            Integer choice = readInt("Choose an operation: ");
            if (choice == null) {
                continue;
            }
            switch (choice) {
                case 1 -> registerServer();
                case 2 -> findServer();
                case 3 -> findServerNames();
                case 4 -> listServers();
                case 5 -> listSortedServerIds();
                case 0 -> inMenu = false;
                default -> System.out.println("Choose a number from 0 to 5.");
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
            System.out.println("That server ID is already registered.");
            return;
        }
        failureAnalysis.addServer(id);
        auditLogger.log("SERVER_REGISTERED", "id=" + id + ", name=" + name);
        System.out.println("Server registered: " + id + " - " + name);
        System.out.println("DSA: linked list inventory, hash-table ID lookup, BST ordering, and Trie name search.");
    }

    private void findServer() {
        Integer id = readInt("Enter the server ID to find: ");
        if (id == null) {
            return;
        }

        Server server = dataCenter.findServer(id);
        if (server == null) {
            System.out.println("No server has ID " + id + ".");
            return;
        }
        System.out.println("Found: " + server.getId() + " - " + server.getName()
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
            System.out.println("No server names start with \"" + prefix + "\".");
            return;
        }
        System.out.println("Matching names: " + names);
        System.out.println("DSA: Trie follows the entered name prefix.");
    }

    private void listServers() {
        List<Server> servers = dataCenter.registeredServers();
        if (servers.isEmpty()) {
            System.out.println("No servers are registered.");
            return;
        }
        System.out.println("Servers in registration order:");
        for (Server server : servers) {
            System.out.println("- " + server.getId() + " - " + server.getName()
                    + " (" + onlineStatus(server) + ")");
        }
        System.out.println("DSA: the custom linked list preserves registration order.");
    }

    private void listSortedServerIds() {
        List<Integer> serverIds = dataCenter.serverIdsInOrder();
        if (serverIds.isEmpty()) {
            System.out.println("No servers are registered.");
            return;
        }
        System.out.println("Server IDs in ascending order: " + serverIds);
        System.out.println("DSA: inorder traversal of the binary search tree.");
    }

    private void manageNetwork() {
        boolean inMenu = true;
        while (inMenu) {
            System.out.println();
            System.out.println("--- Server connections ---");
            System.out.println("1. Connect two servers");
            System.out.println("2. Check whether two servers are connected");
            System.out.println("3. List servers reachable from a server");
            System.out.println("0. Return to the main menu");

            Integer choice = readInt("Choose an operation: ");
            if (choice == null) {
                continue;
            }
            switch (choice) {
                case 1 -> connectServers();
                case 2 -> checkConnection();
                case 3 -> showReachableServers();
                case 0 -> inMenu = false;
                default -> System.out.println("Choose a number from 0 to 3.");
            }
        }
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
            System.out.println("Both server IDs must be registered first.");
            return;
        }
        if (!failureAnalysis.connectServers(firstId, secondId)) {
            System.out.println("The connection was not added. Check that the IDs differ and are not already connected.");
            return;
        }

        auditLogger.log("SERVERS_CONNECTED", firstId + "<->" + secondId);
        System.out.println("Connected server " + firstId + " and server " + secondId + ".");
        System.out.println("DSA: the graph stores the relationship; DSU updates the connectivity groups.");
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
            System.out.println("Both server IDs must be registered first.");
            return;
        }

        boolean connected = failureAnalysis.areConnected(firstId, secondId);
        System.out.println(connected
                ? "The servers are in the same connected network."
                : "The servers are not connected.");
        System.out.println("DSA: DSU checks whether the two servers have the same set representative.");
    }

    private void showReachableServers() {
        Integer serverId = readInt("Enter a server ID to inspect: ");
        if (serverId == null) {
            return;
        }
        if (dataCenter.findServer(serverId) == null) {
            System.out.println("No server has ID " + serverId + ".");
            return;
        }

        System.out.println("Servers reachable from " + serverId + ": "
                + failureAnalysis.affectedServers(serverId));
        System.out.println("DSA: graph breadth-first search; recent results use an LRU cache.");
    }

    private void reportOutage() {
        Integer serverId = readInt("Enter the failed server ID: ");
        if (serverId == null) {
            return;
        }
        Server server = dataCenter.findServer(serverId);
        if (server == null) {
            System.out.println("No server has ID " + serverId + ".");
            return;
        }
        if (!server.isOnline()) {
            System.out.println("That server is already offline and awaiting recovery.");
            return;
        }

        Integer severity = readInt("Enter severity (1 is most urgent, 5 is least urgent): ");
        if (severity == null) {
            return;
        }
        if (severity < 1 || severity > 5) {
            System.out.println("Severity must be from 1 to 5.");
            return;
        }
        Long deadline = readLong("Enter a deadline number (smaller numbers are sooner): ");
        if (deadline == null) {
            return;
        }
        if (deadline < 0) {
            System.out.println("The deadline number cannot be negative.");
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

        System.out.println("Server " + serverId + " is marked offline.");
        System.out.println("Connected servers in its network: "
                + failureAnalysis.affectedServers(serverId));
        System.out.println(severity == 1
                ? "Incident added to the front of the urgent incident deque."
                : "Incident added to the back of the incident deque.");
    }

    private void dispatchNextIncident() {
        RecoveryTask task = recoveryScheduler.dispatchNextIncident();
        if (task == null) {
            System.out.println("There are no reported incidents to dispatch.");
            return;
        }
        auditLogger.log("INCIDENT_DISPATCHED", "task=" + task.getId());
        System.out.println("Incident for server " + task.getServerId()
                + " added to the recovery priority queue.");
        System.out.println("Dispatched incidents remaining: " + recoveryScheduler.pendingIncidents());
        System.out.println("Recovery tasks waiting: " + recoveryScheduler.pendingTasks());
        System.out.println("DSA: remove from the front of the deque, then insert into the heap-based priority queue.");
    }

    private void processNextRecovery() {
        RecoveryTask task = recoveryScheduler.nextTask();
        if (task == null) {
            System.out.println("There are no dispatched recovery tasks.");
            return;
        }

        Server server = dataCenter.findServer(task.getServerId());
        if (server == null) {
            System.out.println("The recovery task refers to an unknown server ID.");
            return;
        }
        server.setOnline(true);
        auditLogger.log("RECOVERY_COMPLETED", "task=" + task.getId() + ", server=" + task.getServerId());
        System.out.println("Recovered server " + task.getServerId() + ": " + task.getDescription());
        System.out.println("DSA: the heap selected severity " + task.getSeverity()
                + ", then deadline, then task ID.");
    }

    private void manageRoutineJobs() {
        boolean inMenu = true;
        while (inMenu) {
            System.out.println();
            System.out.println("--- Routine jobs ---");
            System.out.println("1. Submit a routine job");
            System.out.println("2. Process the next routine job");
            System.out.println("3. View the number of waiting jobs");
            System.out.println("0. Return to the main menu");

            Integer choice = readInt("Choose an operation: ");
            if (choice == null) {
                continue;
            }
            switch (choice) {
                case 1 -> submitJob();
                case 2 -> processNextJob();
                case 3 -> System.out.println("Waiting jobs: " + dataCenter.queuedJobs());
                case 0 -> inMenu = false;
                default -> System.out.println("Choose a number from 0 to 3.");
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
        System.out.println("Job " + job.getId() + " added to the waiting line.");
        System.out.println("DSA: the custom queue processes routine jobs in first-in, first-out order.");
    }

    private void processNextJob() {
        Job job = dataCenter.processNextJob();
        if (job == null) {
            System.out.println("There are no routine jobs waiting.");
            return;
        }
        auditLogger.log("JOB_COMPLETED", "job=" + job.getId());
        System.out.println("Completed job " + job.getId() + ": " + job.getDescription());
        System.out.println("Waiting jobs: " + dataCenter.queuedJobs());
        System.out.println("DSA: the queue removes jobs in first-in, first-out order.");
    }

    private void manageConfiguration() {
        boolean inMenu = true;
        while (inMenu) {
            System.out.println();
            System.out.println("--- System configuration ---");
            System.out.println("Current configuration: " + dataCenter.currentConfiguration());
            System.out.println("1. Change configuration");
            System.out.println("2. Undo the most recent change");
            System.out.println("0. Return to the main menu");

            Integer choice = readInt("Choose an operation: ");
            if (choice == null) {
                continue;
            }
            switch (choice) {
                case 1 -> changeConfiguration();
                case 2 -> rollbackConfiguration();
                case 0 -> inMenu = false;
                default -> System.out.println("Choose a number from 0 to 2.");
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
        System.out.println("Configuration changed to \"" + newConfiguration + "\".");
        System.out.println("DSA: the previous configuration is pushed onto a stack for LIFO undo.");
    }

    private void rollbackConfiguration() {
        String restoredConfiguration = dataCenter.rollbackConfiguration();
        if (restoredConfiguration == null) {
            System.out.println("There is no configuration change to undo.");
            return;
        }
        auditLogger.log("CONFIGURATION_UNDONE", restoredConfiguration);
        System.out.println("Restored configuration: \"" + restoredConfiguration + "\".");
        System.out.println("DSA: the stack restores the most recently saved configuration first.");
    }

    private void manageTelemetry() {
        boolean inMenu = true;
        while (inMenu) {
            System.out.println();
            System.out.println("--- Telemetry ---");
            System.out.println("Buffered events: " + telemetryManager.bufferedEvents()
                    + "/" + telemetryManager.eventCapacity());
            System.out.println("Load samples: " + formatSamples(telemetryManager.loadSamples()));
            System.out.println("1. Record a telemetry event");
            System.out.println("2. Read the oldest telemetry event");
            System.out.println("3. Enter or replace load samples");
            System.out.println("4. Sum a range of load samples");
            System.out.println("5. Update one load sample");
            System.out.println("0. Return to the main menu");

            Integer choice = readInt("Choose an operation: ");
            if (choice == null) {
                continue;
            }
            switch (choice) {
                case 1 -> recordTelemetry();
                case 2 -> readTelemetry();
                case 3 -> enterLoadSamples();
                case 4 -> sumLoadSamples();
                case 5 -> updateLoadSample();
                case 0 -> inMenu = false;
                default -> System.out.println("Choose a number from 0 to 5.");
            }
        }
    }

    private void recordTelemetry() {
        Integer serverId = readInt("Enter the server ID: ");
        if (serverId == null) {
            return;
        }
        if (dataCenter.findServer(serverId) == null) {
            System.out.println("No server has ID " + serverId + ".");
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
            System.out.println("Enter a finite numeric value.");
            return;
        }

        TelemetryEvent event = new TelemetryEvent(System.currentTimeMillis(), serverId, metric, value);
        if (!telemetryManager.record(event)) {
            System.out.println("The telemetry buffer is full. Read an event before recording another.");
            return;
        }
        auditLogger.log("TELEMETRY_RECORDED", "server=" + serverId + ", metric=" + metric);
        System.out.println("Telemetry event recorded.");
        System.out.println("DSA: the fixed-size circular queue reuses positions as old events are read.");
    }

    private void readTelemetry() {
        if (telemetryManager.bufferedEvents() == 0) {
            System.out.println("There are no telemetry events to read.");
            return;
        }
        TelemetryEvent event = telemetryManager.readNext();
        System.out.println("Oldest event: server " + event.getServerId() + ", "
                + event.getMetric() + " = " + event.getValue());
        System.out.println("DSA: the circular queue returns telemetry in first-in, first-out order.");
    }

    private void enterLoadSamples() {
        Integer count = readInt("How many load samples (1-50)? ");
        if (count == null) {
            return;
        }
        if (count < 1 || count > 50) {
            System.out.println("Enter a sample count from 1 to 50.");
            return;
        }

        int[] samples = new int[count];
        for (int index = 0; index < count; index++) {
            Integer sample = readInt("Enter load sample " + index + " (0-100): ");
            if (sample == null) {
                return;
            }
            if (sample < 0 || sample > 100) {
                System.out.println("Load samples must be from 0 to 100. No samples were changed.");
                return;
            }
            samples[index] = sample;
        }
        telemetryManager.setLoadSamples(samples);
        System.out.println("Stored " + count + " load samples: " + formatSamples(samples));
        System.out.println("DSA: the segment tree is built for range sums and point updates.");
    }

    private void sumLoadSamples() {
        int sampleCount = telemetryManager.loadSamples().length;
        if (sampleCount == 0) {
            System.out.println("Enter load samples before requesting a range sum.");
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
            System.out.println("Enter a valid range from 0 to " + (sampleCount - 1) + ".");
            return;
        }
        System.out.println("Sum of samples " + firstIndex + " through " + lastIndex + ": "
                + telemetryManager.totalLoad(firstIndex, lastIndex));
        System.out.println("DSA: segment tree range-sum query.");
    }

    private void updateLoadSample() {
        int sampleCount = telemetryManager.loadSamples().length;
        if (sampleCount == 0) {
            System.out.println("Enter load samples before updating one.");
            return;
        }
        Integer index = readInt("Enter the sample index to update (0-based): ");
        if (index == null) {
            return;
        }
        if (index < 0 || index >= sampleCount) {
            System.out.println("Enter an index from 0 to " + (sampleCount - 1) + ".");
            return;
        }
        Integer value = readInt("Enter the new load value (0-100): ");
        if (value == null) {
            return;
        }
        if (value < 0 || value > 100) {
            System.out.println("Load samples must be from 0 to 100.");
            return;
        }
        telemetryManager.updateLoad(index, value);
        System.out.println("Updated sample " + index + ". Current samples: "
                + formatSamples(telemetryManager.loadSamples()));
        System.out.println("DSA: segment tree point update.");
    }

    private void manageFailoverPlan() {
        boolean inMenu = true;
        while (inMenu) {
            System.out.println();
            System.out.println("--- Failover plan ---");
            System.out.println("1. Create or replace a plan");
            System.out.println("2. View the current plan");
            System.out.println("0. Return to the main menu");

            Integer choice = readInt("Choose an operation: ");
            if (choice == null) {
                continue;
            }
            switch (choice) {
                case 1 -> createFailoverPlan();
                case 2 -> showFailoverPlan();
                case 0 -> inMenu = false;
                default -> System.out.println("Choose a number from 0 to 2.");
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
            System.out.println("The primary and backup IDs must be registered servers.");
            return;
        }
        if ((firstBackup != null && firstBackup.equals(primaryId))
                || (secondBackup != null && secondBackup.equals(primaryId))
                || (firstBackup != null && firstBackup.equals(secondBackup))) {
            System.out.println("Each server can appear only once in the plan.");
            return;
        }

        dataCenter.setFailoverPlan(primaryId, firstBackup, secondBackup);
        auditLogger.log("FAILOVER_PLAN_SET", "primary=" + primaryId);
        showFailoverPlan();
        System.out.println("DSA: a binary tree stores one primary and up to two backup servers.");
    }

    private void showFailoverPlan() {
        List<Integer> plan = dataCenter.failoverPlanPreorder();
        if (plan.isEmpty()) {
            System.out.println("No failover plan has been created.");
            return;
        }
        System.out.println("Failover plan in preorder (primary first): " + plan);
        System.out.println("DSA: binary tree preorder traversal.");
    }

    private void manageAuditLog() {
        boolean inMenu = true;
        while (inMenu) {
            System.out.println();
            System.out.println("--- Audit log ---");
            System.out.println("1. View records and the Merkle root");
            System.out.println("2. Verify a record");
            System.out.println("0. Return to the main menu");

            Integer choice = readInt("Choose an operation: ");
            if (choice == null) {
                continue;
            }
            switch (choice) {
                case 1 -> showAuditLog();
                case 2 -> verifyAuditRecord();
                case 0 -> inMenu = false;
                default -> System.out.println("Choose a number from 0 to 2.");
            }
        }
    }

    private void showAuditLog() {
        List<AuditRecord> records = auditLogger.records();
        if (records.isEmpty()) {
            System.out.println("The audit log is empty.");
            return;
        }
        for (AuditRecord record : records) {
            System.out.println(record.getSequence() + ". " + record.getAction()
                    + " - " + record.getDetails());
        }
        System.out.println("Merkle root: " + auditLogger.merkleRoot());
    }

    private void verifyAuditRecord() {
        List<AuditRecord> records = auditLogger.records();
        if (records.isEmpty()) {
            System.out.println("There are no audit records to verify.");
            return;
        }
        showAuditLog();
        Integer sequence = readInt("Enter the record number to verify: ");
        if (sequence == null) {
            return;
        }
        if (sequence < 1 || sequence > records.size()) {
            System.out.println("Enter a record number from 1 to " + records.size() + ".");
            return;
        }

        AuditRecord original = records.get(sequence - 1);
        System.out.println("Original action: " + original.getAction());
        String action = readLine("Candidate action (Enter keeps the original): ");
        if (action.isEmpty()) {
            action = original.getAction();
        }
        System.out.println("Original details: " + original.getDetails());
        String details = readLine("Candidate details (Enter keeps the original): ");
        if (details.isEmpty()) {
            details = original.getDetails();
        }

        AuditRecord candidate = new AuditRecord(original.getSequence(), action, details);
        boolean valid = auditLogger.verifyRecord(
                sequence - 1, candidate.canonicalForm(), auditLogger.merkleRoot());
        System.out.println(valid
                ? "Record matches the current trusted Merkle root."
                : "Record does not match the current trusted Merkle root.");
    }

    private boolean isRegistered(int serverId) {
        return dataCenter.findServer(serverId) != null;
    }

    private static String onlineStatus(Server server) {
        return server.isOnline() ? "online" : "offline";
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
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException exception) {
            System.out.println("Enter a whole number. This operation was cancelled.");
            return null;
        }
    }

    private Long readLong(String prompt) {
        String value = readLine(prompt);
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException exception) {
            System.out.println("Enter a whole number. This operation was cancelled.");
            return null;
        }
    }

    private Double readDouble(String prompt) {
        String value = readLine(prompt);
        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException exception) {
            System.out.println("Enter a numeric value. This operation was cancelled.");
            return null;
        }
    }

    private String readNonBlankLine(String prompt) {
        String value = readLine(prompt);
        if (value.isEmpty()) {
            System.out.println("The value cannot be empty. This operation was cancelled.");
            return null;
        }
        return value;
    }

    private String readLine(String prompt) {
        System.out.print(prompt);
        return input.nextLine().trim();
    }
}
