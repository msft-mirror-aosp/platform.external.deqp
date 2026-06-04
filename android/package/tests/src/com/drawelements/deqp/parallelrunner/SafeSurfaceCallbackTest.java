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
import org.easymock.EasyMock;
import org.junit.Before;
import org.junit.Test;
import static org.easymock.EasyMock.*;

/**
 * Unit tests for {@link SafeSurfaceCallback} using EasyMock.
 * These are pure JUnit tests designed to run.
 */
public class SafeSurfaceCallbackTest {

    private SurfaceHolder mockHolder;
    private SurfaceLifecycleListener mockListener;

    @Before
    public void setUp() {
        mockHolder = createMock(SurfaceHolder.class);
        mockListener = createMock(SurfaceLifecycleListener.class);
    }

    @Test
    public void testSurfaceCreated_delegatesToListener() {
        int workerId = 42;
        SafeSurfaceCallback callback = new SafeSurfaceCallback(workerId, mockListener);

        mockListener.onSurfaceCreated(workerId, mockHolder);
        expectLastCall().once();

        replay(mockHolder, mockListener);

        callback.surfaceCreated(mockHolder);

        verify(mockHolder, mockListener);
    }

    @Test
    public void testSurfaceChanged_delegatesToListener() {
        int workerId = 42;
        int format = 1;
        int width = 100;
        int height = 200;
        SafeSurfaceCallback callback = new SafeSurfaceCallback(workerId, mockListener);

        mockListener.onSurfaceChanged(workerId, mockHolder, format, width, height);
        expectLastCall().once();

        replay(mockHolder, mockListener);

        callback.surfaceChanged(mockHolder, format, width, height);

        verify(mockHolder, mockListener);
    }

    @Test
    public void testSurfaceDestroyed_delegatesToListener() {
        int workerId = 42;
        SafeSurfaceCallback callback = new SafeSurfaceCallback(workerId, mockListener);

        mockListener.onSurfaceDestroyed(workerId);
        expectLastCall().once();

        replay(mockHolder, mockListener);

        callback.surfaceDestroyed(mockHolder);

        verify(mockHolder, mockListener);
    }
}
