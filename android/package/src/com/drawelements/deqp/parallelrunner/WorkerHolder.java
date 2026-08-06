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
import java.io.File;
import java.util.concurrent.ExecutorService;

/**
 * Encapsulates the state and lifecycle of a single parallel rendering worker.
 */
class WorkerHolder implements SurfaceHolder.Callback, WorkerServiceConnection.Callback {
    private static final String TAG = "WorkerHolder";

    interface SchedulerCallback {
        void checkAllWorkersFinished();
        /**
         * Acquires an available service ID from the pool, or {@code null} if none is available.
         */
        Integer acquireServiceId();
        void releaseServiceId(int id);
    }

    interface ConnectionFactory {
        WorkerServiceConnection create(Context context, int id, WorkerServiceConnection.Callback callback);
    }

    static ConnectionFactory connectionFactory = WorkerServiceConnection::new;

    public static void setConnectionFactory(ConnectionFactory factory) {
        connectionFactory = factory;
    }

    private final Context applicationContext;
    private final int id;
    private final DeqpTestBatchLoader testBatchLoader;
    private final String logDir;
    private final String cmdLine;
    private final ExecutorService dispatchExecutor;
    private final Object stateLock;
    private final SchedulerCallback schedulerCallback;

    private int activeServiceId = -1;
    private Surface surface;
    private WorkerServiceConnection connection;
    private boolean isBusy = false;
    private String currentBatch;

    WorkerHolder(Context context, int id, DeqpTestBatchLoader testBatchLoader,
                 String logDir, String cmdLine, ExecutorService dispatchExecutor, Object stateLock, SchedulerCallback schedulerCallback) {
        this.applicationContext = context.getApplicationContext();
        this.id = id;
        this.testBatchLoader = testBatchLoader;
        this.logDir = logDir;
        this.cmdLine = cmdLine;
        this.dispatchExecutor = dispatchExecutor;
        this.stateLock = stateLock;
        this.schedulerCallback = schedulerCallback;
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        Log.i(TAG, "Surface created for worker " + id);
        synchronized (stateLock) {
            this.surface = holder.getSurface();
        }
        safeBind();
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {}

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        Log.i(TAG, "Surface destroyed for worker " + id);
        synchronized (stateLock) {
            this.surface = null;
            if (this.currentBatch != null) {
                testBatchLoader.getBatchQueue().add(this.currentBatch);
            }
        }
        reset();
    }

    @Override
    public void onConnected(ISurfaceWorker worker) {
        final int serviceId;
        synchronized (stateLock) {
            serviceId = activeServiceId;
        }
        Log.i(TAG, "Worker " + id + " connected (service ID " + serviceId + ").");
        tryDispatch();
    }

    @Override
    public void onDisconnected() {
        final int serviceId;
        synchronized (stateLock) {
            serviceId = activeServiceId;
        }
        Log.i(TAG, "Worker " + id + " disconnected (service ID " + serviceId + ").");
        handleWorkerDisconnected();
    }

    private void safeBind() {
        synchronized (stateLock) {
            if (surface != null && surface.isValid()) {
                if (connection == null) {
                    Integer serviceId = schedulerCallback.acquireServiceId();
                    if (serviceId == null) {
                        Log.w(TAG, "Worker " + id + " cannot bind: no available service ID in pool (max allowed: "
                                + ParallelRunnerConfig.MAX_ALLOWED_WORKERS + ")");
                        return;
                    }
                    activeServiceId = serviceId;
                    connection = connectionFactory.create(applicationContext, activeServiceId, this);
                }
                connection.bind();
            } else {
                Log.w(TAG, "Worker " + id + " cannot bind: surface is " +
                      (surface == null ? "null" : "invalid"));
            }
        }
    }

    private void reset() {
        synchronized (stateLock) {
            isBusy = false;
            currentBatch = null;
            if (connection != null) {
                connection.unbind();
                connection = null;
            }
            if (activeServiceId != -1) {
                schedulerCallback.releaseServiceId(activeServiceId);
                activeServiceId = -1;
            }
        }
    }

    void onShutdown() {
        synchronized (stateLock) {
            this.surface = null;
        }
        reset();
    }

    boolean isBusy() {
        synchronized (stateLock) {
            return isBusy;
        }
    }

    boolean isBound() {
        synchronized (stateLock) {
            return connection != null && connection.isBound();
        }
    }

    private void tryDispatch() {
        String batchFile = null;
        Surface activeSurface = null;
        ISurfaceWorker activeWorker = null;
        boolean shouldUnbind = false;
        synchronized (stateLock) {
            if (isBusy) {
                Log.w(TAG, "Worker " + id + " tryDispatch failed: worker is busy");
                return;
            }
            if (surface == null || !surface.isValid()) {
                Log.w(TAG, "Worker " + id + " tryDispatch failed: surface is " +
                      (surface == null ? "null" : "invalid"));
                return;
            }
            if (connection == null) {
                Log.e(TAG, "Worker " + id + " tryDispatch failed: connection is null");
                return;
            }
            activeWorker = connection.getWorker();
            if (activeWorker == null) {
                Log.e(TAG, "Worker " + id + " tryDispatch failed: ISurfaceWorker is null");
                return;
            }
            batchFile = testBatchLoader.getBatchQueue().poll();
            if (batchFile == null) {
                shouldUnbind = true;
            } else {
                isBusy = true;
                currentBatch = batchFile;
                activeSurface = surface;
            }
        }

        if (shouldUnbind) {
            reset();
            schedulerCallback.checkAllWorkersFinished();
            return;
        }

        final Surface finalSurface = activeSurface;
        final ISurfaceWorker finalWorker = activeWorker;
        final String finalBatch = batchFile;
        String logArg = "";
        if (logDir != null && !logDir.trim().isEmpty()) {
            File logFile = new File(logDir, "TestLog_worker_" + id + ".qpa");
            logArg = "--deqp-log-filename=" + logFile.getAbsolutePath() + " ";
        }
        final String caseListArg = "--deqp-caselist-file=" + finalBatch;
        final String fullCmdLine = (cmdLine != null && !cmdLine.trim().isEmpty())
                ? cmdLine.trim() + " " + logArg + caseListArg
                : logArg + caseListArg;
        dispatchExecutor.execute(() -> {
            boolean success = false;
            try {
                success = finalWorker.startTestBatch(finalSurface, fullCmdLine);
            } catch (Exception e) {
                Log.e(TAG, "Execution failure on worker " + id, e);
            }

            if (!success) {
                handleExecutionFailure(finalBatch);
            } else {
                resetAndScheduleNext(false, null);
            }
        });
    }

    private void handleExecutionFailure(String failedBatch) {
        synchronized (stateLock) {
            if (!failedBatch.equals(currentBatch)) {
                return;
            }
        }
        resetAndScheduleNext(true, "Worker " + id + " execution failed. Scheduling next batch.");
    }

    private void handleWorkerDisconnected() {
        resetAndScheduleNext(true, null);
    }

    private void resetAndScheduleNext(boolean putBatchBack, String failureMessage) {
        synchronized (stateLock) {
            if (putBatchBack && currentBatch != null) {
                testBatchLoader.getBatchQueue().add(currentBatch);
            }
        }
        reset();
        boolean shouldBind = false;
        synchronized (stateLock) {
            if (!dispatchExecutor.isShutdown() && !testBatchLoader.getBatchQueue().isEmpty()) {
                if (failureMessage != null) {
                    Log.w(TAG, failureMessage);
                }
                shouldBind = true;
            } else {
                schedulerCallback.checkAllWorkersFinished();
            }
        }
        if (shouldBind) {
            safeBind();
        }
    }
}
