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
import java.util.ArrayList;
import java.util.List;
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
    private LogParsersCoordinator mockCoordinator;
    private ExecutorService directExecutor;
    private final Object stateLock = new Object();

    private static class TestSurface extends Surface {
        boolean valid = true;

        @Override
        public boolean isValid() {
            return valid;
        }
    }

    private static class TestSchedulerCallback implements WorkerHolder.SchedulerCallback {
        boolean checkAllWorkersFinishedCalled = false;
        int acquiredCount = 0;
        int releasedCount = 0;

        @Override
        public void checkAllWorkersFinished() {
            checkAllWorkersFinishedCalled = true;
        }

        @Override
        public Integer acquireServiceId() {
            return acquiredCount++;
        }

        @Override
        public void releaseServiceId(int id) {
            releasedCount++;
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
        mockCoordinator = createMock(LogParsersCoordinator.class);

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

    private List<TestWorkerServiceConnection> registerMockConnectionFactory() {
        final List<TestWorkerServiceConnection> capturedConnections = new ArrayList<>();
        WorkerHolder.setConnectionFactory((context, workerId, callback) -> {
            TestWorkerServiceConnection conn =
                    new TestWorkerServiceConnection(context, workerId, callback);
            capturedConnections.add(conn);
            return conn;
        });
        return capturedConnections;
    }

    private SurfaceHolder createMockSurfaceHolder(Surface surface) {
        SurfaceHolder mockHolder = createMock(SurfaceHolder.class);
        expect(mockHolder.getSurface()).andReturn(surface).anyTimes();
        replay(mockHolder);
        return mockHolder;
    }

    private static final String TEST_LOG_DIR = "/sdcard/deqpparallel/logs/";
    private static final String TEST_CMD_LINE = "--deqp-gl-config-name=rgba8888d24s8";

    @After
    public void tearDown() {
        WorkerHolder.setConnectionFactory(WorkerServiceConnection::new);
        AsyncLogParsersCoordinator.reset();
    }

    @Test
    public void testSurfaceCreatedTriggersBind() {
        TestSchedulerCallback callback = new TestSchedulerCallback();
        List<TestWorkerServiceConnection> capturedConnections = registerMockConnectionFactory();
        WorkerHolder holder = new WorkerHolder(context, 0, batchLoader, TEST_LOG_DIR, TEST_CMD_LINE, directExecutor, stateLock, callback, mockCoordinator);

        SurfaceHolder mockHolder = createMockSurfaceHolder(new TestSurface());

        holder.surfaceCreated(mockHolder);

        assertEquals(1, capturedConnections.size());
        assertTrue(capturedConnections.get(0).bindCalled);
        assertEquals(1, callback.acquiredCount);
    }

    @Test
    public void testSurfaceDestroyedTriggersUnbind() throws Exception {
        setupBatchLoaderWithFiles("batch_1.txt");
        ISurfaceWorker mockWorker = createMock(ISurfaceWorker.class);
        TestSurface testSurface = new TestSurface();
        List<TestWorkerServiceConnection> capturedConnections = registerMockConnectionFactory();
        TestSchedulerCallback callback = new TestSchedulerCallback();

        WorkerHolder holder = new WorkerHolder(context, 0, batchLoader, TEST_LOG_DIR, TEST_CMD_LINE, directExecutor, stateLock, callback, mockCoordinator);
        SurfaceHolder mockHolder = createMockSurfaceHolder(testSurface);
        holder.surfaceCreated(mockHolder);

        // When startTestBatch is called, simulate surface destruction
        expect(mockWorker.startTestBatch(eq(testSurface), contains("batch_1.txt"))).andAnswer(() -> {
            holder.surfaceDestroyed(mockHolder);
            return true;
        }).once();
        mockCoordinator.parse(anyObject());
        expectLastCall().once();
        mockCoordinator.onTestProcessFinished(anyObject());
        expectLastCall().once();

        replay(mockWorker, mockCoordinator);

        capturedConnections.get(0).workerVal = mockWorker;
        holder.onConnected(capturedConnections.get(0), mockWorker);

        verify(mockWorker, mockCoordinator);
        assertTrue(capturedConnections.get(0).unbindCalled);
        assertEquals(1, callback.releasedCount);
    }

    @Test
    public void testOnConnectedDispatchesBatch() throws Exception {
        setupBatchLoaderWithFiles("batch_1.txt");
        ISurfaceWorker mockWorker = createMock(ISurfaceWorker.class);
        TestSurface testSurface = new TestSurface();
        List<TestWorkerServiceConnection> capturedConnections = registerMockConnectionFactory();
        TestSchedulerCallback callback = new TestSchedulerCallback();

        WorkerHolder holder = new WorkerHolder(context, 0, batchLoader, TEST_LOG_DIR, TEST_CMD_LINE, directExecutor, stateLock, callback, mockCoordinator);

        // Set valid surface
        SurfaceHolder mockHolder = createMockSurfaceHolder(testSurface);
        holder.surfaceCreated(mockHolder);

        expect(mockWorker.startTestBatch(eq(testSurface), contains("batch_1.txt"))).andReturn(true).once();
        mockCoordinator.parse(anyObject());
        expectLastCall().once();
        mockCoordinator.onTestProcessFinished(anyObject());
        expectLastCall().once();

        replay(mockWorker, mockCoordinator);

        // Trigger connection
        capturedConnections.get(0).workerVal = mockWorker;
        holder.onConnected(capturedConnections.get(0), mockWorker);

        verify(mockWorker, mockCoordinator);
        assertTrue(callback.checkAllWorkersFinishedCalled);
        assertTrue(capturedConnections.get(0).unbindCalled);
    }

    @Test
    public void testOnConnectedDispatchesBatchWithCmdLineAndLogDir() throws Exception {
        setupBatchLoaderWithFiles("batch_1.txt");
        ISurfaceWorker mockWorker = createMock(ISurfaceWorker.class);
        TestSurface testSurface = new TestSurface();
        List<TestWorkerServiceConnection> capturedConnections = registerMockConnectionFactory();
        TestSchedulerCallback callback = new TestSchedulerCallback();

        String baseCmdLine = "--deqp-watchdog=enable --deqp-gl-config-name=rgba8888d24s8";
        String logDir = "/sdcard/deqplogs";
        WorkerHolder holder = new WorkerHolder(context, 0, batchLoader, logDir, baseCmdLine, directExecutor, stateLock, callback, mockCoordinator);

        SurfaceHolder mockHolder = createMockSurfaceHolder(testSurface);
        holder.surfaceCreated(mockHolder);

        expect(mockWorker.startTestBatch(eq(testSurface), matches(".*--deqp-log-filename=/sdcard/deqplogs/TestLog_parallel_0_\\d+\\.qpa --deqp-caselist-file=.*"))).andReturn(true).once();
        mockCoordinator.parse(anyObject());
        expectLastCall().once();
        mockCoordinator.onTestProcessFinished(anyObject());
        expectLastCall().once();

        replay(mockWorker, mockCoordinator);

        capturedConnections.get(0).workerVal = mockWorker;
        holder.onConnected(capturedConnections.get(0), mockWorker);

        verify(mockWorker, mockCoordinator);
        assertTrue(callback.checkAllWorkersFinishedCalled);
        assertTrue(capturedConnections.get(0).unbindCalled);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testOnConnectedDispatchesBatchWithNullCmdLineAndLogDir() throws Exception {
        setupBatchLoaderWithFiles("batch_1.txt");
        ISurfaceWorker mockWorker = createMock(ISurfaceWorker.class);
        TestSurface testSurface = new TestSurface();
        List<TestWorkerServiceConnection> capturedConnections = registerMockConnectionFactory();
        TestSchedulerCallback callback = new TestSchedulerCallback();

        WorkerHolder holder = new WorkerHolder(context, 0, batchLoader, null, null, directExecutor, stateLock, callback, mockCoordinator);

        SurfaceHolder mockHolder = createMockSurfaceHolder(testSurface);
        holder.surfaceCreated(mockHolder);

        capturedConnections.get(0).workerVal = mockWorker;
        holder.onConnected(capturedConnections.get(0), mockWorker);
    }

    @Test
    public void testQueueExhaustionCallsCheckAllWorkersFinished() throws Exception {
        List<TestWorkerServiceConnection> capturedConnections = registerMockConnectionFactory();
        TestSchedulerCallback callback = new TestSchedulerCallback();
        WorkerHolder holder = new WorkerHolder(context, 0, batchLoader, TEST_LOG_DIR, TEST_CMD_LINE, directExecutor, stateLock, callback, mockCoordinator);

        SurfaceHolder mockHolder = createMockSurfaceHolder(new TestSurface());
        holder.surfaceCreated(mockHolder);

        replay(mockCoordinator);

        capturedConnections.get(0).workerVal = createMock(ISurfaceWorker.class);
        holder.onConnected(capturedConnections.get(0), capturedConnections.get(0).workerVal);

        verify(mockCoordinator);
        assertTrue(callback.checkAllWorkersFinishedCalled);
        assertTrue(capturedConnections.get(0).unbindCalled);
        assertEquals(1, callback.releasedCount);
    }

    @Test
    public void testExecutionFailureUnbindsAndSafeBinds() throws Exception {
        setupBatchLoaderWithFiles("batch_1.txt", "batch_2.txt");
        ISurfaceWorker mockWorker = createMock(ISurfaceWorker.class);
        TestSurface testSurface = new TestSurface();
        List<TestWorkerServiceConnection> capturedConnections = registerMockConnectionFactory();
        TestSchedulerCallback callback = new TestSchedulerCallback();

        WorkerHolder holder = new WorkerHolder(context, 0, batchLoader, TEST_LOG_DIR, TEST_CMD_LINE, directExecutor, stateLock, callback, mockCoordinator);
        SurfaceHolder mockHolder = createMockSurfaceHolder(testSurface);
        holder.surfaceCreated(mockHolder);

        expect(mockWorker.startTestBatch(eq(testSurface), contains("batch_1.txt"))).andReturn(false).once();
        mockCoordinator.parse(anyObject());
        expectLastCall().once();
        mockCoordinator.onTestProcessFinished(anyObject());
        expectLastCall().once();

        replay(mockWorker, mockCoordinator);

        capturedConnections.get(0).workerVal = mockWorker;
        holder.onConnected(capturedConnections.get(0), mockWorker);

        verify(mockWorker, mockCoordinator);
        // Connection 1 (service ID 0) should be unbound
        assertTrue(capturedConnections.get(0).unbindCalled);
        // Connection 2 (service ID 1) should be created and bound
        assertEquals(2, capturedConnections.size());
        assertTrue(capturedConnections.get(1).bindCalled);

        String expectedBatch2 = new File(tempFolder.getRoot(), "batch_2.txt").getAbsolutePath();
        assertEquals(expectedBatch2, batchLoader.getBatchQueue().poll());
        assertNull(batchLoader.getBatchQueue().poll());
        assertEquals(1, callback.releasedCount);
    }

    @Test
    public void testWorkerDisconnectedTriggersUnbindAndSafeBinds() throws Exception {
        setupBatchLoaderWithFiles("batch_1.txt", "batch_2.txt");
        ISurfaceWorker mockWorker = createMock(ISurfaceWorker.class);
        TestSurface testSurface = new TestSurface();
        List<TestWorkerServiceConnection> capturedConnections = registerMockConnectionFactory();
        TestSchedulerCallback callback = new TestSchedulerCallback();

        WorkerHolder holder = new WorkerHolder(context, 0, batchLoader, TEST_LOG_DIR, TEST_CMD_LINE, directExecutor, stateLock, callback, mockCoordinator);
        SurfaceHolder mockHolder = createMockSurfaceHolder(testSurface);
        holder.surfaceCreated(mockHolder);

        expect(mockWorker.startTestBatch(eq(testSurface), contains("batch_1.txt"))).andAnswer(() -> {
            holder.onDisconnected(capturedConnections.get(0));
            return false;
        }).once();
        mockCoordinator.parse(anyObject());
        expectLastCall().once();
        mockCoordinator.onTestProcessFinished(anyObject());
        expectLastCall().once();

        replay(mockWorker, mockCoordinator);

        capturedConnections.get(0).workerVal = mockWorker;
        holder.onConnected(capturedConnections.get(0), mockWorker);

        verify(mockWorker, mockCoordinator);
        // Connection 1 (service ID 0) should be unbound
        assertTrue(capturedConnections.get(0).unbindCalled);
        // Connection 2 (service ID 1) should be created and bound
        assertEquals(2, capturedConnections.size());
        assertTrue(capturedConnections.get(1).bindCalled);

        String expectedBatch2 = new File(tempFolder.getRoot(), "batch_2.txt").getAbsolutePath();
        assertEquals(expectedBatch2, batchLoader.getBatchQueue().poll());
        assertNull(batchLoader.getBatchQueue().poll());
        assertEquals(1, callback.releasedCount);
    }

    @Test
    public void testSurfaceDestroyedReleasesServiceId() {
        TestSchedulerCallback callback = new TestSchedulerCallback();
        WorkerHolder holder = new WorkerHolder(context, 0, batchLoader, TEST_LOG_DIR, TEST_CMD_LINE, directExecutor, stateLock, callback, mockCoordinator);

        SurfaceHolder mockHolder = createMockSurfaceHolder(new TestSurface());
        holder.surfaceCreated(mockHolder);
        assertEquals(0, callback.releasedCount);

        holder.surfaceDestroyed(mockHolder);
        assertEquals(1, callback.releasedCount);
    }

    @Test
    public void testStaleDisconnectionFromPreviousConnectionIgnored() throws Exception {
        setupBatchLoaderWithFiles("batch_1.txt", "batch_2.txt");
        ISurfaceWorker mockWorker1 = createMock(ISurfaceWorker.class);
        ISurfaceWorker mockWorker2 = createMock(ISurfaceWorker.class);
        TestSurface testSurface = new TestSurface();
        List<TestWorkerServiceConnection> capturedConnections = registerMockConnectionFactory();
        TestSchedulerCallback callback = new TestSchedulerCallback();

        WorkerHolder holder = new WorkerHolder(context, 0, batchLoader, TEST_LOG_DIR, TEST_CMD_LINE, directExecutor, stateLock, callback, mockCoordinator);
        SurfaceHolder mockHolder = createMockSurfaceHolder(testSurface);
        holder.surfaceCreated(mockHolder);

        // Batch 1 finishes successfully on connection 0
        expect(mockWorker1.startTestBatch(eq(testSurface), contains("batch_1.txt"))).andReturn(true).once();
        expect(mockWorker2.startTestBatch(eq(testSurface), contains("batch_2.txt"))).andReturn(true).once();
        mockCoordinator.parse(anyObject());
        expectLastCall().times(2);
        mockCoordinator.onTestProcessFinished(anyObject());
        expectLastCall().times(2);

        replay(mockWorker1, mockWorker2, mockCoordinator);

        capturedConnections.get(0).workerVal = mockWorker1;
        holder.onConnected(capturedConnections.get(0), mockWorker1);

        // Connection 1 was unbound, connection 2 was created
        assertEquals(2, capturedConnections.size());
        assertTrue(capturedConnections.get(0).unbindCalled);

        // Now simulate delayed onDisconnected from connection 0 arriving
        holder.onDisconnected(capturedConnections.get(0));

        // Connection 2 should NOT be unbound by the stale disconnect
        assertFalse(capturedConnections.get(1).unbindCalled);

        // Connection 2 connects and runs batch 2
        capturedConnections.get(1).workerVal = mockWorker2;
        holder.onConnected(capturedConnections.get(1), mockWorker2);

        verify(mockWorker1, mockWorker2, mockCoordinator);
        assertTrue(capturedConnections.get(1).unbindCalled);
        assertTrue(callback.checkAllWorkersFinishedCalled);
        assertEquals(2, callback.releasedCount);
    }

    @Test
    public void testOnConnectedWithInvalidSurfaceTriggersResetAndChecksFinished() throws Exception {
        setupBatchLoaderWithFiles("batch_1.txt");
        ISurfaceWorker mockWorker = createMock(ISurfaceWorker.class);
        TestSurface testSurface = new TestSurface();
        List<TestWorkerServiceConnection> capturedConnections = registerMockConnectionFactory();
        TestSchedulerCallback callback = new TestSchedulerCallback();

        WorkerHolder holder = new WorkerHolder(context, 0, batchLoader, TEST_LOG_DIR, TEST_CMD_LINE, directExecutor, stateLock, callback, mockCoordinator);

        SurfaceHolder mockHolder = createMockSurfaceHolder(testSurface);
        holder.surfaceCreated(mockHolder);

        // Invalidate surface before onConnected arrives
        testSurface.valid = false;

        capturedConnections.get(0).workerVal = mockWorker;
        holder.onConnected(capturedConnections.get(0), mockWorker);

        // Connection should be unbound, service ID released, and checkAllWorkersFinished called
        assertTrue(capturedConnections.get(0).unbindCalled);
        assertEquals(1, callback.releasedCount);
        assertTrue(callback.checkAllWorkersFinishedCalled);
    }

    @Test
    public void testStaleConnectionFromPreviousConnectionIgnored() throws Exception {
        setupBatchLoaderWithFiles("batch_1.txt");
        ISurfaceWorker mockWorker = createMock(ISurfaceWorker.class);
        TestSurface testSurface = new TestSurface();
        List<TestWorkerServiceConnection> capturedConnections = registerMockConnectionFactory();
        TestSchedulerCallback callback = new TestSchedulerCallback();

        WorkerHolder holder = new WorkerHolder(context, 0, batchLoader, TEST_LOG_DIR, TEST_CMD_LINE, directExecutor, stateLock, callback, mockCoordinator);

        SurfaceHolder mockHolder = createMockSurfaceHolder(testSurface);
        holder.surfaceCreated(mockHolder);

        // Surface destroyed resets the holder and unbinds connection 0
        holder.surfaceDestroyed(mockHolder);
        assertTrue(capturedConnections.get(0).unbindCalled);

        // Now a delayed onConnected from connection 0 arrives
        holder.onConnected(capturedConnections.get(0), mockWorker);

        // Should be ignored: worker is not busy and batch remains in queue
        assertFalse(holder.isBusy());
        assertEquals(1, batchLoader.getBatchQueue().size());
    }

    @Test
    public void testOnConnectedWithNullWorkerTriggersResetAndChecksFinished() throws Exception {
        setupBatchLoaderWithFiles("batch_1.txt");
        TestSurface testSurface = new TestSurface();
        List<TestWorkerServiceConnection> capturedConnections = registerMockConnectionFactory();
        TestSchedulerCallback callback = new TestSchedulerCallback();

        WorkerHolder holder = new WorkerHolder(context, 0, batchLoader, TEST_LOG_DIR, TEST_CMD_LINE, directExecutor, stateLock, callback, mockCoordinator);

        SurfaceHolder mockHolder = createMockSurfaceHolder(testSurface);
        holder.surfaceCreated(mockHolder);

        // onConnected with null worker (e.g. stub resolution failure)
        capturedConnections.get(0).workerVal = null;
        holder.onConnected(capturedConnections.get(0), null);

        // Connection should be unbound, service ID released, and checkAllWorkersFinished called
        assertTrue(capturedConnections.get(0).unbindCalled);
        assertEquals(1, callback.releasedCount);
        assertTrue(callback.checkAllWorkersFinishedCalled);
    }
}
