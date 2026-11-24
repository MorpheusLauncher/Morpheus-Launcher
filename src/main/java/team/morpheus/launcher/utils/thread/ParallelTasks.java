package team.morpheus.launcher.utils.thread;

import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;

public class ParallelTasks {

    private static final int DEFAULT_THREADS = 16;
    private static final int DOWNLOAD_THREADS = configuredThreads();

    private static int configuredThreads() {
        String value = System.getenv("DOWNLOAD_THREADS");
        if (value != null) {
            try {
                int threads = Integer.parseInt(value.trim());
                if (threads >= 1 && threads <= 32) return threads;
            } catch (NumberFormatException ignored) {
                // Use the default for malformed settings.
            }
        }
        return DEFAULT_THREADS;
    }

    private final Collection<Runnable> tasks = new ArrayList<>();

    private BiConsumer<Integer, Integer> onProgress;

    public void add(final Runnable task) {
        tasks.add(task);
    }

    public void go() throws InterruptedException {
        go(new ForkJoinPool(DOWNLOAD_THREADS));
    }

    // Each batch owns its executor and shuts it down after completion.
    void go(ExecutorService threads) throws InterruptedException {
        final int total = tasks.size();
        final AtomicInteger completed = new AtomicInteger(0);

        try {
            final CountDownLatch latch = new CountDownLatch(total);
            for (final Runnable task : tasks) {
                final Runnable wrappedTask = (task instanceof DownloadFileTask) ? new DownloadFileTask(((DownloadFileTask) task).getSource(), ((DownloadFileTask) task).getTarget(), completed, total) : task;

                threads.execute(() -> {
                    try {
                        wrappedTask.run();
                        if (task instanceof DownloadFileTask && onProgress != null) {
                            onProgress.accept(completed.get(), total);
                        }
                    } finally {
                        if (!(task instanceof DownloadFileTask)) {
                            int done = completed.incrementAndGet();
                            if (onProgress != null) onProgress.accept(done, total);
                        }
                        latch.countDown();
                    }
                });
            }
            latch.await();
        } finally {
            threads.shutdown();
        }
    }
}