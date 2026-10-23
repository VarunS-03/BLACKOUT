package blackout.cli;

import org.junit.jupiter.api.Test;

import java.io.OutputStream;
import java.io.PrintStream;
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
}
