package blackout.web;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static org.junit.jupiter.api.Assertions.*;

class WebDashboardServerTest {
    private final HttpClient client = HttpClient.newHttpClient();

    @Test
    void dashboardAndScenarioMenuAreServedFromLoopback() throws Exception {
        try (WebDashboardServer server = startServer()) {
            assertEquals("127.0.0.1", serverAddress(server));
            HttpResponse<String> page = get(server, "/");
            assertEquals(200, page.statusCode());
            assertTrue(page.body().contains("Data Center Failure and Recovery Simulator"));
            assertTrue(page.body().contains("/app.js"));

            HttpResponse<String> options = get(server, "/api/scenarios");
            assertEquals(200, options.statusCode());
            assertTrue(options.body().contains("\"id\":\"story\""));
            assertTrue(options.body().contains("\"id\":\"critical\""));
            assertTrue(options.body().contains("\"id\":\"integrity\""));
        }
    }

    @Test
    void criticalAndMultipleScenariosReturnActualRecoveryStateAndOrdering() throws Exception {
        try (WebDashboardServer server = startServer()) {
            HttpResponse<String> critical = postScenario(server, "critical");
            assertEquals(200, critical.statusCode());
            assertTrue(critical.body().contains("\"passed\":true"));
            assertTrue(critical.body().contains("\"pendingTasks\":0"));
            assertTrue(critical.body().contains("\"Customer database\":true"));
            assertTrue(critical.body().contains("\"recoveryOrder\":[\"Customer database\"]"));

            HttpResponse<String> multiple = postScenario(server, "multiple");
            assertEquals(200, multiple.statusCode());
            assertTrue(multiple.body().contains("\"passed\":true"));
            assertTrue(multiple.body().contains("Customer database\",\"Authentication service\",\"Analytics platform\",\"Banking API"));
        }
    }

    @Test
    void edgeIntegrityAndFullStoryScenariosAreConnectedToTheExistingRunner() throws Exception {
        try (WebDashboardServer server = startServer()) {
            HttpResponse<String> edge = postScenario(server, "edge");
            assertEquals(200, edge.statusCode());
            assertTrue(edge.body().contains("\"total\":4"));
            assertTrue(edge.body().contains("\"name\":\"unknown server\",\"passed\":true"));

            HttpResponse<String> integrity = postScenario(server, "integrity");
            assertEquals(200, integrity.statusCode());
            assertTrue(integrity.body().contains("\"name\":\"audit integrity\",\"passed\":true"));
            assertTrue(integrity.body().contains("\"name\":\"cache and discovery\",\"passed\":true"));

            HttpResponse<String> story = postScenario(server, "story");
            assertEquals(200, story.statusCode());
            assertTrue(story.body().contains("\"total\":9"));
            assertTrue(story.body().contains("\"passed\":true"));
        }
    }

    @Test
    void invalidRequestsAndUnknownPathsReturnJsonErrors() throws Exception {
        try (WebDashboardServer server = startServer()) {
            HttpResponse<String> invalidJson = post(server, "not json");
            assertEquals(400, invalidJson.statusCode());
            assertTrue(invalidJson.headers().firstValue("Content-Type").orElse("").startsWith("application/json"));
            assertTrue(invalidJson.body().contains("\"error\""));

            HttpResponse<String> unknown = postScenario(server, "system");
            assertEquals(400, unknown.statusCode());
            assertTrue(unknown.body().contains("Unknown scenario"));

            HttpResponse<String> missing = get(server, "/unknown");
            assertEquals(404, missing.statusCode());
            assertTrue(missing.body().contains("\"error\""));
        }
    }

    @Test
    void wrongMethodIsReportedAsJson() throws Exception {
        try (WebDashboardServer server = startServer()) {
            HttpRequest request = HttpRequest.newBuilder(uri(server, "/api/run")).GET().build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            assertEquals(405, response.statusCode());
            assertTrue(response.body().contains("\"error\""));
        }
    }

    private WebDashboardServer startServer() throws IOException {
        WebDashboardServer server = new WebDashboardServer(0);
        server.start();
        return server;
    }

    private String serverAddress(WebDashboardServer server) throws Exception {
        return server.getAddress().getAddress().getHostAddress();
    }

    private HttpResponse<String> get(WebDashboardServer server, String path) throws Exception {
        HttpRequest request = HttpRequest.newBuilder(uri(server, path)).GET().build();
        return client.send(request, HttpResponse.BodyHandlers.ofString());
    }

    private HttpResponse<String> postScenario(WebDashboardServer server, String scenario) throws Exception {
        return post(server, "{\"scenario\":\"" + scenario + "\"}");
    }

    private HttpResponse<String> post(WebDashboardServer server, String body) throws Exception {
        HttpRequest request = HttpRequest.newBuilder(uri(server, "/api/run"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body)).build();
        return client.send(request, HttpResponse.BodyHandlers.ofString());
    }

    private URI uri(WebDashboardServer server, String path) {
        return URI.create("http://127.0.0.1:" + server.getPort() + path);
    }
}
