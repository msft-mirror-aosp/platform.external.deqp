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

package com.drawelements.deqp.testercore;

import java.io.IOException;

/**
 * Interface for parsing test execution logs and publishing events to a listener.
 * Implementations process log streams and forward test lifecycle and result events
 * to a {@link TestEventListener}.
 */
public interface LogParser {
    /**
     * Initializes the parser with the target event listener and log file location.
     *
     * @param testEventListener Target listener to receive parsed test lifecycle events.
     * @param logFileName Absolute path to the test log file.
     * @param logData Flag indicating whether detailed log output should be parsed and broadcast.
     * @throws IOException If an error occurs opening or accessing the log file.
     */
    void init(TestEventListener testEventListener, String logFileName, boolean logData) throws IOException;

    /**
     * Parses available chunks of data from the log file and dispatches corresponding events.
     *
     * @return true if data was available and processed, false if EOF was reached.
     * @throws IOException If an error occurs while reading the log file.
     */
    boolean parse() throws IOException;

    /**
     * Releases any resources or file descriptors held by the parser.
     *
     * @throws IOException If an error occurs while closing resources.
     */
    void deinit() throws IOException;
}
