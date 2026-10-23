package blackout.cli;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class StoryRunnerTest {
    @Test
    void completeStoryRunsAllScenariosWithVerifiedOutcomes() {
        StoryRunner runner = quietRunner();

        List<StoryRunner.ScenarioResult> results = runner.runAll();

        assertEquals(9, results.size());
        assertTrue(results.stream().allMatch(StoryRunner.ScenarioResult::passed));
        assertEquals("critical outage", results.get(1).name());
        assertEquals(List.of("Customer database", "Authentication service", "Analytics platform",
                "Banking API"), results.get(2).recoveryOrder());
        assertEquals("audit integrity", results.get(7).name());
        assertEquals("cache and discovery", results.get(8).name());
    }

    @Test
    void criticalOutageReportsConnectedContextAndRestoresOnlyTheRequestedTarget() {
        StoryRunner.ScenarioResult result = quietRunner().runCriticalOutage();

        assertTrue(result.passed());
        assertEquals(4, result.impactedServers().size());
        assertEquals(List.of("Customer database"), result.recoveryOrder());
        assertTrue(result.serverOnline().values().stream().allMatch(Boolean::booleanValue));
        assertEquals(0, result.pendingTasks());
    }

    @Test
    void multipleIncidentScenarioReportsDequeAndHeapOrdersFromActualProcessing() {
        ByteArrayOutputStream captured = new ByteArrayOutputStream();
        StoryRunner runner = new StoryRunner(new PrintStream(captured, true, StandardCharsets.UTF_8));

        StoryRunner.ScenarioResult result = runner.runMultipleIncidents();
        String output = captured.toString(StandardCharsets.UTF_8);

        assertTrue(result.passed());
        assertEquals(List.of("Customer database", "Authentication service", "Analytics platform",
                "Banking API"), result.recoveryOrder());
        assertTrue(output.contains("Task 2 | Customer database | severity 1 | deadline 900 | deque FRONT"));
        assertTrue(output.contains("Task 1 | Banking API | severity 2 | deadline 500 | deque BACK"));
        assertTrue(output.contains("Observed deque dispatch order: Customer database -> Banking API -> Authentication service -> Analytics platform"));
        assertTrue(output.contains("Observed heap processing order: Customer database -> Authentication service -> Analytics platform -> Banking API"));
        assertTrue(output.indexOf("Processed heap-selected task 3 for Authentication service")
                < output.indexOf("Processed heap-selected task 4 for Analytics platform"),
                "Equal severity and deadline tasks should be processed in task-ID order.");
        assertEquals(4, occurrences(output, "recovery SUCCEEDED (ONLINE)"));
        assertTrue(output.contains("Final report: PASS"));
    }

    @Test
    void edgeScenariosCoverIsolationInvalidDuplicateAndEmptyQueue() {
        List<StoryRunner.ScenarioResult> results = quietRunner().runEdgeCases();

        assertEquals(4, results.size());
        assertTrue(results.stream().allMatch(StoryRunner.ScenarioResult::passed));
        assertEquals(List.of("Isolated reporting node"), results.get(0).impactedServers());
        assertEquals(0, results.get(1).auditRecordCount());
        assertEquals(0, results.get(3).pendingTasks());
    }

    @Test
    void integrityCacheAndTrieScenariosVerifyActualStructureResults() {
        StoryRunner runner = quietRunner();

        StoryRunner.ScenarioResult integrity = runner.runIntegrity();
        StoryRunner.ScenarioResult discovery = runner.runCacheAndDiscovery();

        assertTrue(integrity.passed());
        assertEquals(9, integrity.auditRecordCount());
        assertTrue(discovery.passed());
    }

    private StoryRunner quietRunner() {
        return new StoryRunner(new PrintStream(OutputStream.nullOutputStream()));
    }

    private int occurrences(String text, String value) {
        int count = 0;
        int index = 0;
        while ((index = text.indexOf(value, index)) >= 0) {
            count++;
            index += value.length();
        }
        return count;
    }
}
