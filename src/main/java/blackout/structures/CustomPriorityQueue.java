package blackout.structures;

import blackout.model.RecoveryTask;

import java.util.Comparator;

public class CustomPriorityQueue {
    private final BinaryHeap<RecoveryTask> heap = new BinaryHeap<>(
            Comparator.comparingInt(RecoveryTask::getSeverity)
                    .thenComparingLong(RecoveryTask::getDeadline)
                    .thenComparingInt(RecoveryTask::getId));

    public void add(RecoveryTask task) {
        heap.add(task);
    }

    public RecoveryTask poll() {
        if (heap.isEmpty()) {
            return null;
        }
        return heap.remove();
    }

    public RecoveryTask peek() {
        if (heap.isEmpty()) {
            return null;
        }
        return heap.peek();
    }

    public int size() {
        return heap.size();
    }

    public boolean isEmpty() {
        return heap.isEmpty();
    }
}
