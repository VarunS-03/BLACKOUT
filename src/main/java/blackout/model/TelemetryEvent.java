package blackout.model;
public class TelemetryEvent {
    private final long timestamp;
    private final int serverId;
    private final String metric;
    private final double value;
    public TelemetryEvent(long timestamp, int serverId, String metric, double value) {
        this.timestamp = timestamp; this.serverId = serverId; this.metric = metric; this.value = value;
    }
    public long getTimestamp() { return timestamp; }
    public int getServerId() { return serverId; }
    public String getMetric() { return metric; }
    public double getValue() { return value; }
    @Override public String toString() { return timestamp + "|server=" + serverId + "|" + metric + "|" + value; }
}
