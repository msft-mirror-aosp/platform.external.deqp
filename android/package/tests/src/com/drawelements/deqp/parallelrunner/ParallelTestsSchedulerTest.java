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
import com.drawelements.deqp.testercore.DeqpInstrumentation;
import java.io.File;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import static org.easymock.EasyMock.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Unit tests for {@link ParallelTestsScheduler} verifying state machine tracking,
 * service bindings, and test batch scheduling.
 */
@RunWith(AndroidJUnit4.class)
public class ParallelTestsSchedulerTest {

    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();

    private Context context;
    private DeqpTestBatchLoader batchLoader;
    private ParallelTestsScheduler.Callback mockSchedulerCallback;

    private static class TestSurface extends Surface {
        @Override
        public boolean isValid() {
            return true;
        }
    }

    private static class TestWorkerServiceConnection extends WorkerServiceConnection {
        final Callback callback;
        boolean bindCalled = false;
        boolean unbindCalled = false;
        boolean isBoundVal = false;
        ISurfaceWorker workerVal = null;

        TestWorkerServiceConnection(Context context, int workerId, Callback callback) {
            super(context, workerId, callback);
            this.callback = callback;
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

    private static class TestWorkerHolder extends WorkerHolder {
        boolean onShutdownCalled = false;
        final String capturedLogDir;
        final String capturedCmdLine;

        TestWorkerHolder(Context context, int id, DeqpTestBatchLoader testBatchLoader,
                         String logDir, String cmdLine, ExecutorService dispatchExecutor, Object stateLock, SchedulerCallback schedulerCallback, LogParsersCoordinator coordinator) {
            super(context, id, testBatchLoader, logDir, cmdLine, dispatchExecutor, stateLock, schedulerCallback, coordinator);
            this.capturedLogDir = logDir;
            this.capturedCmdLine = cmdLine;
        }

        @Override
        void onShutdown() {
            onShutdownCalled = true;
        }
    }

    @Before
    public void setUp() {
        context = ApplicationProvider.getApplicationContext();
        batchLoader = new DeqpTestBatchLoader();
        mockSchedulerCallback = createMock(ParallelTestsScheduler.Callback.class);
        AsyncLogParsersCoordinator.initialize(1, false, DeqpInstrumentation.REPORTING_MODE_JAVA_LOG_PARSER, new LogParserFactoryImpl());
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
        WorkerHolder.setConnectionFactory((context, workerId, callback) -> {
            capturedConnection[0] = new TestWorkerServiceConnection(context, workerId, callback);
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


    private static final String TEST_LOG_DIR = "/sdcard/deqpparallel/logs/";
    private static final String TEST_CMD_LINE = "--deqp-gl-config-name=rgba8888d24s8";

    @After
    public void tearDown() {
        WorkerHolder.setConnectionFactory(WorkerServiceConnection::new);
        ParallelTestsScheduler.setWorkerHolderFactory(WorkerHolder::new);
        AsyncLogParsersCoordinator.reset();
    }

    @Test
    public void testSurfaceCreatedTriggersBindAndDispatch() throws Exception {
        setupBatchLoaderWithFiles("batch_1.txt");

        ISurfaceWorker mockWorker = createMock(ISurfaceWorker.class);
        TestSurface testSurface = new TestSurface();
        TestWorkerServiceConnection[] capturedConnection = registerMockConnectionFactory();

        CountDownLatch completionLatch = new CountDownLatch(1);
        mockSchedulerCallback.onAllTestsCompleted();
        expectLastCall().andAnswer(() -> {
            completionLatch.countDown();
            return null;
        }).once();

        expect(mockWorker.startTestBatch(eq(testSurface), contains("batch_1.txt"))).andReturn(true).once();

        replay(mockWorker, mockSchedulerCallback);

        ParallelTestsScheduler scheduler = new ParallelTestsScheduler(context, 1, batchLoader, TEST_LOG_DIR, TEST_CMD_LINE, mockSchedulerCallback);
        SurfaceHolder mockHolder = createMockSurfaceHolder(testSurface);
        
        // 1. Surface created -> Triggers mockConn.bind()
        scheduler.getWorkerCallback(0).surfaceCreated(mockHolder);
        assertNotNull(capturedConnection[0]);
        assertTrue(capturedConnection[0].bindCalled);

        // 2. Connect worker -> Dispatches batch_1
        capturedConnection[0].workerVal = mockWorker;
        capturedConnection[0].callback.onConnected(mockWorker);

        // Wait for completion
        assertTrue(completionLatch.await(2, TimeUnit.SECONDS));

        verify(mockWorker, mockSchedulerCallback);
        scheduler.shutdown();
    }



    @Test
    public void testQueueExhaustionTriggersShutdownCallback() throws Exception {
        setupBatchLoaderWithFiles("batch_1.txt");

        ISurfaceWorker mockWorker = createMock(ISurfaceWorker.class);
        TestSurface testSurface = new TestSurface();
        TestWorkerServiceConnection[] capturedConnection = registerMockConnectionFactory();

        CountDownLatch completionLatch = new CountDownLatch(1);
        mockSchedulerCallback.onAllTestsCompleted();
        expectLastCall().andAnswer(() -> {
            completionLatch.countDown();
            return null;
        }).once();

        expect(mockWorker.startTestBatch(eq(testSurface), contains("batch_1.txt"))).andReturn(true).once();

        replay(mockWorker, mockSchedulerCallback);

        ParallelTestsScheduler scheduler = new ParallelTestsScheduler(context, 1, batchLoader, TEST_LOG_DIR, TEST_CMD_LINE, mockSchedulerCallback);
        SurfaceHolder mockHolder = createMockSurfaceHolder(testSurface);

        scheduler.getWorkerCallback(0).surfaceCreated(mockHolder);
        capturedConnection[0].workerVal = mockWorker;
        capturedConnection[0].isBoundVal = true;
        capturedConnection[0].callback.onConnected(mockWorker);

        // Wait for the completion callback to fire automatically
        assertTrue(completionLatch.await(2, TimeUnit.SECONDS));

        // It should have unbound automatically on completion
        assertTrue(capturedConnection[0].unbindCalled);

        verify(mockWorker, mockSchedulerCallback);
        scheduler.shutdown();
    }

    @Test
    public void testRegisterSurfaceAppendsCallback() {
        ParallelTestsScheduler scheduler = new ParallelTestsScheduler(context, 1, batchLoader, TEST_LOG_DIR, TEST_CMD_LINE, mockSchedulerCallback);
        SurfaceHolder mockHolder = createMock(SurfaceHolder.class);

        mockHolder.addCallback(scheduler.getWorkerCallback(0));
        expectLastCall().once();

        replay(mockHolder);
        scheduler.registerSurface(0, mockHolder);
        verify(mockHolder);
        scheduler.shutdown();
    }

    @Test
    public void testShutdownCallsOnShutdownOnWorkers() {
        final TestWorkerHolder[] capturedHolder = new TestWorkerHolder[1];
        ParallelTestsScheduler.setWorkerHolderFactory(
                (context, workerId, loader, logDir, cmdLine, executor, lock, callback, coordinator) -> {
                    capturedHolder[0] = new TestWorkerHolder(
                            context, workerId, loader, logDir, cmdLine, executor, lock, callback, coordinator);
                    return capturedHolder[0];
                });

        ParallelTestsScheduler scheduler = new ParallelTestsScheduler(context, 1, batchLoader, TEST_LOG_DIR, TEST_CMD_LINE, mockSchedulerCallback);
        scheduler.shutdown();

        assertNotNull(capturedHolder[0]);
        assertTrue(capturedHolder[0].onShutdownCalled);
    }

    @Test
    public void testCmdLineAndLogDirPropagationToWorkers() {
        final TestWorkerHolder[] capturedHolder = new TestWorkerHolder[1];
        ParallelTestsScheduler.setWorkerHolderFactory(
                (context, workerId, loader, logDir, cmdLine, executor, lock, callback, coordinator) -> {
                    capturedHolder[0] = new TestWorkerHolder(
                            context, workerId, loader, logDir, cmdLine, executor, lock, callback, coordinator);
                    return capturedHolder[0];
                });

        String testCmdLine = "--deqp-watchdog=enable --deqp-gl-config-name=rgba8888d24s8";
        String testLogDir = "/tmp/log";
        ParallelTestsScheduler scheduler = new ParallelTestsScheduler(context, 1, batchLoader, testLogDir, testCmdLine, mockSchedulerCallback);

        assertNotNull(capturedHolder[0]);
        assertEquals(testLogDir, capturedHolder[0].capturedLogDir);
        assertEquals(testCmdLine, capturedHolder[0].capturedCmdLine);
        scheduler.shutdown();
    }

    @Test
    public void testAcquireAndReleaseServiceIdPool() {
        ParallelTestsScheduler scheduler = new ParallelTestsScheduler(context, 1, batchLoader, TEST_LOG_DIR, TEST_CMD_LINE, mockSchedulerCallback);
        Integer id1 = scheduler.acquireServiceId();
        Integer id2 = scheduler.acquireServiceId();

        assertEquals(0, (int) id1);
        assertEquals(1, (int) id2);

        scheduler.releaseServiceId(id1);

        for (int i = 2; i < ParallelRunnerConfig.MAX_ALLOWED_WORKERS; i++) {
            assertEquals(i, (int) scheduler.acquireServiceId());
        }

        assertEquals(0, (int) scheduler.acquireServiceId());
        scheduler.shutdown();
    }
}
