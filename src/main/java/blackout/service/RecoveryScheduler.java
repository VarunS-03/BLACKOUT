package blackout.service;

import blackout.model.RecoveryTask;
import blackout.structures.CustomDeque;
import blackout.structures.CustomPriorityQueue;

public class RecoveryScheduler {
    private final CustomDeque<RecoveryTask> reportedIncidents = new CustomDeque<>();
    private final CustomPriorityQueue recoveryTasks = new CustomPriorityQueue();

    public void reportIncident(RecoveryTask task, boolean urgent) {
        if (urgent) {
            reportedIncidents.addFirst(task);
        } else {
            reportedIncidents.addLast(task);
        }
    }

    public RecoveryTask dispatchNextIncident() {
        if (reportedIncidents.isEmpty()) {
            return null;
        }

        RecoveryTask task = reportedIncidents.removeFirst();
        schedule(task);
        return task;
    }

    public void schedule(RecoveryTask task) {
        recoveryTasks.add(task);
    }

    public RecoveryTask nextTask() {
        return recoveryTasks.poll();
    }

    public int pendingIncidents() {
        return reportedIncidents.size();
    }

    public int pendingTasks() {
        return recoveryTasks.size();
    }
}
