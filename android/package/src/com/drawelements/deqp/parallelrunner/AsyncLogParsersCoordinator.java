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

import com.drawelements.deqp.testercore.Log;
import com.drawelements.deqp.testercore.LogParser;

import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Coordinates parallel parsing of multiple log files. Manages a pool of workers
 * and distributes
 * finished test TestEvent one by one.
 */
public class AsyncLogParsersCoordinator implements LogParsersCoordinator {

    private static final String LOG_TAG = "dEQP/AsyncLogParsersCoordinator";
    private static final int DISPATCHER_TIMEOUT_MS = 2000;
    private static final int MAX_NO_PROGRESS_CHECKS = 3;
    private static final TestEvent POISON_PILL = new TestEvent();

    private final int maxWorkers;
    private final boolean logData;
    private final String eventReportingMode;
    private final BlockingQueue<TestEvent> testEventQueue;
    private final CopyOnWriteArrayList<TestEventSubscriber> subscribers;
    private ExecutorService workersExecutor;
    private ExecutorService dispatcherExecutor;
    private volatile boolean isInitialized;
    private final Map<String, LogParserWorker> workers = new ConcurrentHashMap<>();
    private final Object lock = new Object();
    private final LogParserFactory logParserFactory;
    private final AtomicLong totalEventsPublished = new AtomicLong(0);
    private final LogParserWorker.TimingSettings workerTimingSettings;

    private static volatile AsyncLogParsersCoordinator instance;

    public static void initialize(int maxWorkers, boolean logData, String eventReportingMode,
            LogParserFactory logParserFactory) {
        initialize(maxWorkers, logData, eventReportingMode, logParserFactory,
                new LogParserWorker.TimingSettings());
    }

    public static void initialize(int maxWorkers, boolean logData, String eventReportingMode,
            LogParserFactory logParserFactory,
            LogParserWorker.TimingSettings workerTimingSettings) {
        if (instance == null) {
            synchronized (AsyncLogParsersCoordinator.class) {
                if (instance == null) {
                    instance = new AsyncLogParsersCoordinator(maxWorkers, logData, eventReportingMode,
                            logParserFactory, workerTimingSettings);
                }
            }
        }
    }

    public static LogParsersCoordinator getInstance() {
        if (instance == null) {
            throw new IllegalStateException("AsyncLogParsersCoordinator is not initialized");
        }
        return instance;
    }

    private AsyncLogParsersCoordinator(int maxWorkers, boolean logData, String eventReportingMode,
            LogParserFactory logParserFactory,
            LogParserWorker.TimingSettings workerTimingSettings) {
        this.maxWorkers = Math.max(1, maxWorkers);
        this.logData = logData;
        this.eventReportingMode = eventReportingMode;
        this.logParserFactory = logParserFactory;
        this.workerTimingSettings = workerTimingSettings;
        this.testEventQueue = new LinkedBlockingQueue<>();
        this.subscribers = new CopyOnWriteArrayList<>();
        this.isInitialized = false;
    }

    @Override
    public void onTestProcessFinished(String logFilePath) {
        Log.i(LOG_TAG, "onTestProcessFinished: " + logFilePath);
        LogParserWorker worker = workers.get(logFilePath);
        if (worker != null) {
            worker.onTestProcessFinished();
        }
    }

    @Override
    public void subscribe(TestEventSubscriber subscriber) {
        if (subscriber != null) {
            subscribers.addIfAbsent(subscriber);
        }
    }

    @Override
    public void unsubscribe(TestEventSubscriber subscriber) {
        if (subscriber != null) {
            subscribers.remove(subscriber);
        }
    }

    private void publish(TestEvent event) {
        if (event == null) {
            return;
        }
        for (TestEventSubscriber subscriber : subscribers) {
            subscriber.onTestEventReceived(event);
        }
        totalEventsPublished.incrementAndGet();
    }

    @Override
    public void parse(String logFilePath) {
        synchronized (lock) {
            if (!isInitialized) {
                init();
            }

            startWorker(logFilePath);
        }
    }

    private void init() {
        isInitialized = true;

        dispatcherExecutor = Executors.newSingleThreadExecutor(
                r -> new Thread(r, "TestEventDispatcherThread"));
        @SuppressWarnings("unused")
        Future<?> unused = dispatcherExecutor.submit(this::dispatchTestEvents);

        workersExecutor = Executors.newFixedThreadPool(maxWorkers);
    }

    private void startWorker(String logFilePath) {
        if (workers.containsKey(logFilePath)) {
            Log.i(LOG_TAG, "Worker already active for file: " + logFilePath);
            return;
        }

        LogParser parser = logParserFactory.create(eventReportingMode);
        LogParserWorker.Callback callback = new LogParserWorker.Callback() {
            @Override
            public void onParseSuccess(String file) {
                Log.i(LOG_TAG, "File parsing finished successfully: " + file);
                workers.remove(file);
            }

            @Override
            public void onParseFailed(String file, Throwable error) {
                Log.e(LOG_TAG, "File parsing failed: " + file, error);
                workers.remove(file);
            }
        };
        LogParserWorker worker = new LogParserWorker(parser, testEventQueue, logFilePath, logData,
                callback, workerTimingSettings);
        workers.put(logFilePath, worker);
        @SuppressWarnings("unused")
        Future<?> unused = workersExecutor.submit(worker);
    }

    @Override
    public void deinit() {
        synchronized (lock) {
            if (!isInitialized) {
                return;
            }

            awaitTerminationWithProgress(
                    workersExecutor,
                    workerTimingSettings.noDataTimeoutMs,
                    "log parser workers");
            workersExecutor = null;

            isInitialized = false;

            testEventQueue.offer(POISON_PILL);
            awaitTerminationWithProgress(
                    dispatcherExecutor,
                    DISPATCHER_TIMEOUT_MS,
                    "event dispatcher");
            dispatcherExecutor = null;

            subscribers.clear();
            workers.clear();
            testEventQueue.clear();
        }
    }

    private void awaitTerminationWithProgress(
            ExecutorService executor, long timeoutMs, String phaseDescription) {
        if (executor == null) {
            return;
        }

        executor.shutdown();
        int noProgressCount = 0;
        long lastPublishedEvents = totalEventsPublished.get();
        try {
            while (!executor.awaitTermination(timeoutMs, TimeUnit.MILLISECONDS)) {
                long currentPublishedEvents = totalEventsPublished.get();
                Log.d(LOG_TAG, "Waiting for " + phaseDescription + " to terminate...");

                if (currentPublishedEvents == lastPublishedEvents) {
                    noProgressCount++;
                    if (noProgressCount >= MAX_NO_PROGRESS_CHECKS) {
                        Log.w(LOG_TAG, "No progress observed for " + phaseDescription + " after "
                                + MAX_NO_PROGRESS_CHECKS
                                + " consecutive checks; forcing shutdown.");
                        executor.shutdownNow();
                        break;
                    }
                } else {
                    noProgressCount = 0;
                    lastPublishedEvents = currentPublishedEvents;
                }
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    private void dispatchTestEvents() {
        try {
            while (true) {
                TestEvent event = testEventQueue.take();
                if (event == POISON_PILL) {
                    Log.i(LOG_TAG, "Poison pill received. Shutting down dispatcher.");
                    break;
                }
                publish(event);
            }
        } catch (InterruptedException e) {
            Log.i(LOG_TAG, "Dispatcher thread interrupted.");
            Thread.currentThread().interrupt();
        }
    }

    // Visible for testing
    public static void reset() {
        synchronized (AsyncLogParsersCoordinator.class) {
            if (instance != null) {
                instance.deinit();
                instance = null;
            }
        }
    }
}
