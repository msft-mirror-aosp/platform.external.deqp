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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import android.os.Bundle;
import com.drawelements.deqp.testercore.TestEventConstants;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

@RunWith(RobolectricTestRunner.class)
public class TestSessionEventsAccumulatorTest {

    private BlockingQueue<TestEvent> testEventQueue;
    private TestSessionEventsAccumulator accumulator;

    @Before
    public void setUp() {
        testEventQueue = new LinkedBlockingQueue<>();
    }

    @Test
    public void testLogData_ignoresWhenNoActiveTestCase() throws InterruptedException {
        accumulator = new TestSessionEventsAccumulator(testEventQueue, true);
        // testLogData called without beginTestCase
        accumulator.testLogData("test log");
        assertTrue(testEventQueue.isEmpty());
    }

    @Test
    public void testTestCaseResult_ignoresWhenNoActiveTestCase() {
        accumulator = new TestSessionEventsAccumulator(testEventQueue, true);
        // testCaseResult called without beginTestCase
        accumulator.testCaseResult("Pass", "Details passed");
        accumulator.endTestCase();
        assertTrue(testEventQueue.isEmpty());
    }

    @Test
    public void testConstructor_nullQueueThrows() {
        try {
            new TestSessionEventsAccumulator(null, true);
            assertTrue("Should throw IllegalArgumentException", false);
        } catch (IllegalArgumentException e) {
            assertEquals("testEventQueue cannot be null", e.getMessage());
        }
    }

    @Test
    public void testSessionEvents() {
        accumulator = new TestSessionEventsAccumulator(testEventQueue, true);

        accumulator.beginSession();
        accumulator.sessionInfo("key1", "value1");
        accumulator.endSession();

        assertEquals(3, testEventQueue.size());

        TestEvent r1 = testEventQueue.poll();
        assertNotNull(r1);
        List<Bundle> b1 = r1.getBundles();
        assertEquals(1, b1.size());
        assertEquals(TestEventConstants.BEGIN_SESSION,
            b1.get(0).getString(TestEventConstants.KEY_EVENT_TYPE));

        TestEvent r2 = testEventQueue.poll();
        assertNotNull(r2);
        List<Bundle> b2 = r2.getBundles();
        assertEquals(1, b2.size());
        assertEquals(TestEventConstants.SESSION_INFO,
            b2.get(0).getString(TestEventConstants.KEY_EVENT_TYPE));
        assertEquals("key1", b2.get(0).getString(TestEventConstants.KEY_SESSION_INFO_NAME));
        assertEquals("value1", b2.get(0).getString(TestEventConstants.KEY_SESSION_INFO_VALUE));

        TestEvent r3 = testEventQueue.poll();
        assertNotNull(r3);
        List<Bundle> b3 = r3.getBundles();
        assertEquals(1, b3.size());
        assertEquals(TestEventConstants.END_SESSION,
            b3.get(0).getString(TestEventConstants.KEY_EVENT_TYPE));
    }

    @Test
    public void testTestCaseLifecycle() throws InterruptedException {
        accumulator = new TestSessionEventsAccumulator(testEventQueue, true);

        accumulator.beginTestCase("dEQP-GLES3.info.version");
        accumulator.testLogData("Log message 1\n");
        accumulator.testCaseResult("Pass", "Details passed");
        accumulator.endTestCase();

        assertEquals(1, testEventQueue.size());
        TestEvent event = testEventQueue.poll();
        assertNotNull(event);

        List<Bundle> bundles = event.getBundles();
        assertEquals(4, bundles.size());

        assertEquals(TestEventConstants.BEGIN_TEST_CASE,
            bundles.get(0).getString(TestEventConstants.KEY_EVENT_TYPE));
        assertEquals("dEQP-GLES3.info.version",
            bundles.get(0).getString(TestEventConstants.KEY_BEGIN_TEST_CASE_PATH));

        assertEquals(TestEventConstants.TEST_LOG_DATA,
            bundles.get(1).getString(TestEventConstants.KEY_EVENT_TYPE));
        assertEquals("Log message 1\n",
            bundles.get(1).getString(TestEventConstants.KEY_TEST_LOG_DATA_LOG));

        assertEquals(TestEventConstants.TEST_CASE_RESULT,
            bundles.get(2).getString(TestEventConstants.KEY_EVENT_TYPE));
        assertEquals("Pass",
            bundles.get(2).getString(TestEventConstants.KEY_TEST_CASE_RESULT_CODE));
        assertEquals("Details passed",
            bundles.get(2).getString(TestEventConstants.KEY_TEST_CASE_RESULT_DETAILS));

        assertEquals(TestEventConstants.END_TEST_CASE,
            bundles.get(3).getString(TestEventConstants.KEY_EVENT_TYPE));
    }

    @Test
    public void testTestCaseLifecycle_logDataDisabled() throws InterruptedException {
        accumulator = new TestSessionEventsAccumulator(testEventQueue, false);

        accumulator.beginTestCase("dEQP-GLES3.info.version");
        accumulator.testLogData("Log message 1\n"); // Should be ignored
        accumulator.testCaseResult("Pass", "Details passed");
        accumulator.endTestCase();

        assertEquals(1, testEventQueue.size());
        TestEvent event = testEventQueue.poll();
        assertNotNull(event);

        List<Bundle> bundles = event.getBundles();
        assertEquals(3, bundles.size()); // No TestLogData bundle

        assertEquals(TestEventConstants.BEGIN_TEST_CASE,
            bundles.get(0).getString(TestEventConstants.KEY_EVENT_TYPE));
        assertEquals(TestEventConstants.TEST_CASE_RESULT,
            bundles.get(1).getString(TestEventConstants.KEY_EVENT_TYPE));
        assertEquals(TestEventConstants.END_TEST_CASE,
            bundles.get(2).getString(TestEventConstants.KEY_EVENT_TYPE));
    }

    @Test
    public void testTestCaseTermination() throws InterruptedException {
        accumulator = new TestSessionEventsAccumulator(testEventQueue, true);

        accumulator.beginTestCase("dEQP-GLES3.info.crash");
        accumulator.testLogData("Some logs before crash\n");
        accumulator.terminateTestCase("Native crash");

        assertEquals(1, testEventQueue.size());
        TestEvent event = testEventQueue.poll();
        assertNotNull(event);

        List<Bundle> bundles = event.getBundles();
        assertEquals(3, bundles.size());

        assertEquals(TestEventConstants.BEGIN_TEST_CASE,
            bundles.get(0).getString(TestEventConstants.KEY_EVENT_TYPE));
        assertEquals(TestEventConstants.TEST_LOG_DATA,
            bundles.get(1).getString(TestEventConstants.KEY_EVENT_TYPE));
        assertEquals(TestEventConstants.TERMINATE_TEST_CASE,
            bundles.get(2).getString(TestEventConstants.KEY_EVENT_TYPE));
        assertEquals("Native crash",
            bundles.get(2).getString(TestEventConstants.KEY_TERMINATE_TEST_CASE_REASON));
    }

    @Test
    public void testLargeLogDataChunking() throws InterruptedException {
        accumulator = new TestSessionEventsAccumulator(testEventQueue, true);

        accumulator.beginTestCase("dEQP-GLES3.info.large");

        // Build a large log message > 4KB
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 5000; i++) {
            sb.append('A');
        }
        String largeLog = sb.toString();

        accumulator.testLogData(largeLog);
        accumulator.endTestCase();

        assertEquals(1, testEventQueue.size());
        TestEvent event = testEventQueue.poll();
        assertNotNull(event);

        List<Bundle> bundles = event.getBundles();
        // Expected bundles: BeginTestCase, TestLogData (chunk 1), TestLogData (chunk 2), EndTestCase
        assertEquals(4, bundles.size());

        assertEquals(TestEventConstants.BEGIN_TEST_CASE,
            bundles.get(0).getString(TestEventConstants.KEY_EVENT_TYPE));

        assertEquals(TestEventConstants.TEST_LOG_DATA,
            bundles.get(1).getString(TestEventConstants.KEY_EVENT_TYPE));
        assertEquals(4096,
            bundles.get(1).getString(TestEventConstants.KEY_TEST_LOG_DATA_LOG).length());

        assertEquals(TestEventConstants.TEST_LOG_DATA,
            bundles.get(2).getString(TestEventConstants.KEY_EVENT_TYPE));
        assertEquals(5000 - 4096,
            bundles.get(2).getString(TestEventConstants.KEY_TEST_LOG_DATA_LOG).length());

        assertEquals(TestEventConstants.END_TEST_CASE,
            bundles.get(3).getString(TestEventConstants.KEY_EVENT_TYPE));
    }
}
