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

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

@RunWith(RobolectricTestRunner.class)
public class LogParsersCoordinatorTest {

    private LogParsersCoordinator coordinator;

    @Before
    public void setUp() {
        coordinator = new LogParsersCoordinator();
    }

    @After
    public void tearDown() {
        coordinator.deinit();
    }

    private File createDummyQpaFile() throws IOException {
        File dummyQpa = File.createTempFile("dummy", ".qpa");
        dummyQpa.deleteOnExit();
        try (PrintWriter out = new PrintWriter(dummyQpa)) {
            out.println("#beginSession");
            out.println("#beginTestCaseResult dummy.test");
            out.println("<Result StatusCode=\"Pass\">Details</Result>");
            out.println("#endTestCaseResult");
            out.println("#endSession");
        }
        return dummyQpa;
    }

    @Test
    public void testPublishToSubscribers() throws Exception {
        final List<TestEvent> receivedTestEvents = new ArrayList<>();
        final java.util.concurrent.CountDownLatch latch = new java.util.concurrent.CountDownLatch(
            3);
        TestEventSubscriber subscriber = new TestEventSubscriber() {
            @Override
            public void onTestEventReceived(TestEvent event) {
                receivedTestEvents.add(event);
                latch.countDown();
            }
        };

        coordinator.subscribe(subscriber);

        File dummyQpa = createDummyQpaFile();
        coordinator.parse(dummyQpa.getAbsolutePath(), true);

        boolean receivedAll = latch.await(3, java.util.concurrent.TimeUnit.SECONDS);
        assertTrue("Timeout waiting for 3 events", receivedAll);

        assertEquals(3, receivedTestEvents.size());
    }

    @Test
    public void testUnsubscribe() throws Exception {
        final List<TestEvent> receivedTestEvents = new ArrayList<>();
        TestEventSubscriber subscriber = new TestEventSubscriber() {
            @Override
            public void onTestEventReceived(TestEvent event) {
                receivedTestEvents.add(event);
            }
        };

        coordinator.subscribe(subscriber);
        coordinator.unsubscribe(subscriber);

        final java.util.concurrent.CountDownLatch latch = new java.util.concurrent.CountDownLatch(
            3);
        TestEventSubscriber helperSubscriber = new TestEventSubscriber() {
            @Override
            public void onTestEventReceived(TestEvent event) {
                latch.countDown();
            }
        };
        coordinator.subscribe(helperSubscriber);

        File dummyQpa = createDummyQpaFile();
        coordinator.parse(dummyQpa.getAbsolutePath(), true);

        boolean receivedAll = latch.await(3, java.util.concurrent.TimeUnit.SECONDS);
        assertTrue("Timeout waiting for 3 events in helper subscriber", receivedAll);

        // Ensure the dispatcher thread has completely finished its publish loop
        // by joining it via deinit(). This prevents a false positive where the main
        // thread asserts while the dispatcher thread is still iterating over subscribers.
        coordinator.deinit();

        assertTrue(receivedTestEvents.isEmpty());
    }

    @Test
    public void testPublishMultipleSubscribers() throws Exception {
        final List<TestEvent> events1 = new ArrayList<>();
        final List<TestEvent> events2 = new ArrayList<>();
        final java.util.concurrent.CountDownLatch latch = new java.util.concurrent.CountDownLatch(
            6); // 3 events * 2 subscribers

        TestEventSubscriber sub1 = new TestEventSubscriber() {
            @Override
            public void onTestEventReceived(TestEvent event) {
                events1.add(event);
                latch.countDown();
            }
        };
        TestEventSubscriber sub2 = new TestEventSubscriber() {
            @Override
            public void onTestEventReceived(TestEvent event) {
                events2.add(event);
                latch.countDown();
            }
        };

        coordinator.subscribe(sub1);
        coordinator.subscribe(sub2);

        File dummyQpa = createDummyQpaFile();
        coordinator.parse(dummyQpa.getAbsolutePath(), true);

        boolean receivedAll = latch.await(3, java.util.concurrent.TimeUnit.SECONDS);
        assertTrue("Timeout waiting for 6 events across subscribers", receivedAll);

        assertEquals(3, events1.size());
        assertEquals(3, events2.size());
    }

    @Test
    public void testTestEventsFromMultipleParsers() throws Exception {
        final List<TestEvent> receivedTestEvents = new ArrayList<>();
        final java.util.concurrent.CountDownLatch latch = new java.util.concurrent.CountDownLatch(
            6);
        TestEventSubscriber subscriber = new TestEventSubscriber() {
            @Override
            public void onTestEventReceived(TestEvent event) {
                synchronized (receivedTestEvents) {
                    receivedTestEvents.add(event);
                }
                latch.countDown();
            }
        };
        coordinator.subscribe(subscriber);

        File dummyQpa1 = createDummyQpaFile();
        File dummyQpa2 = createDummyQpaFile();

        coordinator.parse(dummyQpa1.getAbsolutePath(), true);
        coordinator.parse(dummyQpa2.getAbsolutePath(), true);

        // wait till we receive 6 values with timeout
        boolean receivedAll = latch.await(3, java.util.concurrent.TimeUnit.SECONDS);
        assertTrue("Timeout waiting for 6 events", receivedAll);

        assertEquals(6, receivedTestEvents.size());
    }

    @Test
    public void testParseAndDeinit() throws Exception {
        final List<TestEvent> receivedTestEvents = new ArrayList<>();
        final java.util.concurrent.CountDownLatch firstEventLatch = new java.util.concurrent.CountDownLatch(
            1);

        TestEventSubscriber subscriber = new TestEventSubscriber() {
            @Override
            public void onTestEventReceived(TestEvent event) {
                receivedTestEvents.add(event);
                firstEventLatch.countDown();
            }
        };
        coordinator.subscribe(subscriber);

        File dummyQpa = File.createTempFile("dummy", ".qpa");
        dummyQpa.deleteOnExit();

        // Start parsing while the file is still being written to
        coordinator.parse(dummyQpa.getAbsolutePath(), true);

        // We use PrintWriter with auto-flush so lines are written immediately
        try (java.io.PrintWriter out = new java.io.PrintWriter(
            new java.io.FileOutputStream(dummyQpa), true)) {
            // Write first line to trigger an event
            out.println("#beginSession");

            // Deterministically wait for parsing to start and the first event to arrive
            boolean gotEvent = firstEventLatch.await(3, java.util.concurrent.TimeUnit.SECONDS);
            assertTrue("Timeout waiting for first event", gotEvent);

            // Now deinit
            coordinator.deinit();

            int sizeAfterDeinit = receivedTestEvents.size();

            // Verify no extra events are fired after deinit returns
            // We subscribe the extraSubscriber BEFORE writing the remaining lines to guarantee we catch them if they fire.
            final java.util.concurrent.CountDownLatch extraEventLatch = new java.util.concurrent.CountDownLatch(
                1);
            TestEventSubscriber extraSubscriber = new TestEventSubscriber() {
                @Override
                public void onTestEventReceived(TestEvent event) {
                    extraEventLatch.countDown();
                }
            };
            coordinator.subscribe(extraSubscriber);

            // Write remaining lines that should NOT be parsed or emitted since we called deinit()
            out.println("#beginTestCaseResult dummy.test");
            out.println("<Result StatusCode=\"Pass\">Details</Result>");
            out.println("#endTestCaseResult");
            out.println("#endSession");

            boolean gotExtraEvent = extraEventLatch.await(100,
                java.util.concurrent.TimeUnit.MILLISECONDS);
            assertFalse("No events should be received after deinit", gotExtraEvent);
            assertEquals(sizeAfterDeinit, receivedTestEvents.size());
        }
    }


    @Test
    public void testOnTestProcessFinished_unknownFile() {
        // Should not throw exception
        coordinator.onTestProcessFinished("nonexistent.qpa");
    }

}
