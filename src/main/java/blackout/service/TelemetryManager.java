package blackout.service;

import blackout.model.TelemetryEvent;
import blackout.structures.CircularQueue;
import blackout.structures.SegmentTree;

import java.util.Arrays;

public class TelemetryManager {
    private final CircularQueue<TelemetryEvent> eventBuffer;
    private int[] loadSamples = new int[0];
    private SegmentTree loadTree = new SegmentTree(loadSamples);

    public TelemetryManager(int eventCapacity) {
        eventBuffer = new CircularQueue<>(eventCapacity);
    }

    public boolean record(TelemetryEvent event) {
        return eventBuffer.offer(event);
    }

    public TelemetryEvent readNext() {
        return eventBuffer.poll();
    }

    public int bufferedEvents() {
        return eventBuffer.size();
    }

    public int eventCapacity() {
        return eventBuffer.capacity();
    }

    public void setLoadSamples(int[] samples) {
        loadSamples = Arrays.copyOf(samples, samples.length);
        loadTree = new SegmentTree(loadSamples);
    }

    public long totalLoad(int firstIndex, int lastIndex) {
        return loadTree.rangeSum(firstIndex, lastIndex);
    }

    public void updateLoad(int index, int value) {
        loadTree.update(index, value);
        loadSamples[index] = value;
    }

    public int[] loadSamples() {
        return Arrays.copyOf(loadSamples, loadSamples.length);
    }
}
