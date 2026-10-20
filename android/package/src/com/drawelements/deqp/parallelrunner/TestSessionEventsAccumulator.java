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
import com.drawelements.deqp.testercore.TestEventConstants;
import com.drawelements.deqp.testercore.TestEventListener;
import java.util.concurrent.BlockingQueue;

/**
 * Implementation of TestEventListener that aggregates streaming parser events into cohesive,
 * ordered TestEvent objects and places them into a shared blocking queue.
 *
 * <p>A single {@code TestSessionEventsAccumulator} instance should be created and used per
 * parser/runner thread (i.e. one per test session/QPA instance) to prevent interleaved outputs from
 * parallel test runs.
 */
public class TestSessionEventsAccumulator implements TestEventListener {


    private final BlockingQueue<TestEvent> testEventQueue;
    private final boolean shouldLogData;
    private TestEvent currentEvent;

    public TestSessionEventsAccumulator(BlockingQueue<TestEvent> testEventQueue,
        boolean shouldLogData) {
        if (testEventQueue == null) {
            throw new IllegalArgumentException("testEventQueue cannot be null");
        }
        this.testEventQueue = testEventQueue;
        this.shouldLogData = shouldLogData;
        this.currentEvent = null;
    }

    @Override
    public synchronized void beginTestCase(String testCase) {
        currentEvent = new TestEvent();

        Bundle info = new Bundle();
        info.putString(TestEventConstants.KEY_EVENT_TYPE, TestEventConstants.BEGIN_TEST_CASE);
        info.putString(TestEventConstants.KEY_BEGIN_TEST_CASE_PATH, testCase);
        currentEvent.addBundle(info);
    }

    @Override
    public synchronized void testLogData(String log) throws InterruptedException {
        if (currentEvent == null || !shouldLogData || log == null) {
            return;
        }

        final int chunkSize = 4 * 1024;
        int length = log.length();
        for (int i = 0; i < length; i += chunkSize) {
            int end = Math.min(length, i + chunkSize);
            String message = log.substring(i, end);

            Bundle info = new Bundle();
            info.putString(TestEventConstants.KEY_EVENT_TYPE, TestEventConstants.TEST_LOG_DATA);
            info.putString(TestEventConstants.KEY_TEST_LOG_DATA_LOG, message);
            currentEvent.addBundle(info);
        }
    }

    @Override
    public synchronized void testCaseResult(String code, String details) {
        if (currentEvent == null) {
            return;
        }

        Bundle info = new Bundle();
        info.putString(TestEventConstants.KEY_EVENT_TYPE, TestEventConstants.TEST_CASE_RESULT);
        info.putString(TestEventConstants.KEY_TEST_CASE_RESULT_CODE, code);
        info.putString(TestEventConstants.KEY_TEST_CASE_RESULT_DETAILS, details);
        currentEvent.addBundle(info);
    }

    @Override
    public synchronized void endTestCase() {
        if (currentEvent == null) {
            return;
        }

        Bundle info = new Bundle();
        info.putString(TestEventConstants.KEY_EVENT_TYPE, TestEventConstants.END_TEST_CASE);
        currentEvent.addBundle(info);

        flushCurrentEvent();
    }

    @Override
    public synchronized void terminateTestCase(String reason) {
        if (currentEvent == null) {
            return;
        }

        Bundle info = new Bundle();
        info.putString(TestEventConstants.KEY_EVENT_TYPE, TestEventConstants.TERMINATE_TEST_CASE);
        info.putString(TestEventConstants.KEY_TERMINATE_TEST_CASE_REASON, reason);
        currentEvent.addBundle(info);

        flushCurrentEvent();
    }

    @Override
    public synchronized void beginSession() {
        TestEvent sessionEvent = new TestEvent();
        Bundle info = new Bundle();
        info.putString(TestEventConstants.KEY_EVENT_TYPE, TestEventConstants.BEGIN_SESSION);
        sessionEvent.addBundle(info);

        enqueue(sessionEvent);
    }

    @Override
    public synchronized void endSession() {
        TestEvent sessionEvent = new TestEvent();
        Bundle info = new Bundle();
        info.putString(TestEventConstants.KEY_EVENT_TYPE, TestEventConstants.END_SESSION);
        sessionEvent.addBundle(info);

        enqueue(sessionEvent);
    }

    @Override
    public synchronized void sessionInfo(String name, String value) {
        TestEvent sessionEvent = new TestEvent();
        Bundle info = new Bundle();
        info.putString(TestEventConstants.KEY_EVENT_TYPE, TestEventConstants.SESSION_INFO);
        info.putString(TestEventConstants.KEY_SESSION_INFO_NAME, name);
        info.putString(TestEventConstants.KEY_SESSION_INFO_VALUE, value);
        sessionEvent.addBundle(info);

        enqueue(sessionEvent);
    }

    private void flushCurrentEvent() {
        if (currentEvent != null) {
            enqueue(currentEvent);
            currentEvent = null;
        }
    }

    private void enqueue(TestEvent event) {
        try {
            testEventQueue.put(event);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
