public class Task implements Comparable<Task> {
    private final String id;
    private final int priority;
    private final Runnable action;
    private final long timeStamp;

    public Task(String id, int priority, Runnable action) {
        this.id = id;
        this.priority = priority;
        this.action = action;
        this.timeStamp = System.currentTimeMillis();
    }

    @Override
    public int compareTo(Task other) {

        // Run Higher Priority task first
        if (this.priority != other.priority) {
            return Integer.compare(other.priority, this.priority);
        }

        return Long.compare(this.timeStamp, other.timeStamp); // Run Task based on FIFO if priority is same
    }

    public void execute() {
        try {
            this.action.run();
        } catch (Exception e) {
            System.err.println("Error executing Task-" + this.id + " : " + e.getMessage());
            e.printStackTrace();
        }
    }

    // Getter Methods
    public String getId() {
        return this.id;
    }

    public int getPriority() {
        return this.priority;
    }

    public long getTimeStamp() {
        return this.timeStamp;
    }
}