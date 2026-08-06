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

/**
 * Interface for coordinating parallel log parsing of multiple log files.
 */
public interface LogParsersCoordinator {

    /**
     * Starts or schedules parsing for the specified test log file.
     *
     * @param logFilePath path to the log file to be parsed
     */
    void parse(String logFilePath);

    /**
     * Notifies the coordinator that the test process generating the specified log file has finished,
     * allowing the parser to complete processing and clean up worker resources.
     *
     * @param logFilePath path to the log file whose test process has terminated
     */
    void onTestProcessFinished(String logFilePath);

    /**
     * Registers a subscriber to receive test execution events emitted during log parsing.
     *
     * @param subscriber the subscriber to add
     */
    void subscribe(TestEventSubscriber subscriber);

    /**
     * Unregisters a previously subscribed test event listener.
     *
     * @param subscriber the subscriber to remove
     */
    void unsubscribe(TestEventSubscriber subscriber);

    /**
     * Shuts down the coordinator, terminating active workers, clearing pending events, and removing
     * all subscribers.
     */
    void deinit();
}
