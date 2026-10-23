package blackout.web;

import blackout.cli.StoryRunner;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Serves the local dashboard and runs the existing deterministic story scenarios. */
public final class WebDashboardServer implements AutoCloseable {
    private static final int DEFAULT_PORT = 8765;
    private static final int MAX_REQUEST_BYTES = 4096;
    private static final Pattern SCENARIO_REQUEST = Pattern.compile(
            "\\s*\\{\\s*\"scenario\"\\s*:\\s*\"([a-z]+)\"\\s*}\\s*");
    private static final List<ScenarioOption> SCENARIOS = List.of(
            new ScenarioOption("story", "Run the complete story"),
            new ScenarioOption("healthy", "Healthy data center"),
            new ScenarioOption("critical", "Critical outage and recovery"),
            new ScenarioOption("multiple", "Multiple incidents and priority order"),
            new ScenarioOption("edge", "Isolated, invalid, duplicate, and empty-queue cases"),
            new ScenarioOption("integrity", "Audit integrity, cache, and service discovery"));

    private final HttpServer server;

    public WebDashboardServer() throws IOException {
        this(DEFAULT_PORT);
    }

    /** A port of zero asks the operating system for an available port, useful for local tests. */
    public WebDashboardServer(int port) throws IOException {
        if (port < 0 || port > 65535) throw new IllegalArgumentException("Port must be between 0 and 65535.");
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", port), 0);
        server.createContext("/", this::handleRequest);
    }

    public void start() {
        server.start();
    }

    public int getPort() {
        return server.getAddress().getPort();
    }

    InetSocketAddress getAddress() {
        return server.getAddress();
    }

    @Override
    public void close() {
        server.stop(0);
    }

    private void handleRequest(HttpExchange exchange) throws IOException {
        try (exchange) {
            String path = exchange.getRequestURI().getPath();
            String method = exchange.getRequestMethod();
            if (path.equals("/api/scenarios")) {
                if (!method.equals("GET")) {
                    sendJson(exchange, 405, error("Method not allowed. Use GET."));
                    return;
                }
                sendJson(exchange, 200, scenariosJson());
                return;
            }
            if (path.equals("/api/run")) {
                if (!method.equals("POST")) {
                    sendJson(exchange, 405, error("Method not allowed. Use POST."));
                    return;
                }
                handleRun(exchange);
                return;
            }
            if (!method.equals("GET")) {
                sendJson(exchange, 405, error("Method not allowed. Use GET for dashboard files."));
                return;
            }
            serveAsset(exchange, path);
        } catch (RuntimeException exception) {
            sendJson(exchange, 500, error("The simulator could not complete the request."));
        }
    }

    private void handleRun(HttpExchange exchange) throws IOException {
        byte[] requestBytes = exchange.getRequestBody().readNBytes(MAX_REQUEST_BYTES + 1);
        if (requestBytes.length > MAX_REQUEST_BYTES) {
            sendJson(exchange, 413, error("Request body is too large."));
            return;
        }

        String body = new String(requestBytes, StandardCharsets.UTF_8);
        Matcher matcher = SCENARIO_REQUEST.matcher(body);
        if (!matcher.matches()) {
            sendJson(exchange, 400, error("Expected a JSON object with one scenario name."));
            return;
        }

        String scenario = matcher.group(1);
        if (!isKnownScenario(scenario)) {
            sendJson(exchange, 400, error("Unknown scenario. Choose one of the listed options."));
            return;
        }

        try {
            List<StoryRunner.ScenarioResult> results = runScenario(scenario);
            int passedCount = 0;
            for (StoryRunner.ScenarioResult result : results) if (result.passed()) passedCount++;
            boolean passed = passedCount == results.size();
            sendJson(exchange, passed ? 200 : 500, resultJson(scenario, passed, passedCount, results));
        } catch (RuntimeException exception) {
            sendJson(exchange, 500, error("The simulator could not complete the selected scenario."));
        }
    }

    private List<StoryRunner.ScenarioResult> runScenario(String scenario) {
        StoryRunner runner = new StoryRunner(new java.io.PrintStream(OutputStream.nullOutputStream()));
        return switch (scenario) {
            case "story" -> runner.runAll();
            case "healthy" -> List.of(runner.runHealthyDataCenter());
            case "critical" -> List.of(runner.runCriticalOutage());
            case "multiple" -> List.of(runner.runMultipleIncidents());
            case "edge" -> runner.runEdgeCases();
            case "integrity" -> runner.runIntegrityAndDiscovery();
            default -> throw new IllegalArgumentException("Unknown scenario.");
        };
    }

    private boolean isKnownScenario(String name) {
        for (ScenarioOption scenario : SCENARIOS) if (scenario.id().equals(name)) return true;
        return false;
    }

    private String scenariosJson() {
        StringBuilder json = new StringBuilder("{\"scenarios\":[");
        for (int i = 0; i < SCENARIOS.size(); i++) {
            if (i > 0) json.append(',');
            ScenarioOption scenario = SCENARIOS.get(i);
            json.append("{\"id\":").append(quote(scenario.id()))
                    .append(",\"label\":").append(quote(scenario.label())).append('}');
        }
        return json.append("]}").toString();
    }

    private String resultJson(String scenario, boolean passed, int passedCount,
                              List<StoryRunner.ScenarioResult> results) {
        StringBuilder json = new StringBuilder("{\"scenario\":").append(quote(scenario))
                .append(",\"passed\":").append(passed)
                .append(",\"summary\":{\"passed\":").append(passedCount)
                .append(",\"total\":").append(results.size()).append("},\"results\":[");
        for (int i = 0; i < results.size(); i++) {
            if (i > 0) json.append(',');
            appendScenarioResult(json, results.get(i));
        }
        return json.append("]}").toString();
    }

    private void appendScenarioResult(StringBuilder json, StoryRunner.ScenarioResult result) {
        json.append("{\"name\":").append(quote(result.name()))
                .append(",\"passed\":").append(result.passed())
                .append(",\"serverOnline\":{");
        int stateIndex = 0;
        for (Map.Entry<String, Boolean> state : result.serverOnline().entrySet()) {
            if (stateIndex++ > 0) json.append(',');
            json.append(quote(state.getKey())).append(':').append(state.getValue());
        }
        json.append("},\"impactedServers\":");
        appendStrings(json, result.impactedServers());
        json.append(",\"recoveryOrder\":");
        appendStrings(json, result.recoveryOrder());
        json.append(",\"pendingTasks\":").append(result.pendingTasks())
                .append(",\"auditRecordCount\":").append(result.auditRecordCount()).append('}');
    }

    private void appendStrings(StringBuilder json, List<String> values) {
        json.append('[');
        for (int i = 0; i < values.size(); i++) {
            if (i > 0) json.append(',');
            json.append(quote(values.get(i)));
        }
        json.append(']');
    }

    private String quote(String value) {
        StringBuilder escaped = new StringBuilder("\"");
        for (int i = 0; i < value.length(); i++) {
            char character = value.charAt(i);
            switch (character) {
                case '"' -> escaped.append("\\\"");
                case '\\' -> escaped.append("\\\\");
                case '\n' -> escaped.append("\\n");
                case '\r' -> escaped.append("\\r");
                case '\t' -> escaped.append("\\t");
                default -> {
                    if (character < 0x20) escaped.append(String.format("\\u%04x", (int) character));
                    else escaped.append(character);
                }
            }
        }
        return escaped.append('"').toString();
    }

    private void serveAsset(HttpExchange exchange, String path) throws IOException {
        String resource = switch (path) {
            case "/", "/index.html" -> "/web/index.html";
            case "/style.css" -> "/web/style.css";
            case "/app.js" -> "/web/app.js";
            default -> null;
        };
        if (resource == null) {
            sendJson(exchange, 404, error("Not found."));
            return;
        }
        byte[] content;
        try (var input = WebDashboardServer.class.getResourceAsStream(resource)) {
            if (input == null) {
                sendJson(exchange, 500, error("A dashboard file is missing from the application package."));
                return;
            }
            content = input.readAllBytes();
        }
        String contentType = resource.endsWith(".css") ? "text/css; charset=utf-8"
                : resource.endsWith(".js") ? "text/javascript; charset=utf-8"
                : "text/html; charset=utf-8";
        exchange.getResponseHeaders().set("Content-Type", contentType);
        exchange.getResponseHeaders().set("X-Content-Type-Options", "nosniff");
        exchange.getResponseHeaders().set("Cache-Control", "no-store");
        exchange.sendResponseHeaders(200, content.length);
        try (OutputStream output = exchange.getResponseBody()) {
            output.write(content);
        }
    }

    private String error(String message) {
        return "{\"error\":" + quote(message) + "}";
    }

    private void sendJson(HttpExchange exchange, int status, String json) throws IOException {
        byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
        exchange.getResponseHeaders().set("Cache-Control", "no-store");
        exchange.getResponseHeaders().set("X-Content-Type-Options", "nosniff");
        exchange.sendResponseHeaders(status, bytes.length);
        try (OutputStream output = exchange.getResponseBody()) {
            output.write(bytes);
        }
    }

    private record ScenarioOption(String id, String label) { }
}
