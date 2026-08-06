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

import android.util.Log;

/**
 * Common configuration constants for the parallel runner.
 */
public class ParallelRunnerConfig {
    public static final int DEFAULT_MAX_WORKERS = 4;
    public static final int MAX_ALLOWED_WORKERS = 12;

    /**
     * Validates and clamps the requested worker count to a valid range [1, MAX_ALLOWED_WORKERS].
     *
     * @param count  The requested worker count.
     * @param logTag The tag to use for logging warnings if the count is out of bounds.
     * @return A valid worker count within allowed limits.
     */
    public static int clampWorkerCount(int count, String logTag) {
        if (count <= 0) {
            Log.w(logTag, "Invalid workerCount=" + count + ". Defaulting to " + DEFAULT_MAX_WORKERS);
            return DEFAULT_MAX_WORKERS;
        } else if (count > MAX_ALLOWED_WORKERS) {
            Log.w(logTag, "workerCount=" + count + " > max allowed=" + MAX_ALLOWED_WORKERS
                    + ". Defaulting to max allowed (" + MAX_ALLOWED_WORKERS + ").");
            return MAX_ALLOWED_WORKERS;
        }
        return count;
    }
}
