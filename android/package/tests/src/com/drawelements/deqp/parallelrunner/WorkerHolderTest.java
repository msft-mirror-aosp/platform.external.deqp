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

import android.content.Context;
import android.view.Surface;
import android.view.SurfaceHolder;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import java.io.File;
import java.util.concurrent.ExecutorService;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import static org.easymock.EasyMock.*;
import static org.junit.Assert.*;

/**
 * Unit tests for {@link WorkerHolder} verifying its standalone state machine tracking,
 * surface lifecycle callbacks, and test batch dispatching logic.
 */
@RunWith(AndroidJUnit4.class)
public class WorkerHolderTest {

    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();

    private Context context;
    private DeqpTestBatchLoader batchLoader;
    private WorkerHolder.SchedulerCallback mockSchedulerCallback;
    private ExecutorService directExecutor;
    private final Object stateLock = new Object();

    private static class TestSurface extends Surface {
        @Override
        public boolean isValid() {
            return true;
        }
    }

    private static class TestWorkerServiceConnection extends WorkerServiceConnection {
        boolean bindCalled = false;
        boolean unbindCalled = false;
        boolean isBoundVal = false;
        ISurfaceWorker workerVal = null;

        TestWorkerServiceConnection(Context context, int workerId, Callback callback) {
            super(context, workerId, callback);
        }

        @Override
        public synchronized void bind() {
            bindCalled = true;
            isBoundVal = true;
        }

        @Override
        public synchronized void unbind() {
            unbindCalled = true;
            isBoundVal = false;
            workerVal = null;
        }

        @Override
        public synchronized boolean isBound() {
            return isBoundVal;
        }

        @Override
        public synchronized ISurfaceWorker getWorker() {
            return workerVal;
        }
    }

    @Before
    public void setUp() {
        context = ApplicationProvider.getApplicationContext();
        batchLoader = new DeqpTestBatchLoader();
        mockSchedulerCallback = createMock(WorkerHolder.SchedulerCallback.class);

        // Use a direct executor to run background tasks synchronously in tests
        directExecutor = createMock(ExecutorService.class);
        expect(directExecutor.isShutdown()).andReturn(false).anyTimes();

        // When task is executed, run it directly on the calling thread
        directExecutor.execute(anyObject());
        expectLastCall().andAnswer(() -> {
            Runnable r = (Runnable) getCurrentArguments()[0];
            r.run();
            return null;
        }).anyTimes();

        replay(directExecutor);
    }

    private void setupBatchLoaderWithFiles(String... filenames) throws Exception {
        File rootDir = tempFolder.getRoot();
        for (String filename : filenames) {
            new File(rootDir, filename).createNewFile();
        }
        batchLoader.loadFromDirectory(rootDir.getAbsolutePath());
    }

    private TestWorkerServiceConnection[] registerMockConnectionFactory() {
        final TestWorkerServiceConnection[] capturedConnection = new TestWorkerServiceConnection[1];
        WorkerHolder.setConnectionFactory((ctx, id, cb) -> {
            capturedConnection[0] = new TestWorkerServiceConnection(ctx, id, cb);
            return capturedConnection[0];
        });
        return capturedConnection;
    }

    private SurfaceHolder createMockSurfaceHolder(Surface surface) {
        SurfaceHolder mockHolder = createMock(SurfaceHolder.class);
        expect(mockHolder.getSurface()).andReturn(surface).anyTimes();
        replay(mockHolder);
        return mockHolder;
    }

    @After
    public void tearDown() {
        WorkerHolder.setConnectionFactory(WorkerServiceConnection::new);
    }

    @Test
    public void testSurfaceCreatedTriggersBind() {
        TestWorkerServiceConnection[] capturedConnection = registerMockConnectionFactory();
        WorkerHolder holder = new WorkerHolder(context, 0, batchLoader, directExecutor, stateLock, mockSchedulerCallback);

        SurfaceHolder mockHolder = createMockSurfaceHolder(new TestSurface());

        holder.surfaceCreated(mockHolder);

        assertNotNull(capturedConnection[0]);
        assertTrue(capturedConnection[0].bindCalled);
    }

    @Test
    public void testSurfaceDestroyedTriggersUnbindAndPutsBatchBack() throws Exception {
        setupBatchLoaderWithFiles("batch_1.txt");
        ISurfaceWorker mockWorker = createMock(ISurfaceWorker.class);
        TestSurface testSurface = new TestSurface();
        TestWorkerServiceConnection[] capturedConnection = registerMockConnectionFactory();

        WorkerHolder holder = new WorkerHolder(context, 0, batchLoader, directExecutor, stateLock, mockSchedulerCallback);
        SurfaceHolder mockHolder = createMockSurfaceHolder(testSurface);
        holder.surfaceCreated(mockHolder);

        // When startTestBatch is called, simulate surface destruction
        expect(mockWorker.startTestBatch(eq(testSurface), contains("batch_1.txt"))).andAnswer(() -> {
            holder.surfaceDestroyed(mockHolder);
            return true;
        }).once();

        replay(mockWorker, mockSchedulerCallback);

        capturedConnection[0].workerVal = mockWorker;
        holder.onConnected(mockWorker);

        verify(mockWorker, mockSchedulerCallback);
        assertTrue(capturedConnection[0].unbindCalled);

        // The batch should be put back in the queue
        String expectedPath = new File(tempFolder.getRoot(), "batch_1.txt").getAbsolutePath();
        assertEquals(expectedPath, batchLoader.getBatchQueue().poll());
    }

    @Test
    public void testOnConnectedDispatchesBatch() throws Exception {
        setupBatchLoaderWithFiles("batch_1.txt");
        ISurfaceWorker mockWorker = createMock(ISurfaceWorker.class);
        TestSurface testSurface = new TestSurface();
        TestWorkerServiceConnection[] capturedConnection = registerMockConnectionFactory();

        WorkerHolder holder = new WorkerHolder(context, 0, batchLoader, directExecutor, stateLock, mockSchedulerCallback);

        // Set valid surface
        SurfaceHolder mockHolder = createMockSurfaceHolder(testSurface);
        holder.surfaceCreated(mockHolder);

        expect(mockWorker.startTestBatch(eq(testSurface), contains("batch_1.txt"))).andReturn(true).once();
        mockSchedulerCallback.checkAllWorkersFinished();
        expectLastCall().once();

        replay(mockWorker, mockSchedulerCallback);

        // Trigger connection
        capturedConnection[0].workerVal = mockWorker;
        holder.onConnected(mockWorker);

        verify(mockWorker, mockSchedulerCallback);
        assertTrue(capturedConnection[0].unbindCalled);
    }

    @Test
    public void testQueueExhaustionCallsCheckAllWorkersFinished() throws Exception {
        // No files loaded, batch queue is empty
        TestWorkerServiceConnection[] capturedConnection = registerMockConnectionFactory();
        WorkerHolder holder = new WorkerHolder(context, 0, batchLoader, directExecutor, stateLock, mockSchedulerCallback);

        SurfaceHolder mockHolder = createMockSurfaceHolder(new TestSurface());
        holder.surfaceCreated(mockHolder);

        mockSchedulerCallback.checkAllWorkersFinished();
        expectLastCall().once();

        replay(mockSchedulerCallback);

        capturedConnection[0].workerVal = createMock(ISurfaceWorker.class);
        holder.onConnected(capturedConnection[0].workerVal);

        verify(mockSchedulerCallback);
        assertTrue(capturedConnection[0].unbindCalled);
    }

    @Test
    public void testExecutionFailureUnbindsAndSafeBinds() throws Exception {
        setupBatchLoaderWithFiles("batch_1.txt", "batch_2.txt");
        ISurfaceWorker mockWorker = createMock(ISurfaceWorker.class);
        TestSurface testSurface = new TestSurface();
        TestWorkerServiceConnection[] capturedConnection = registerMockConnectionFactory();

        WorkerHolder holder = new WorkerHolder(context, 0, batchLoader, directExecutor, stateLock, mockSchedulerCallback);
        SurfaceHolder mockHolder = createMockSurfaceHolder(testSurface);
        holder.surfaceCreated(mockHolder);

        // Reset tracking
        capturedConnection[0].bindCalled = false;

        // startTestBatch fails
        expect(mockWorker.startTestBatch(eq(testSurface), contains("batch_1.txt"))).andReturn(false).once();

        replay(mockWorker, mockSchedulerCallback);

        capturedConnection[0].workerVal = mockWorker;
        holder.onConnected(mockWorker);

        verify(mockWorker, mockSchedulerCallback);
        assertTrue(capturedConnection[0].unbindCalled);
        // It should attempt to bind again to try the next batch
        assertTrue(capturedConnection[0].bindCalled);

        // Verify the failed batch is returned to the queue
        String expectedBatch2 = new File(tempFolder.getRoot(), "batch_2.txt").getAbsolutePath();
        String expectedBatch1 = new File(tempFolder.getRoot(), "batch_1.txt").getAbsolutePath();
        assertEquals(expectedBatch2, batchLoader.getBatchQueue().poll());
        assertEquals(expectedBatch1, batchLoader.getBatchQueue().poll());
    }

    @Test
    public void testWorkerDisconnectedTriggersUnbindAndPutsBatchBack() throws Exception {
        setupBatchLoaderWithFiles("batch_1.txt", "batch_2.txt");
        ISurfaceWorker mockWorker = createMock(ISurfaceWorker.class);
        TestSurface testSurface = new TestSurface();
        TestWorkerServiceConnection[] capturedConnection = registerMockConnectionFactory();

        WorkerHolder holder = new WorkerHolder(context, 0, batchLoader, directExecutor, stateLock, mockSchedulerCallback);
        SurfaceHolder mockHolder = createMockSurfaceHolder(testSurface);
        holder.surfaceCreated(mockHolder);

        // Reset tracking
        capturedConnection[0].bindCalled = false;

        // startTestBatch simulates disconnect by calling onDisconnected()
        expect(mockWorker.startTestBatch(eq(testSurface), contains("batch_1.txt"))).andAnswer(() -> {
            holder.onDisconnected();
            return false;
        }).once();

        replay(mockWorker, mockSchedulerCallback);

        capturedConnection[0].workerVal = mockWorker;
        holder.onConnected(mockWorker);

        verify(mockWorker, mockSchedulerCallback);
        assertTrue(capturedConnection[0].unbindCalled);
        // It should attempt to bind again to try the next batch
        assertTrue(capturedConnection[0].bindCalled);

        // Verify the crashed batch is returned to the queue
        String expectedBatch2 = new File(tempFolder.getRoot(), "batch_2.txt").getAbsolutePath();
        String expectedBatch1 = new File(tempFolder.getRoot(), "batch_1.txt").getAbsolutePath();
        assertEquals(expectedBatch2, batchLoader.getBatchQueue().poll());
        assertEquals(expectedBatch1, batchLoader.getBatchQueue().poll());
    }
}
