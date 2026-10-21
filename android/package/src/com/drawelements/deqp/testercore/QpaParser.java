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
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

public class QpaParser implements LogParser {
    private static final int MAX_REUSABLE_LOG_BUFFER_CHARS = 1024 * 1024;

    private LineReader mLineReader;

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
        this.mLineReader = new LineReader(new File(filePath));
        resetLogData();
        this.currentTestPath = null;
    }

    @Override
    public void deinit() throws IOException {
        if (mLineReader != null) {
            try {
                mLineReader.flushRemaining();
            } finally {
                try {
                    mLineReader.close();
                } finally {
                    mLineReader = null;
                    currentLogData = new StringBuilder();
                }
            }
        }
    }

    private void resetLogData() {
        if (currentLogData.capacity() > MAX_REUSABLE_LOG_BUFFER_CHARS) {
            currentLogData = new StringBuilder();
        } else {
            currentLogData.setLength(0);
        }
    }

    /**
     * Called by the Orchestrator to read available lines from the log file.
     * If EOF is reached (waiting for worker to finish rendering), returns false.
     * @return true if data was read, false if EOF reached.
     */
    @Override
    public boolean parse() throws IOException {
        if (mLineReader == null) {
            return false;
        }

        long bytesBefore = mLineReader.getTotalBytesRead();
        boolean gotData = false;
        String line;

        while ((line = mLineReader.nextLine()) != null) {
            processLine(line);
            gotData = true;
        }

        boolean newBytes = mLineReader.getTotalBytesRead() > bytesBefore;
        return gotData || newBytes;
    }

    private void processLine(String line) {
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
            resetLogData();

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

    private class LineReader implements AutoCloseable {
        private static final int BUFFER_SIZE_BYTES = 64 * 1024;

        private final RandomAccessFile mRaf;
        private byte[] mReadBuffer = new byte[BUFFER_SIZE_BYTES];
        private long mTotalBytesRead = 0;
        private int mBufferPos = 0;
        private int mBufferEnd = 0;

        LineReader(File file) throws IOException {
            // "r" mode: We do not lock the file. The C++ worker can write to it freely.
            this.mRaf = new RandomAccessFile(file, "r");
        }

        String nextLine() throws IOException {
            while (true) {
                // 1. Try to find a newline in the buffered data
                int newlineIdx = -1;
                for (int i = mBufferPos; i < mBufferEnd; i++) {
                    if (mReadBuffer[i] == '\n') {
                        newlineIdx = i;
                        break;
                    }
                }

                if (newlineIdx != -1) {
                    return extractString(newlineIdx);
                }

                // 2. Buffer is empty/partial, try to refill from disk
                if (!refill()) {
                    return null; // EOF or no new data yet
                }
            }
        }

        private String extractString(int newlineIdx) {
            String line = createString(newlineIdx);
            mBufferPos = newlineIdx + 1; // Advance past the newline character
            return line;
        }

        void flushRemaining() {
            if (mBufferEnd > mBufferPos) {
                String line = createString(mBufferEnd);
                mBufferPos = mBufferEnd;
                processLine(line);
            }
        }

        private String createString(int endIndex) {
            int length = endIndex - mBufferPos;

            // QPA logs may contain CRLF line endings on some platforms, 
            // so we strip the carriage return (\r) if it precedes the newline.
            if (length > 0 && mReadBuffer[endIndex - 1] == '\r') {
                length--;
            }

            return new String(mReadBuffer, mBufferPos, length, StandardCharsets.UTF_8);
        }

        private boolean refill() throws IOException {
            // Compact buffer by shifting remaining unparsed bytes to the front.
            if (mBufferPos > 0) {
                int remaining = mBufferEnd - mBufferPos;
                if (remaining > 0) {
                    System.arraycopy(mReadBuffer, mBufferPos, mReadBuffer, 0, remaining);
                }
                mBufferPos = 0;
                mBufferEnd = remaining;
            }

            // Shrink buffer back to default capacity if empty and previously expanded.
            if (mBufferEnd == 0 && mReadBuffer.length > BUFFER_SIZE_BYTES) {
                mReadBuffer = new byte[BUFFER_SIZE_BYTES];
            }

            // Expand buffer if full of a single line that exceeds current buffer capacity
            if (mBufferEnd == mReadBuffer.length) {
                mReadBuffer = Arrays.copyOf(mReadBuffer, mReadBuffer.length * 2);
            }

            // Read next chunk from file
            int bytesRead = mRaf.read(mReadBuffer, mBufferEnd, mReadBuffer.length - mBufferEnd);
            if (bytesRead <= 0) {
                return false;
            }

            mTotalBytesRead += bytesRead;
            mBufferEnd += bytesRead;
            return true;
        }

        long getTotalBytesRead() {
            return mTotalBytesRead;
        }

        @Override
        public void close() throws IOException {
            mRaf.close();
        }
    }
}
