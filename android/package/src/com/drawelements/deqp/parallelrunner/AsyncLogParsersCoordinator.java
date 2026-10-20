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
import com.drawelements.deqp.testercore.DeqpInstrumentation;

import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * Coordinates parallel parsing of multiple log files. Manages a pool of workers and distributes
 * finished test TestEvent one by one.
 */
public class AsyncLogParsersCoordinator implements LogParsersCoordinator {

    private static final String LOG_TAG = "dEQP/AsyncLogParsersCoordinator";
    private static final int EXECUTOR_TERMINATION_TIMEOUT_SECONDS = 2;
    private static final int DISPATCHER_THREAD_JOIN_TIMEOUT_MS = 2000;

    private final int maxWorkers;
    private final boolean logData;
    private final String eventReportingMode;
    private final BlockingQueue<TestEvent> testEventQueue;
    private final CopyOnWriteArrayList<TestEventSubscriber> subscribers;
    private ExecutorService workersExecutor;
    private Thread dispatcherThread;
    private volatile boolean isInitialized;
    private final Map<String, LogParserWorker> workers = new ConcurrentHashMap<>();
    private final Object lock = new Object();
    private final LogParserFactory logParserFactory;

    private static volatile AsyncLogParsersCoordinator instance;

    public static void initialize(int maxWorkers, boolean logData, String eventReportingMode,
            LogParserFactory logParserFactory) {
        if (instance == null) {
            synchronized (AsyncLogParsersCoordinator.class) {
                if (instance == null) {
                    instance = new AsyncLogParsersCoordinator(maxWorkers, logData, eventReportingMode,
                            logParserFactory);
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
            LogParserFactory logParserFactory) {
        this.maxWorkers = Math.max(1, maxWorkers);
        this.logData = logData;
        this.eventReportingMode = eventReportingMode;
        this.logParserFactory = logParserFactory;
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

        dispatcherThread = new Thread(new Runnable() {
            @Override
            public void run() {
                dispatchTestEvents();
            }
        }, "TestEventDispatcherThread");
        dispatcherThread.start();

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
            callback);
        workers.put(logFilePath, worker);
        @SuppressWarnings("unused")
        java.util.concurrent.Future<?> unused = workersExecutor.submit(worker);
    }

    @Override
    public void deinit() {
        synchronized (lock) {
            if (!isInitialized) {
                return;
            }

            isInitialized = false;

            if (workersExecutor != null) {
                workersExecutor.shutdownNow();
                try {
                    workersExecutor.awaitTermination(EXECUTOR_TERMINATION_TIMEOUT_SECONDS,
                        TimeUnit.SECONDS);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                workersExecutor = null;
            }

            if (dispatcherThread != null) {
                dispatcherThread.interrupt();
                try {
                    dispatcherThread.join(DISPATCHER_THREAD_JOIN_TIMEOUT_MS);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                dispatcherThread = null;
            }

            subscribers.clear();
            workers.clear();
            testEventQueue.clear();
        }
    }

    private void dispatchTestEvents() {
        try {
            while (isInitialized) {
                TestEvent event = testEventQueue.take();
                publish(event);
            }
        } catch (InterruptedException e) {
            Log.i(LOG_TAG, "Dispatcher thread interrupted.");
            Thread.currentThread().interrupt();
        } finally {
            Log.i(LOG_TAG, "Flushing remaining events.");
            while (!testEventQueue.isEmpty()) {
                TestEvent event = testEventQueue.poll();
                if (event != null) {
                    publish(event);
                }
            }
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
