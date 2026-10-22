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

import android.os.DeadObjectException;
import android.os.RemoteException;
import android.util.Log;
import com.drawelements.deqp.testercore.TestEventListener;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Forwards native dEQP test events received via JNI to a remote {@link ITestEventCallback},
 * allowing a worker process to report test progress directly to the coordinator over Binder IPC.
 *
 * <p>If the remote callback dies (signalled by a {@link DeadObjectException}), the failure is
 * logged once and all subsequent events are dropped.
 */
public final class NativeTestEventBridge implements TestEventListener {
    private static final String TAG = "NativeTestEventBridge";
    private final ITestEventCallback callback;
    private final AtomicBoolean isCallbackDead = new AtomicBoolean(false);

    /** A single Binder call to the remote callback. */
    @FunctionalInterface
    private interface RemoteCall {
        void run() throws RemoteException;
    }

    public NativeTestEventBridge(ITestEventCallback callback) {
        if (callback == null) {
            throw new IllegalArgumentException("callback cannot be null");
        }
        this.callback = callback;
    }

    @Override
    public void beginSession() {
        dispatch("beginSession", callback::beginSession);
    }

    @Override
    public void endSession() {
        dispatch("endSession", callback::endSession);
    }

    @Override
    public void sessionInfo(String name, String value) {
        dispatch("sessionInfo", () -> callback.sessionInfo(name, value));
    }

    @Override
    public void beginTestCase(String testCase) {
        dispatch("beginTestCase", () -> callback.beginTestCase(testCase));
    }

    @Override
    public void endTestCase() {
        dispatch("endTestCase", callback::endTestCase);
    }

    @Override
    public void testCaseResult(String code, String details) {
        dispatch("testCaseResult", () -> callback.testCaseResult(code, details));
    }

    @Override
    public void terminateTestCase(String reason) {
        dispatch("terminateTestCase", () -> callback.terminateTestCase(reason));
    }

    @Override
    public void testLogData(String log) {
        // Ignored in AIDL-JNI approach
    }

    /**
     * Invokes {@code call} on the remote callback unless it is already known to be dead.
     *
     * <p>A {@link DeadObjectException} marks the callback as dead and is logged only once. Any
     * other exception is logged and swallowed so that it never propagates back into native code.
     */
    private void dispatch(String eventName, RemoteCall call) {
        if (isCallbackDead.get()) {
            return;
        }
        try {
            call.run();
        } catch (DeadObjectException e) {
            if (isCallbackDead.compareAndSet(false, true)) {
                Log.w(TAG, "Remote callback died during " + eventName
                        + "; dropping all subsequent test events.", e);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error in " + eventName, e);
        }
    }
}
