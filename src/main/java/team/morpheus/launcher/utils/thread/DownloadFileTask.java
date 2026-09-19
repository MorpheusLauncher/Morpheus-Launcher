package team.morpheus.launcher.utils.thread;

import lombok.Getter;
import team.morpheus.launcher.logging.MyLogger;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class DownloadFileTask implements Runnable {

    private static final MyLogger log = new MyLogger(DownloadFileTask.class);
    private static final Map<String, Object> TARGET_LOCKS = new ConcurrentHashMap<>();
    private static final Map<String, Boolean> COMPLETED_TARGETS = new ConcurrentHashMap<>();

    @Getter
    private final URL source;
    @Getter
    private final String target;
    private final AtomicInteger completed;
    private final int total;

    /**
     * Costruttore con contatore
     */
    public DownloadFileTask(URL source, String target, AtomicInteger completed, int total) {
        this.source = source;
        this.target = target;
        this.completed = completed;
        this.total = total;
    }

    /**
     * Costruttore legacy senza contatore
     */
    public DownloadFileTask(URL source, String target) {
        this(source, target, null, 0);
    }

    @Override
    public void run() {
        Path destination = Paths.get(target).toAbsolutePath();
        Object lock = TARGET_LOCKS.computeIfAbsent(destination.toString(), ignored -> new Object());
        synchronized (lock) {
            try {
                // A previous process may have left a partial/locked file behind.
                // Only skip duplicates completed successfully in this JVM.
                if (COMPLETED_TARGETS.containsKey(destination.toString())) return;
                Files.createDirectories(destination.getParent());
                IOException last = null;
                int attempt = 1;
                while (attempt <= 3) {
                    Path temporary = Files.createTempFile(destination.getParent(), ".download-", ".tmp");
                    try (InputStream in = source.openStream()) {
                        Files.copy(in, temporary, StandardCopyOption.REPLACE_EXISTING);
                        Files.move(temporary, destination, StandardCopyOption.REPLACE_EXISTING);
                        COMPLETED_TARGETS.put(destination.toString(), Boolean.TRUE);
                        String progress = (completed != null) ? String.format("[%d/%d] ", completed.incrementAndGet(), total) : "";
                        log.info(String.format("%sDownloaded: %s from: %s", progress, target, source));
                        return;
                    } catch (IOException e) {
                        last = e;
                        if (attempt == 3) throw e;
                        try {
                            Thread.sleep(150L * attempt);
                        } catch (InterruptedException interrupted) {
                            Thread.currentThread().interrupt();
                            throw new IOException("Download interrupted: " + source, interrupted);
                        }
                    } finally {
                        Files.deleteIfExists(temporary);
                    }
                    attempt++;
                }
                if (last != null) throw last;
            } catch (Exception e) {
                log.error("Download failed: " + source + " -> " + target, e);
            }
        }
    }
}
