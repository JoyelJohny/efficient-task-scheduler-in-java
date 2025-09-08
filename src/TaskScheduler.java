import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

public class TaskScheduler {
    private final PriorityBlockingQueue<Task> taskQueue;
    private final ThreadPoolExecutor executorService;
    private final AtomicBoolean isShutdown;
    private final AtomicLong completedTasks;
    private final AtomicLong rejectedTasks;

    public TaskScheduler(int coreWorkers, int maxWorkers) {
        this.taskQueue = new PriorityBlockingQueue<>();
        this.executorService = new ThreadPoolExecutor(
                coreWorkers, maxWorkers,
                60L, TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(),
                new ThreadFactory() {
                    private int counter = 0;

                    @Override
                    public Thread newThread(Runnable r) {
                        Thread t = new Thread(r, "TaskScheduler-Worker-" + (++counter));
                        t.setDaemon(false);
                        return t;
                    }
                });
        this.isShutdown = new AtomicBoolean(false);
        this.completedTasks = new AtomicLong(0);
        this.rejectedTasks = new AtomicLong(0);

        startTaskProcessor();
    }

    private void startTaskProcessor() {
        executorService.submit(() -> {
            while (!isShutdown.get() || !taskQueue.isEmpty()) {
                try {
                    Task task = taskQueue.poll(100, TimeUnit.MILLISECONDS);
                    if (task != null) {
                        executorService.submit(() -> {
                            try {
                                System.out.println("Executing " + task.getId() +
                                        " with priority " + task.getPriority());
                                task.execute();
                                completedTasks.incrementAndGet();
                            } catch (Exception e) {
                                System.err.println("Task execution failed: " + e.getMessage());
                            }
                        });
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        });
    }

    public boolean submitTask(Task task) {
        if (isShutdown.get()) {
            rejectedTasks.incrementAndGet();
            return false;
        }
        return taskQueue.offer(task);
    }

    public CompletableFuture<Void> submitTaskAsync(String id, int priority, Runnable action) {
        Task task = new Task(id, priority, action);
        if (submitTask(task)) {
            return CompletableFuture.runAsync(() -> {
                // This will be handled by the task processor
            }, executorService);
        }
        return CompletableFuture.failedFuture(new RejectedExecutionException("Task rejected"));
    }

    public TaskStats getStats() {
        return new TaskStats(
                taskQueue.size(),
                completedTasks.get(),
                rejectedTasks.get(),
                executorService.getActiveCount(),
                executorService.getPoolSize());
    }

    public void shutdown() {
        isShutdown.set(true);
        executorService.shutdown();
    }

    public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
        return executorService.awaitTermination(timeout, unit);
    }

    public static class TaskStats {
        public final int queueSize;
        public final long completedTasks;
        public final long rejectedTasks;
        public final int activeWorkers;
        public final int totalWorkers;

        public TaskStats(int queueSize, long completedTasks, long rejectedTasks,
                int activeWorkers, int totalWorkers) {
            this.queueSize = queueSize;
            this.completedTasks = completedTasks;
            this.rejectedTasks = rejectedTasks;
            this.activeWorkers = activeWorkers;
            this.totalWorkers = totalWorkers;
        }

        @Override
        public String toString() {
            return String.format("TaskStats{queue=%d, completed=%d, rejected=%d, active=%d, total=%d}",
                    queueSize, completedTasks, rejectedTasks, activeWorkers, totalWorkers);
        }
    }
}