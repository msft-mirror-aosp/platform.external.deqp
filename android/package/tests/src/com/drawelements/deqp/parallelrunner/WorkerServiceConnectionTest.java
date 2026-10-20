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

import static org.easymock.EasyMock.*;
import static org.junit.Assert.*;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Unit tests for {@link WorkerServiceConnection} verifying binding lifecycle and callbacks.
 */
@RunWith(AndroidJUnit4.class)
public class WorkerServiceConnectionTest {

    private TestContext testContext;
    private WorkerServiceConnection.Callback mockCallback;

    @Before
    public void setUp() {
        testContext = new TestContext(ApplicationProvider.getApplicationContext());
        mockCallback = createMock(WorkerServiceConnection.Callback.class);
    }

    @Test
    public void testBindSuccess() {
        WorkerServiceConnection connection = new WorkerServiceConnection(testContext, 0, mockCallback);

        replay(mockCallback);

        connection.bind();

        assertTrue(connection.isBound());
        assertTrue(testContext.bindServiceCalled);
        verify(mockCallback);
    }

    @Test
    public void testBindFailure() {
        WorkerServiceConnection connection = new WorkerServiceConnection(testContext, 0, mockCallback);
        testContext.setBindServiceResult(false);

        replay(mockCallback);

        connection.bind();

        assertFalse(connection.isBound());
        assertTrue(testContext.bindServiceCalled);
        verify(mockCallback);
    }

    @Test
    public void testDoubleBindIgnored() {
        WorkerServiceConnection connection = new WorkerServiceConnection(testContext, 0, mockCallback);

        replay(mockCallback);

        connection.bind();
        testContext.bindServiceCalled = false;
        connection.bind();

        assertTrue(connection.isBound());
        assertFalse(testContext.bindServiceCalled);
        verify(mockCallback);
    }

    @Test
    public void testUnbind() {
        WorkerServiceConnection connection = new WorkerServiceConnection(testContext, 0, mockCallback);

        replay(mockCallback);

        connection.bind();
        connection.unbind();

        assertFalse(connection.isBound());
        assertNull(connection.getWorker());
        assertTrue(testContext.unbindServiceCalled);
        verify(mockCallback);
    }

    @Test
    public void testDoubleUnbindIgnored() {
        WorkerServiceConnection connection = new WorkerServiceConnection(testContext, 0, mockCallback);

        replay(mockCallback);

        connection.bind();
        connection.unbind(); // First legitimate unbind

        testContext.unbindServiceCalled = false; // Reset mock tracking flag
        connection.unbind(); // Second unbind should be ignored safely

        assertFalse(testContext.unbindServiceCalled);
        verify(mockCallback);
    }

    @Test
    public void testOnServiceConnected() {
        WorkerServiceConnection connection = new WorkerServiceConnection(testContext, 0, mockCallback);
        IBinder mockBinder = createMock(IBinder.class);
        ISurfaceWorker mockWorker = createMock(ISurfaceWorker.class);

        expect(mockBinder.queryLocalInterface(anyObject())).andReturn(mockWorker);
        mockCallback.onConnected(mockWorker);
        expectLastCall().once();

        replay(mockCallback, mockBinder, mockWorker);

        connection.bind();
        connection.onServiceConnected(null, mockBinder);

        assertEquals(mockWorker, connection.getWorker());
        verify(mockCallback, mockBinder, mockWorker);
    }

    @Test
    public void testOnServiceConnectedAfterUnbind() {
        WorkerServiceConnection connection = new WorkerServiceConnection(testContext, 0, mockCallback);
        IBinder mockBinder = createMock(IBinder.class);
        ISurfaceWorker mockWorker = createMock(ISurfaceWorker.class);

        replay(mockCallback, mockBinder, mockWorker);

        connection.bind();
        connection.unbind();
        connection.onServiceConnected(null, mockBinder);

        assertNull(connection.getWorker());
        verify(mockCallback, mockBinder, mockWorker);
    }

    @Test
    public void testOnServiceDisconnected() {
        WorkerServiceConnection connection = new WorkerServiceConnection(testContext, 0, mockCallback);
        IBinder mockBinder = createMock(IBinder.class);
        ISurfaceWorker mockWorker = createMock(ISurfaceWorker.class);

        expect(mockBinder.queryLocalInterface(anyObject())).andReturn(mockWorker);
        mockCallback.onConnected(mockWorker);
        expectLastCall().once();

        mockCallback.onDisconnected();
        expectLastCall().once();

        replay(mockCallback, mockBinder, mockWorker);

        connection.bind();
        connection.onServiceConnected(null, mockBinder);
        connection.onServiceDisconnected(null);

        assertNull(connection.getWorker());
        verify(mockCallback, mockBinder, mockWorker);
    }

    @Test
    public void testOnServiceDisconnectedAfterUnbind() {
        WorkerServiceConnection connection = new WorkerServiceConnection(testContext, 0, mockCallback);
        IBinder mockBinder = createMock(IBinder.class);
        ISurfaceWorker mockWorker = createMock(ISurfaceWorker.class);

        expect(mockBinder.queryLocalInterface(anyObject())).andReturn(mockWorker);
        mockCallback.onConnected(mockWorker);
        expectLastCall().once();

        replay(mockCallback, mockBinder, mockWorker);

        connection.bind();
        connection.onServiceConnected(null, mockBinder);
        connection.unbind();
        connection.onServiceDisconnected(null);

        assertNull(connection.getWorker());
        verify(mockCallback, mockBinder, mockWorker);
    }

    @Test
    public void testOnBindingDied() {
        WorkerServiceConnection connection = new WorkerServiceConnection(testContext, 0, mockCallback);
        mockCallback.onDisconnected();
        expectLastCall().once();

        replay(mockCallback);

        connection.bind();
        connection.onBindingDied(null);

        assertFalse(connection.isBound());
        assertNull(connection.getWorker());
        assertTrue(testContext.unbindServiceCalled);
        verify(mockCallback);
    }

    @Test
    public void testOnNullBinding() {
        WorkerServiceConnection connection = new WorkerServiceConnection(testContext, 0, mockCallback);
        mockCallback.onDisconnected();
        expectLastCall().once();

        replay(mockCallback);

        connection.bind();
        connection.onNullBinding(null);

        assertFalse(connection.isBound());
        assertNull(connection.getWorker());
        assertTrue(testContext.unbindServiceCalled);
        verify(mockCallback);
    }

    private static class TestContext extends ContextWrapper {
        private boolean bindServiceResult = true;
        private boolean bindServiceCalled = false;
        private boolean unbindServiceCalled = false;

        TestContext(Context base) {
            super(base);
        }

        @Override
        public Context getApplicationContext() {
            return this;
        }

        void setBindServiceResult(boolean result) {
            this.bindServiceResult = result;
        }

        @Override
        public boolean bindService(Intent service, ServiceConnection conn, int flags) {
            bindServiceCalled = true;
            return bindServiceResult;
        }

        @Override
        public void unbindService(ServiceConnection conn) {
            unbindServiceCalled = true;
        }
    }
}
