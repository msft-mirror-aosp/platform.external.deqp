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

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.content.Intent;
import android.content.res.AssetManager;
import android.os.IBinder;
import android.os.Process;
import android.os.RemoteException;
import android.view.Surface;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import java.lang.reflect.InvocationTargetException;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Unit tests for {@link WorkerService} verifying the AIDL binder contract
 * and execution behavior under isolated environments.
 */
@RunWith(AndroidJUnit4.class)
public class WorkerServiceTest {

    private WorkerService workerService;

    @Before
    public void setUp() {
        workerService = new WorkerService();
    }

    private ISurfaceWorker getBoundWorker(WorkerService service) {
        IBinder binder = service.onBind(new Intent());
        assertNotNull("onBind should return a non-null IBinder", binder);
        return ISurfaceWorker.Stub.asInterface(binder);
    }

    @Test
    public void testGetServiceClass() {
        assertEquals(WorkerService.Worker1.class, WorkerService.getServiceClass(0));
        assertEquals(WorkerService.Worker2.class, WorkerService.getServiceClass(1));
        assertEquals(WorkerService.Worker12.class, WorkerService.getServiceClass(11));
    }

    @Test
    public void testOnBindReturnsValidStub() {
        IBinder binder = workerService.onBind(new Intent());

        assertNotNull("onBind should return a non-null IBinder", binder);
        assertTrue("Returned binder should implement ISurfaceWorker", binder instanceof ISurfaceWorker.Stub);
    }

    @Test
    public void testSubclassesInstantiateCorrectly() {
        assertNotNull(new WorkerService.Worker1());
        assertNotNull(new WorkerService.Worker12());
    }

    @Test
    public void testAllWorkerSubclassesBindCorrectly()
            throws NoSuchMethodException, InstantiationException, IllegalAccessException, InvocationTargetException {
        Intent intent = new Intent();
        for (int i = 0; i < 12; i++) {
            Class<?> workerClass = WorkerService.getServiceClass(i);
            WorkerService worker = (WorkerService) workerClass.getDeclaredConstructor().newInstance();
            IBinder binder = worker.onBind(intent);
            assertNotNull(workerClass.getSimpleName() + " should return a non-null IBinder", binder);
            assertTrue(workerClass.getSimpleName() + " binder should implement ISurfaceWorker",
                binder instanceof ISurfaceWorker.Stub);
        }
    }

    @Test
    public void testStartTestBatch_withNullSurface_rejectsExecution() throws RemoteException {
        ISurfaceWorker worker = getBoundWorker(workerService);

        // Verify calling startTestBatch rejects null surface safely without crashing
        boolean accepted = worker.startTestBatch(null, "--deqp-case=dEQP-GLES2.info");
        assertFalse("Worker should reject test batch when surface is null", accepted);

        boolean acceptedSecond = worker.startTestBatch(null, "--deqp-case=dEQP-GLES2.info");
        assertFalse("Worker should reject second test batch sequentially when surface is null", acceptedSecond);
    }

    /**
     * Intercepts execution before {@code nativeStartDeqp} is called, capturing thread priority
     * without requiring libdeqp.so or adding test hooks to WorkerService.
     */
    private static class RecordingWorkerService extends WorkerService {
        volatile int priorityDuringCall = Integer.MIN_VALUE;

        @Override
        public Context getBaseContext() {
            priorityDuringCall = Process.getThreadPriority(Process.myTid());
            throw new RuntimeException("Stop execution before native call");
        }
    }

    @Test
    public void testStartTestBatch_runsNativeEngineAtDisplayPriority() throws RemoteException {
        RecordingWorkerService service = new RecordingWorkerService();
        ISurfaceWorker worker = getBoundWorker(service);

        worker.startTestBatch(new Surface(), "--deqp-case=dEQP-GLES2.info");

        assertEquals(
                "dEQP execution must run at display priority to match the legacy runner",
                Process.THREAD_PRIORITY_DISPLAY,
                service.priorityDuringCall);
    }

    @Test
    public void testStartTestBatch_restoresThreadPriorityAfterBatch() throws RemoteException {
        RecordingWorkerService service = new RecordingWorkerService();
        ISurfaceWorker worker = getBoundWorker(service);

        Process.setThreadPriority(Process.THREAD_PRIORITY_DEFAULT);
        int before = Process.getThreadPriority(Process.myTid());
        worker.startTestBatch(new Surface(), "--deqp-case=dEQP-GLES2.info");
        int after = Process.getThreadPriority(Process.myTid());

        assertEquals("Thread priority must be restored after the batch completes", before, after);
        assertEquals(Process.THREAD_PRIORITY_DEFAULT, after);
    }
}
