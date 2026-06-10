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

import android.view.SurfaceHolder;

import java.lang.ref.WeakReference;

/**
 * SafeSurfaceCallback is a decoupled implementation of {@link SurfaceHolder.Callback}.
 *
 * It binds a specific {@code workerId} to the callback and delegates all surface
 * lifecycle events to a {@link SurfaceLifecycleListener}. This isolates the surface
 * lifecycle from the Activity and allows easy unit testing.
 */
class SafeSurfaceCallback implements SurfaceHolder.Callback {
    private final int workerId;
    private final WeakReference<SurfaceLifecycleListener> listenerRef;

    SafeSurfaceCallback(int workerId, SurfaceLifecycleListener listener) {
        this.workerId = workerId;
        this.listenerRef = new WeakReference<>(listener);
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        SurfaceLifecycleListener listener = listenerRef.get();
        if (listener != null) {
            listener.onSurfaceCreated(workerId, holder);
        }
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        SurfaceLifecycleListener listener = listenerRef.get();
        if (listener != null) {
            listener.onSurfaceChanged(workerId, holder, format, width, height);
        }
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        SurfaceLifecycleListener listener = listenerRef.get();
        if (listener != null) {
            listener.onSurfaceDestroyed(workerId);
        }
    }
}
