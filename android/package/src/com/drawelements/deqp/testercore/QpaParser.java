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

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;

public class QpaParser implements LogParser {
    private RandomAccessFile raf;
    private StringBuilder currentLogData = new StringBuilder();
    private String currentTestPath = null;
    private boolean mLogDataEnabled;
    private TestEventListener mTestEventListener;

    static final String TAG_BEGIN_SESSION = "#beginSession";
    static final String TAG_END_SESSION = "#endSession";
    static final String TAG_SESSION_INFO = "#sessionInfo";
    static final String TAG_BEGIN_TEST_CASE_RESULT = "#beginTestCaseResult";
    static final String TAG_END_TEST_CASE_RESULT = "#endTestCaseResult";
    static final String TAG_TERMINATE_TEST_CASE_RESULT = "#terminateTestCaseResult";
    static final String TAG_TERMINATE_TEST_CASE_PREFIX = "#terminateTestCase";

    static final String TAG_RESULT_START = "<Result StatusCode=";
    static final String TAG_RESULT_END = "</Result>";
    static final String TAG_STATUS_CODE_PREFIX = "StatusCode=\"";

    private static final int SESSION_INFO_VALUE_OFFSET = 13;
    private static final int SESSION_INFO_PARTS_COUNT = 2;
    private static final int BEGIN_TEST_CASE_VALUE_OFFSET = 21;
    private static final int TERMINATE_TEST_CASE_VALUE_OFFSET = 25;

    private static final String DEFAULT_STATUS_CODE = "Fail";

    public QpaParser() {
    }

    @Override
    public void init(TestEventListener testEventListener, String filePath, boolean logDataEnabled) throws IOException {
        this.mTestEventListener = testEventListener;
        this.mLogDataEnabled = logDataEnabled;
        // "r" mode: We do not lock the file. The C++ worker can write to it freely.
        this.raf = new RandomAccessFile(new File(filePath), "r");
    }

    @Override
    public void deinit() throws IOException {
        if (raf != null) raf.close();
    }

    /**
     * Called by the Orchestrator to read available lines from the log file.
     * If EOF is reached (waiting for worker to finish rendering), returns false.
     * @return true if data was read, false if EOF reached.
     */
    @Override
    public boolean parse() throws IOException {
        String line;
        long currentPosition = raf.getFilePointer();
        boolean gotData = false;

        while ((line = raf.readLine()) != null) {
            gotData = true;

            if (line.startsWith(TAG_BEGIN_SESSION)) {
                beginSession();
            }
            else if (line.startsWith(TAG_END_SESSION)) {
                endSession();
            }
            else if (line.startsWith(TAG_SESSION_INFO)) {
                // Format: #sessionInfo name value
                String[] parts = line.substring(SESSION_INFO_VALUE_OFFSET).trim().split("\\s+", SESSION_INFO_PARTS_COUNT);
                if (parts.length == SESSION_INFO_PARTS_COUNT) {
                    String name = parts[0];
                    String value = parseSessionInfoValue(parts[1]);
                    sessionInfo(name, value);
                }
            }
            else if (line.startsWith(TAG_BEGIN_TEST_CASE_RESULT)) {
                currentTestPath = line.substring(BEGIN_TEST_CASE_VALUE_OFFSET).trim();
                currentLogData.setLength(0); // Clear string builder for new test

                beginTestCase(currentTestPath);
            }
            else if (line.startsWith(TAG_END_TEST_CASE_RESULT) || line.startsWith(TAG_TERMINATE_TEST_CASE_RESULT)) {
                boolean isTerminate = line.startsWith(TAG_TERMINATE_TEST_CASE_PREFIX);
                String terminateReason = isTerminate ? line.substring(TERMINATE_TEST_CASE_VALUE_OFFSET).trim() : "";

                String logText = currentLogData.toString();
                String statusCode = extractStatusCode(logText);
                String details = extractDetails(logText, statusCode);

                if (mLogDataEnabled && logText.length() > 0) {
                    String fullXml = "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n" +
                        "<?xml-stylesheet href=\"testlog.xsl\" type=\"text/xsl\"?>\n" +
                        logText;

                    try {
                        testLogData(fullXml);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                }

                testCaseResult(statusCode, isTerminate ? terminateReason : details);
                if (isTerminate) {
                    terminateTestCase(terminateReason);
                } else {
                    endTestCase();
                }
            }
            else {
                currentLogData.append(line).append("\n");
            }

            currentPosition = raf.getFilePointer();
        }

        raf.seek(currentPosition);
        return gotData;
    }

    private void testCaseResult(String code, String details) {
        if (mTestEventListener != null) {
            mTestEventListener.testCaseResult(code, details);
        }
    }

    private void beginTestCase(String testCase) {
        if (mTestEventListener != null) {
            mTestEventListener.beginTestCase(testCase);
        }
    }

    private void endTestCase() {
        if (mTestEventListener != null) {
            mTestEventListener.endTestCase();
        }
    }

    private void testLogData(String log) throws InterruptedException {
        if (mTestEventListener != null) {
            mTestEventListener.testLogData(log);
        }
    }

    private void beginSession() {
        if (mTestEventListener != null) {
            mTestEventListener.beginSession();
        }
    }

    private void endSession() {
        if (mTestEventListener != null) {
            mTestEventListener.endSession();
        }
    }

    private void sessionInfo(String name, String value) {
        if (mTestEventListener != null) {
            mTestEventListener.sessionInfo(name, value);
        }
    }

    private void terminateTestCase(String reason) {
        if (mTestEventListener != null) {
            mTestEventListener.terminateTestCase(reason);
        }
    }

    private String extractStatusCode(String log) {
        int idx = log.lastIndexOf(TAG_STATUS_CODE_PREFIX);

        if (idx != -1) {
            int start = idx + TAG_STATUS_CODE_PREFIX.length();
            int end = log.indexOf("\"", start);
            if (end != -1) {
                return log.substring(start, end);
            }
        }
        return DEFAULT_STATUS_CODE;
    }

    private String parseSessionInfoValue(String str) {
        if (str == null || str.isEmpty()) {
            return "";
        }
        int offset = 0;
        char firstChar = str.charAt(offset);
        boolean isString = firstChar == '"' || firstChar == '\'';
        char quotChar = isString ? firstChar : 0;

        if (isString) {
            offset += 1;
        }

        StringBuilder dst = new StringBuilder();
        while (offset < str.length()) {
            char curChar = str.charAt(offset);
            boolean isEnd = isString ? (curChar == quotChar)
                    : (curChar == ' ' || curChar == '\n' || curChar == '\r');

            if (isEnd) {
                break;
            } else {
                dst.append(curChar);
            }
            offset += 1;
        }

        return dst.toString();
    }

    private String extractDetails(String log, String defaultDetails) {
        int resultStart = log.lastIndexOf(TAG_RESULT_START);
        if (resultStart != -1) {
            int detailsStart = log.indexOf(">", resultStart) + 1;
            int detailsEnd = log.indexOf(TAG_RESULT_END, detailsStart);
            if (detailsStart > 0 && detailsEnd > detailsStart) {
                return log.substring(detailsStart, detailsEnd);
            }
        }
        return defaultDetails;
    }
}
