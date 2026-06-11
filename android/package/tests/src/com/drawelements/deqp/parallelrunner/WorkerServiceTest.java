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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import android.content.Intent;
import android.os.IBinder;
import android.os.RemoteException;
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

    @Test
    public void testOnBindReturnsValidStub() {
        Intent intent = new Intent();
        IBinder binder = workerService.onBind(intent);

        assertNotNull("onBind should return a non-null IBinder", binder);
        assertTrue("Returned binder should implement ISurfaceWorker", binder instanceof ISurfaceWorker.Stub);
    }

    @Test
    public void testSubclassesInstantiateCorrectly() {
        // Assert that targeted worker implementations instantiate successfully for manifest resolution
        assertNotNull(new WorkerService.Worker1());
        assertNotNull(new WorkerService.Worker12());
    }

    @Test
    public void testStartTestBatchAsynchronousInvocation() throws android.os.RemoteException {
        Intent intent = new Intent();
        IBinder binder = workerService.onBind(intent);
        assertNotNull(binder);
        ISurfaceWorker worker = ISurfaceWorker.Stub.asInterface(binder);
        assertNotNull(worker);

        // Verify calling startTestBatch does not crash, even with null surface/args
        boolean accepted = worker.startTestBatch(null, "--deqp-case=dEQP-GLES2.info");
        assertTrue("Worker should accept the first test batch", accepted);

        boolean rejected = worker.startTestBatch(null, "--deqp-case=dEQP-GLES2.info");
        assertFalse("Worker should reject the second test batch", rejected);
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
}
