package org.marrok.amriirad.core;

import javafx.application.Platform;
import javafx.concurrent.Task;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

/**
 * Manages background task execution for non-blocking UI operations.
 * Ensures database and I/O operations don't freeze the JavaFX thread.
 */
public class ConcurrencyManager {
    private static final Logger logger = LogManager.getLogger(ConcurrencyManager.class);

    private final ExecutorService executor;

    public ConcurrencyManager() {
        // Cached thread pool for short-lived tasks
        this.executor = Executors.newCachedThreadPool(r -> {
            Thread t = new Thread(r, "AmrIirad-Background");
            t.setDaemon(true);
            return t;
        });
        logger.info("ConcurrencyManager initialized");
    }

    /**
     * Execute a task in the background and handle result on JavaFX thread.
     * 
     * @param <T>            The result type
     * @param backgroundWork The work to execute in background
     * @param onSuccess      Called on JavaFX thread with the result
     * @param onError        Called on JavaFX thread with any exception
     */
    public <T> void runAsync(
            java.util.concurrent.Callable<T> backgroundWork,
            Consumer<T> onSuccess,
            Consumer<Throwable> onError) {

        Task<T> task = new Task<>() {
            @Override
            protected T call() throws Exception {
                return backgroundWork.call();
            }
        };

        task.setOnSucceeded(e -> {
            if (onSuccess != null) {
                onSuccess.accept(task.getValue());
            }
        });

        task.setOnFailed(e -> {
            Throwable ex = task.getException();
            logger.error("Background task failed", ex);
            if (onError != null) {
                onError.accept(ex);
            }
        });

        executor.submit(task);
    }

    /**
     * Execute a simple runnable in the background.
     * 
     * @param work The work to execute
     */
    public void runInBackground(Runnable work) {
        executor.submit(() -> {
            try {
                work.run();
            } catch (Exception e) {
                logger.error("Background work failed", e);
            }
        });
    }

    /**
     * Run something on the JavaFX Application Thread.
     * Safe to call from any thread.
     */
    public static void runOnUI(Runnable work) {
        if (Platform.isFxApplicationThread()) {
            work.run();
        } else {
            Platform.runLater(work);
        }
    }

    public void dispose() {
        logger.info("Shutting down ConcurrencyManager");
        executor.shutdownNow();
    }
}

