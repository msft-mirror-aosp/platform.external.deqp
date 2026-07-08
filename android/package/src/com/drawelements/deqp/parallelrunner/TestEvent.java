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

import android.os.Bundle;
import java.util.ArrayList;
import java.util.List;

/**
 * A container for accumulating Android {@link Bundle}s corresponding to a test
 * case result or test session event.
 *
 * <p>This is typically populated by a {@code com.drawelements.deqp.parallelrunner.TestSessionEventsAccumulator}
 * during different stages of test execution (e.g. start, log data, results, end) and then
 * consumed from a queue.
 */
public class TestEvent {
    private final List<Bundle> bundles;

    public TestEvent() {
        bundles = new ArrayList<>();
    }

    /**
     * Adds a non-null {@link Bundle} containing test session or test case information.
     *
     * @param bundle the Bundle to add; ignored if {@code null}.
     */
    public synchronized void addBundle(Bundle bundle) {
        if (bundle != null) {
            bundles.add(bundle);
        }
    }

    /**
     * Returns a copy of the list of {@link Bundle}s accumulated so far.
     *
     * @return a new {@link ArrayList} containing the accumulated Bundles.
     */
    public synchronized List<Bundle> getBundles() {
        return new ArrayList<>(bundles);
    }
}

