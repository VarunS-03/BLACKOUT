package blackout.structures;

import blackout.model.RecoveryTask;
import java.util.Comparator;

public class CustomPriorityQueue {
    private final BinaryHeap<RecoveryTask> heap = new BinaryHeap<>(Comparator
            .comparingInt(RecoveryTask::getSeverity)
            .thenComparingLong(RecoveryTask::getDeadline)
            .thenComparingInt(RecoveryTask::getId));

    public void add(RecoveryTask task) {
        heap.add(task);
    }

    public RecoveryTask poll() {
        return heap.isEmpty() ? null : heap.remove();
    }

    public RecoveryTask peek() {
        return heap.isEmpty() ? null : heap.peek();
    }

    public int size() {
        return heap.size();
    }
}
