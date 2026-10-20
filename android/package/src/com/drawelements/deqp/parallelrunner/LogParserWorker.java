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

import com.drawelements.deqp.testercore.LogParser;

import android.util.Log;
import java.io.File;
import java.io.IOException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Runnable task that manages the lifecycle of a single LogParser in a separate thread.
 */
public class LogParserWorker implements Runnable {

    private static final String LOG_TAG = "dEQP/LogParserWorker";

    public static class TimingSettings {

        // Visible for testing
        static final long DEFAULT_NO_ACTIVITY_SLEEP_MS = 100;
        static final long DEFAULT_NO_DATA_SLEEP_MS = 100;
        static final long DEFAULT_NO_DATA_TIMEOUT_MS = 5000;
        static final long DEFAULT_FILE_READY_TIMEOUT_MS = 10000;

        public final long noActivitySleepMs;
        public final long noDataSleepMs;
        public final long noDataTimeoutMs;
        public final long fileReadyTimeoutMs;

        public TimingSettings() {
            this(DEFAULT_NO_ACTIVITY_SLEEP_MS, DEFAULT_NO_DATA_SLEEP_MS,
                DEFAULT_NO_DATA_TIMEOUT_MS, DEFAULT_FILE_READY_TIMEOUT_MS);
        }

        public TimingSettings(long noActivitySleepMs, long noDataSleepMs,
            long noDataTimeoutMs, long fileReadyTimeoutMs) {
            this.noActivitySleepMs = noActivitySleepMs;
            this.noDataSleepMs = noDataSleepMs;
            this.noDataTimeoutMs = noDataTimeoutMs;
            this.fileReadyTimeoutMs = fileReadyTimeoutMs;
        }
    }

    private final TimingSettings config;
    private final LogParser parser;
    private final TestSessionEventsAccumulator accumulator;
    private final String logFile;
    private final boolean shouldLogData;
    private final AtomicBoolean testProcessAlive = new AtomicBoolean(true);

    /**
     * Callback interface to notify the completion status of the parsing job.
     */
    public interface Callback {

        void onParseSuccess(String logFile);

        void onParseFailed(String logFile, Throwable error);
    }

    private final Callback callback;

    public LogParserWorker(LogParser parser, BlockingQueue<TestEvent> testEventQueue,
        String logFile, boolean shouldLogData, Callback callback) {
        this(parser, testEventQueue, logFile, shouldLogData, callback, new TimingSettings());
    }

    public LogParserWorker(LogParser parser, BlockingQueue<TestEvent> testEventQueue,
        String logFile, boolean shouldLogData, Callback callback, TimingSettings config) {
        if (parser == null) {
            throw new IllegalArgumentException("parser cannot be null");
        }
        if (testEventQueue == null) {
            throw new IllegalArgumentException("testEventQueue cannot be null");
        }
        if (logFile == null) {
            throw new IllegalArgumentException("logFile cannot be null");
        }
        this.parser = parser;
        this.accumulator = new TestSessionEventsAccumulator(testEventQueue, shouldLogData);
        this.logFile = logFile;
        this.shouldLogData = shouldLogData;
        this.callback = callback;
        this.config = config;
    }

    public void onTestProcessFinished() {
        testProcessAlive.set(false);
    }

    @Override
    public void run() {
        Throwable error = null;
        try {
            waitForLogFile();

            Log.d(LOG_TAG, "Starting log parser for: " + logFile);
            parser.init(accumulator, logFile, shouldLogData);

            parseWhileTestProcessAlive();
            parseRemainingMessages();

            Log.d(LOG_TAG, "Completed parsing for: " + logFile);
        } catch (InterruptedException e) {
            Log.w(LOG_TAG, "Parser worker interrupted: " + logFile);
            Thread.currentThread().interrupt();
            error = e;
        } catch (Exception e) {
            Log.e(LOG_TAG, "Exception encountered in parser worker for: " + logFile, e);
            error = e;
        } finally {
            try {
                parser.deinit();
            } catch (IOException e) {
                Log.w(LOG_TAG, "Failed to de-initialize parser for: " + logFile, e);
                if (error == null) {
                    error = e;
                }
            }

            if (callback != null) {
                if (error == null) {
                    callback.onParseSuccess(logFile);
                } else {
                    callback.onParseFailed(logFile, error);
                }
            }
        }
    }

    private void waitForLogFile() throws InterruptedException, IOException {
        final long startTimeMs = System.currentTimeMillis();
        final File file = new File(logFile);
        while (true) {
            if (Thread.currentThread().isInterrupted()) {
                throw new InterruptedException();
            }
            if (file.exists()) {
                break;
            }
            final long timeSinceStartMs = System.currentTimeMillis() - startTimeMs;
            if (timeSinceStartMs > config.fileReadyTimeoutMs) {
                throw new IOException("Timeout while waiting for log file: " + logFile);
            }
            Thread.sleep(config.noActivitySleepMs);
        }
    }

    private void parseWhileTestProcessAlive() throws InterruptedException, IOException {
        while (testProcessAlive.get()) {
            if (Thread.currentThread().isInterrupted()) {
                throw new InterruptedException();
            }
            if (!parser.parse()) {
                Thread.sleep(config.noActivitySleepMs);
            }
        }
    }

    private void parseRemainingMessages() throws InterruptedException, IOException {
        long lastDataMs = System.currentTimeMillis();
        while (true) {
            if (parser.parse()) {
                lastDataMs = System.currentTimeMillis();
            } else {
                final long timeSinceLastDataMs = System.currentTimeMillis() - lastDataMs;
                if (timeSinceLastDataMs > config.noDataTimeoutMs) {
                    break; // No new data within threshold, assume EOF
                }
                Thread.sleep(config.noDataSleepMs);
            }
        }
    }
}
