package com.atlassian.mobilekit.module.core;


import androidx.annotation.VisibleForTesting;

import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


public final class JobQueue {

    private final ExecutorService executor;

    public JobQueue() {
        executor = Executors.newFixedThreadPool(5);
    }

    public final void enqueue(Runnable r) {
        executor.execute(r);
    }

    @VisibleForTesting
    public Executor getExecutor() {
        return executor;
    }
}
