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

/**
 * Callback interface for monitoring the lifecycle of a Surface within the parallel runner.
 * By relying on this interface, the Callback system is fully decoupled from the Activity
 * and can be easily unit-tested in isolation.
 */
public interface SurfaceLifecycleListener {
    /**
     * Called when the surface is created and is ready for graphics rendering.
     *
     * @param workerId The unique identifier of the worker assigned to this surface.
     * @param holder   The SurfaceHolder providing access to the underlying surface.
     */
    void onSurfaceCreated(int workerId, SurfaceHolder holder);

    /**
     * Called when the surface structural changes (format or dimensions) occur.
     *
     * @param workerId The unique identifier of the worker assigned to this surface.
     * @param holder   The SurfaceHolder providing access to the underlying surface.
     * @param format   The new PixelFormat of the surface.
     * @param width    The new width of the surface in pixels.
     * @param height   The new height of the surface in pixels.
     */
    void onSurfaceChanged(int workerId, SurfaceHolder holder, int format, int width, int height);

    /**
     * Called when the surface is destroyed and is no longer available for rendering.
     * All rendering loops to this surface must be halted before returning from this method.
     *
     * @param workerId The unique identifier of the worker assigned to this surface.
     */
    void onSurfaceDestroyed(int workerId);
}
