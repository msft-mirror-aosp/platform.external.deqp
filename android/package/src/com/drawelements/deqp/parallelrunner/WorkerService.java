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

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.view.Surface;
import android.util.Log;

public class WorkerService extends Service {
    private static final String TAG = "WorkerService";

    static {
        try {
            System.loadLibrary("deqp");
        } catch (UnsatisfiedLinkError e) {
            Log.w(TAG, "Failed to load libdeqp.so.", e);
        }
    }

    private final ISurfaceWorker.Stub binder = new ISurfaceWorker.Stub() {
        private volatile boolean isExecuting = false;
        private final Object stateLock = new Object();
        @Override
        public boolean startTestBatch(final Surface surface, final String commandLineArgs) {
            synchronized (stateLock) {
                if (isExecuting) {
                    Log.w(TAG, "Worker is already executing a batch. Ignoring request.");
                    return false;
                }
                isExecuting = true;
            }
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        Log.i(TAG, "Executing native dEQP engine in background...");
                        // TODO: Invoke native C++ execution once framework API is available
                    } finally {
                        synchronized (stateLock) {
                            isExecuting = false;
                        }
                        Log.i(TAG, "Batch finished. Terminating worker process.");
                    }
                }
            }, "dEQP-Worker-" + WorkerService.this.getClass().getSimpleName()).start();
            return true;
        }
    };

    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    /**
     * Returns the collection of all available worker service classes. Use this to
     * programmatically iterate over or select worker processes instead of referencing
     * subclasses directly.
     */
    private static final Class<?>[] WORKER_CLASSES = {
        Worker1.class, Worker2.class, Worker3.class, Worker4.class,
        Worker5.class, Worker6.class, Worker7.class, Worker8.class,
        Worker9.class, Worker10.class, Worker11.class, Worker12.class
    };

    /**
     * Returns the worker service class for the specified index.
     * @param index index of the worker service, must be between 0 and 11 inclusive.
     * @return The Class object for the requested worker.
     */
    public static Class<? extends WorkerService> getServiceClass(int index) {
        if (index < 0 || index >= WORKER_CLASSES.length) {
            throw new IllegalArgumentException("Invalid worker index: " + index);
        }
        // This cast is safe because WORKER_CLASSES is typed as Class<? extends WorkerService>
        @SuppressWarnings("unchecked")
        Class<? extends WorkerService> workerClass = (Class<? extends WorkerService>) WORKER_CLASSES[index];
        return workerClass;
    }

    /**
     * Subclasses defined for isolated process declaration in AndroidManifest.xml.
     * These must remain public for the system to instantiate them, but they should
     * not be accessed directly by code; use {@link #getServiceClass} instead.
     */
    public static class Worker1 extends WorkerService {}
    public static class Worker2 extends WorkerService {}
    public static class Worker3 extends WorkerService {}
    public static class Worker4 extends WorkerService {}
    public static class Worker5 extends WorkerService {}
    public static class Worker6 extends WorkerService {}
    public static class Worker7 extends WorkerService {}
    public static class Worker8 extends WorkerService {}
    public static class Worker9 extends WorkerService {}
    public static class Worker10 extends WorkerService {}
    public static class Worker11 extends WorkerService {}
    public static class Worker12 extends WorkerService {}
}
