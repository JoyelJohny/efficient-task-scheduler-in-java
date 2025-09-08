import java.util.concurrent.TimeUnit;

public class Main {
    public static void main(String[] args) throws InterruptedException {

        // Task scheduler example
        taskSchedulerExample();
    }

    private static void taskSchedulerExample() throws InterruptedException {
        System.out.println("=== TaskScheduler Example ===");
        TaskScheduler scheduler = new TaskScheduler(2, 4);

        // Add sample tasks
        for (int i = 1; i <= 15; i++) {
            int priority = (int) (Math.random() * 5) + 1;
            String taskId = "Task-" + i;
            scheduler.submitTask(new Task(taskId, priority, () -> {
                try {
                    Thread.sleep(300);
                    System.out.println("Completed " + taskId);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }));
        }

        // Monitor progress
        for (int i = 0; i < 10; i++) {
            System.out.println("Stats: " + scheduler.getStats());
            Thread.sleep(500);
        }

        scheduler.shutdown();
        scheduler.awaitTermination(10, TimeUnit.SECONDS);
        System.out.println("Final stats: " + scheduler.getStats());
        System.out.println("Task scheduler finished");
    }
}