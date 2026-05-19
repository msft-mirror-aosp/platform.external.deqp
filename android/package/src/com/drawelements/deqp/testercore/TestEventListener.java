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

/**
 * Interface for receiving test session lifecycle events, test execution status,
 * and test log data parsed from dEQP execution output.
 */
public interface TestEventListener {
    /**
     * Reports the final outcome and associated details of the current test case.
     *
     * @param code    The status code of the result (e.g., "Pass", "Fail", "NotSupported").
     * @param details Additional status description or failure summary.
     */
    void testCaseResult(String code, String details);

    /**
     * Called when execution of a new test case commences.
     *
     * @param testCase The fully-qualified path of the test case.
     */
    void beginTestCase(String testCase);

    /**
     * Called when the execution of the current test case successfully concludes.
     */
    void endTestCase();

    /**
     * Transmits raw XML test log content produced during test execution.
     *
     * @param log The raw log data XML string.
     * @throws InterruptedException If the receiving thread is interrupted while publishing log chunks.
     */
    void testLogData(String log) throws InterruptedException;

    /**
     * Called when a test session commences.
     */
    void beginSession();

    /**
     * Called when a test session finishes execution.
     */
    void endSession();

    /**
     * Reports session execution metadata attributes and environment properties.
     *
     * @param name  The name of the parameter attribute.
     * @param value The value associated with the attribute.
     */
    void sessionInfo(String name, String value);

    /**
     * Called when a test case is forcefully halted or aborted unexpectedly.
     *
     * @param reason The specific reason or failure summary leading to termination.
     */
    void terminateTestCase(String reason);
}

