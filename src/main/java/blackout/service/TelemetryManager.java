package blackout.service;

import blackout.model.TelemetryEvent;
import blackout.structures.CircularQueue;

public class TelemetryManager {
    private final CircularQueue<TelemetryEvent> buffer;

    public TelemetryManager(int capacity) {
        buffer = new CircularQueue<>(capacity);
    }

    public boolean record(TelemetryEvent event) {
        return buffer.offer(event);
    }

    public TelemetryEvent readNext() {
        return buffer.poll();
    }

    public int bufferedEvents() {
        return buffer.size();
    }
}
