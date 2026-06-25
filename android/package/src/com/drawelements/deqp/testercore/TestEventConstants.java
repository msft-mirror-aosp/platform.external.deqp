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

public final class TestEventConstants {
    public static final String KEY_EVENT_TYPE = "dEQP-EventType";

    public static final String BEGIN_SESSION = "BeginSession";
    public static final String SESSION_INFO = "SessionInfo";
    public static final String END_SESSION = "EndSession";
    public static final String BEGIN_TEST_CASE = "BeginTestCase";
    public static final String TEST_LOG_DATA = "TestLogData";
    public static final String TEST_CASE_RESULT = "TestCaseResult";
    public static final String END_TEST_CASE = "EndTestCase";
    public static final String TERMINATE_TEST_CASE = "TerminateTestCase";

    public static final String KEY_BEGIN_TEST_CASE_PATH = "dEQP-BeginTestCase-TestCasePath";
    public static final String KEY_TEST_LOG_DATA_LOG = "dEQP-TestLogData-Log";
    public static final String KEY_TEST_CASE_RESULT_CODE = "dEQP-TestCaseResult-Code";
    public static final String KEY_TEST_CASE_RESULT_DETAILS = "dEQP-TestCaseResult-Details";
    public static final String KEY_TERMINATE_TEST_CASE_REASON = "dEQP-TerminateTestCase-Reason";
    public static final String KEY_SESSION_INFO_NAME = "dEQP-SessionInfo-Name";
    public static final String KEY_SESSION_INFO_VALUE = "dEQP-SessionInfo-Value";

    private TestEventConstants() {} // Prevent instantiation
}
