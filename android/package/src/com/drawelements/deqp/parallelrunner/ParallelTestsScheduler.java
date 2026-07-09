/*-------------------------------------------------------------------------
 * drawElements Quality Program Tester Core
 * ----------------------------------------
 *
 * Copyright 2026 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package com.drawelements.deqp.parallelrunner;

import android.content.Context;
import android.util.Log;
import android.view.Surface;
import android.view.SurfaceHolder;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * ParallelTestsScheduler orchestrates dEQP test execution across a 
 * pool of isolated worker processes.
 */
public class ParallelTestsScheduler implements WorkerHolder.SchedulerCallback {
    private static final String TAG = "ParallelTestsScheduler";

    public interface Callback {
        void onAllTestsCompleted();
    }

    interface WorkerHolderFactory {
        WorkerHolder create(Context context, int id, DeqpTestBatchLoader testBatchLoader,
                            ExecutorService dispatchExecutor, Object stateLock, WorkerHolder.SchedulerCallback schedulerCallback);
    }

    private static WorkerHolderFactory workerHolderFactory = WorkerHolder::new;

    // Visible for testing
    static void setWorkerHolderFactory(WorkerHolderFactory factory) {
        workerHolderFactory = factory;
    }

    // Visible for testing
    SurfaceHolder.Callback getWorkerCallback(int workerId) {
        return workers.get(workerId);
    }

    /**
     * Registers the worker's surface callback to the given SurfaceHolder.
     */
    public void registerSurface(int workerId, SurfaceHolder holder) {
        holder.addCallback(workers.get(workerId));
    }
    private final Context context;
    private final DeqpTestBatchLoader testBatchLoader;
    private final Callback completionCallback;
    private final ExecutorService dispatchExecutor;
    private final List<WorkerHolder> workers = new ArrayList<>();
    private final Object stateLock = new Object();
    private boolean callbackFired = false;


    public ParallelTestsScheduler(Context context, int workerCount, DeqpTestBatchLoader testBatchLoader, Callback callback) {
        this.context = context.getApplicationContext();
        this.testBatchLoader = testBatchLoader;
        this.completionCallback = callback;
        this.dispatchExecutor = Executors.newFixedThreadPool(workerCount);

        for (int i = 0; i < workerCount; i++) {
            workers.add(workerHolderFactory.create(this.context, i, testBatchLoader, dispatchExecutor, stateLock, this));
        }
    }

    @Override
    public void checkAllWorkersFinished() {
        boolean triggerCallback = false;
        synchronized (stateLock) {
            if (callbackFired) return;

            boolean allIdle = workers.stream().noneMatch(w -> w.isBusy() || w.isBound());
            if (allIdle && testBatchLoader.getBatchQueue().isEmpty()) {
                callbackFired = true;
                triggerCallback = true;
            }
        }

        if (triggerCallback && completionCallback != null) {
            completionCallback.onAllTestsCompleted();
        }
    }

    public void shutdown() {
        dispatchExecutor.shutdownNow();
        for (WorkerHolder holder : workers) {
            holder.onShutdown();
        }
    }
}