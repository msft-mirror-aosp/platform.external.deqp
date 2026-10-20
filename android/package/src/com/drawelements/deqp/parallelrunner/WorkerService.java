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
import android.content.res.AssetManager;
import android.os.IBinder;
import android.util.Log;
import android.view.Surface;
import java.util.List;

public class WorkerService extends Service {
    private static final String TAG = "WorkerService";

    static {
        try {
            System.loadLibrary("deqp");
        } catch (UnsatisfiedLinkError e) {
            Log.w(TAG, "Failed to load libdeqp.so.", e);
        }
    }

    private native void nativeStartDeqp(Surface surface, String commandLineArgs, AssetManager assetManager);

    private final ISurfaceWorker.Stub binder = new ISurfaceWorker.Stub() {
        private volatile boolean isExecuting = false;
        private final Object stateLock = new Object();
        /**
         * Executes the test batch on the provided surface.
         * This call is blocking and will not return until native dEQP execution completes.
         */
        @Override
        public boolean startTestBatch(final Surface surface, final String commandLineArgs) {
            synchronized (stateLock) {
                if (isExecuting) {
                    Log.w(TAG, "Worker is already executing a batch. Ignoring request.");
                    return false;
                }
                isExecuting = true;
            }
            try {
                Log.i(TAG, "Executing native dEQP engine...");
                if (surface != null) {
                    AssetManager assets = (getBaseContext() != null) ? getAssets() : null;
                    nativeStartDeqp(surface, commandLineArgs, assets);
                    return true;
                } else {
                    Log.w(TAG, "Cannot execute test batch: Surface is null.");
                    return false;
                }
            } catch (Exception e) {
                Log.e(TAG, "Native execution failed.", e);
                return false;
            } finally {
                synchronized (stateLock) {
                    isExecuting = false;
                }
                Log.i(TAG, "Batch finished.");
            }
        }
    };

    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.i(TAG, "Service destroyed. Terminating worker process.");
        System.exit(0);
    }

    /**
     * Returns the collection of all available worker service classes. Use this to
     * programmatically iterate over or select worker processes instead of referencing
     * subclasses directly.
     */
    private static final List<Class<? extends WorkerService>> WORKER_CLASSES = List.of(
        Worker1.class, Worker2.class, Worker3.class, Worker4.class,
        Worker5.class, Worker6.class, Worker7.class, Worker8.class,
        Worker9.class, Worker10.class, Worker11.class, Worker12.class
    );

    /**
     * Returns the worker service class for the specified index.
     * @param index index of the worker service, must be between 0 and 11 inclusive.
     * @return The Class object for the requested worker.
     */
    public static Class<? extends WorkerService> getServiceClass(int index) {
        if (index < 0 || index >= WORKER_CLASSES.size()) {
            throw new IllegalArgumentException("Invalid worker index: " + index);
        }
        return WORKER_CLASSES.get(index);
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
