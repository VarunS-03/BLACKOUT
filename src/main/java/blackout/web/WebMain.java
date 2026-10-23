package blackout.web;

import java.io.IOException;

/** Starts the local BLACKOUT dashboard. */
public final class WebMain {
    private WebMain() { }

    public static void main(String[] args) throws IOException {
        if (args.length != 0) {
            System.out.println("Usage: java -cp target/blackout-simulator-1.0.0.jar blackout.web.WebMain");
            return;
        }
        WebDashboardServer server = new WebDashboardServer();
        server.start();
        System.out.println("BLACKOUT dashboard is running at http://127.0.0.1:" + server.getPort());
        System.out.println("Keep this window open while using the dashboard. Press Ctrl+C to stop it.");
    }
}
