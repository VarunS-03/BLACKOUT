package blackout.service;

import blackout.model.RecoveryTask;
import blackout.structures.CustomPriorityQueue;

public class RecoveryScheduler {
    private final CustomPriorityQueue queue = new CustomPriorityQueue();

    public void schedule(RecoveryTask task) {
        queue.add(task);
    }

    public RecoveryTask nextTask() {
        return queue.poll();
    }

    public int pendingTasks() {
        return queue.size();
    }
}
