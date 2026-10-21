package blackout.model;
public class Job {
    private final int id;
    private final String description;
    private final int priority;
    private boolean completed;
    public Job(int id, String description, int priority) {
        this.id = id; this.description = description; this.priority = priority;
    }
    public int getId() { return id; }
    public String getDescription() { return description; }
    public int getPriority() { return priority; }
    public boolean isCompleted() { return completed; }
    public void setCompleted(boolean completed) { this.completed = completed; }
    @Override public String toString() { return "Job{" + id + ", " + description + ", priority=" + priority + "}"; }
}
