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

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.util.Log;

/**
 * WorkerServiceConnection manages the binder lifecycle connection to a specific WorkerService.
 *
 * It is responsible for binding to the service associated with a given workerId,
 * caching the resulting ISurfaceWorker binder stub, and exposing callback hooks
 * to notify components when connection status changes.
 */
public class WorkerServiceConnection implements ServiceConnection {
    private static final String TAG = "WorkerServiceConnection";

    /**
     * Callback interface to listen for connection lifecycle events.
     */
    public interface Callback {
        /**
         * Called when the connection to the worker service has been successfully established.
         *
         * This method is called on the application's main thread. Implementations should
         * avoid performing heavy or long-running operations directly in this callback
         * to prevent blocking the main thread. Consider offloading work to a background thread.
         *
         * @param worker The active ISurfaceWorker interface.
         */
        void onConnected(ISurfaceWorker worker);

        /**
         * Called when the connection to the worker service is lost.
         */
        void onDisconnected();
    }

    private enum BindState { UNBOUND, BINDING, BOUND }

    private final Context context;
    private final int workerId;
    private final Callback callback;

    private ISurfaceWorker worker;
    private BindState state = BindState.UNBOUND;

    /**
     * Constructs a new WorkerServiceConnection.
     *
     * @param context The android context used to bind/unbind the service.
     * @param workerId The index representing the targeted rendering worker.
     * @param callback Optional callback interface to receive connection notifications.
     */
    public WorkerServiceConnection(Context context, int workerId, Callback callback) {
        this.context = context.getApplicationContext();
        this.workerId = workerId;
        this.callback = callback;
    }

    /**
     * Attempts to establish a connection to the WorkerService.
     * If the service is already bound or binding, this operation is ignored.
     */
    public synchronized void bind() {
        if (state != BindState.UNBOUND) {
            Log.w(TAG, "Worker " + workerId + " already bound or binding.");
            return;
        }

        state = BindState.BINDING;
        boolean success = false;
        try {
            Class<? extends WorkerService> serviceClass = WorkerService.getServiceClass(workerId);
            Intent intent = new Intent(context, serviceClass);
            success = context.bindService(intent, this, Context.BIND_AUTO_CREATE);
        } catch (IllegalArgumentException e) {
            Log.e(TAG, "Exception while binding to Worker service for worker ID: " + workerId, e);
        }

        if (!success) {
            Log.e(TAG, "Failed to bind to Worker service for worker ID: " + workerId);
            state = BindState.UNBOUND;
        }
    }

    /**
     * Disconnects and unbinds from the WorkerService, releasing the binder.
     */
    public synchronized void unbind() {
        if (state == BindState.UNBOUND) {
            Log.w(TAG, "Worker " + workerId + " not bound.");
            return;
        }
        try {
            context.unbindService(this);
        } catch (IllegalArgumentException e) {
            Log.w(TAG, "Worker " + workerId + " unbind failed.", e);
        } finally {
            state = BindState.UNBOUND;
            worker = null;
        }
    }

    public synchronized boolean isBound() {
        return state == BindState.BOUND || state == BindState.BINDING;
    }

    public synchronized ISurfaceWorker getWorker() {
        return worker;
    }

    @Override
    public void onServiceConnected(ComponentName name, IBinder service) {
        ISurfaceWorker localWorker;
        synchronized (this) {
            if (state != BindState.BINDING && state != BindState.BOUND) {
                Log.w(TAG, "onServiceConnected called when not binding/bound for worker ID: " + workerId);
                return;
            }
            state = BindState.BOUND;
            worker = ISurfaceWorker.Stub.asInterface(service);
            localWorker = worker;
        }
        if (callback != null) {
            callback.onConnected(localWorker);
        }
    }

    @Override
    public void onServiceDisconnected(ComponentName name) {
        // Transient loss of connection. The system will attempt to reconnect. Do not unbind.
        boolean wasBound;
        synchronized (this) {
            wasBound = (state == BindState.BOUND);
            worker = null;
        }
        if (wasBound && callback != null) {
            callback.onDisconnected();
        }
    }

    @Override
    public void onBindingDied(ComponentName name) {
        handleFatalDisconnection("onBindingDied");
    }

    @Override
    public void onNullBinding(ComponentName name) {
        handleFatalDisconnection("onNullBinding");
    }

    private void handleFatalDisconnection(String reason) {
        boolean wasBoundOrBinding;
        synchronized (this) {
            wasBoundOrBinding = (state != BindState.UNBOUND);
            if (wasBoundOrBinding) {
                try {
                    context.unbindService(this);
                } catch (IllegalArgumentException e) {
                    Log.w(TAG, "Unbind failed in " + reason, e);
                }
                state = BindState.UNBOUND;
                worker = null;
            }
        }
        if (wasBoundOrBinding && callback != null) {
            callback.onDisconnected();
        }
    }
}
