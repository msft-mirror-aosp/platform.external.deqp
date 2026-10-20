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

import android.os.Bundle;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

@RunWith(RobolectricTestRunner.class)
public class QpaParserTest {

    private File tempQpaFile;
    private QpaParser parser;

    private static class TestDeqpInstrumentation extends DeqpInstrumentation {
        final List<Bundle> results = new ArrayList<>();

        @Override
        public void testLogData(String log) throws InterruptedException {
            final int chunkSize = 4 * 1024;
            while (log != null) {
                String message;
                if (log.length() > chunkSize) {
                    message = log.substring(0, chunkSize);
                    log = log.substring(chunkSize);
                } else {
                    message = log;
                    log = null;
                }
                Bundle info = new Bundle();
                info.putString("dEQP-EventType", "TestLogData");
                info.putString("dEQP-TestLogData-Log", message);
                sendStatus(0, info);
            }
        }

        @Override
        public void sendStatus(int resultCode, Bundle results) {
            this.results.add(results);
        }
    }

    @Before
    public void setUp() throws IOException {
        tempQpaFile = File.createTempFile("test_live_qpa", ".qpa");
    }

    @After
    public void tearDown() {
        if (parser != null) {
            try { parser.deinit(); } catch (IOException e) {
                // Ignore deinit errors for test teardown
            }
        }
        if (tempQpaFile.exists()) {
            tempQpaFile.delete();
        }
    }

    private void appendToFile(String content) throws IOException {
        try (FileOutputStream fos = new FileOutputStream(tempQpaFile, true)) {
            fos.write(content.getBytes());
        }
    }

    @Test
    public void testSessionLifecycleEvents() throws IOException {
        // Spec: #sessionInfo must precede #beginSession
        appendToFile(QpaParser.TAG_SESSION_INFO + " releaseName android-pie\n");
        appendToFile(QpaParser.TAG_BEGIN_SESSION + "\n");
        appendToFile(QpaParser.TAG_END_SESSION + "\n");

        TestDeqpInstrumentation instrumentation = new TestDeqpInstrumentation();
        parser = new QpaParser();
        parser.init(instrumentation, tempQpaFile.getAbsolutePath(), false);
        boolean gotData = parser.parse();

        assertTrue("Parser should return true on #endSession", gotData);
        List<Bundle> results = instrumentation.results;
        assertEquals(3, results.size());

        assertEquals("SessionInfo", results.get(0).getString("dEQP-EventType"));
        assertEquals("releaseName", results.get(0).getString("dEQP-SessionInfo-Name"));
        assertEquals("android-pie", results.get(0).getString("dEQP-SessionInfo-Value"));

        assertEquals("BeginSession", results.get(1).getString("dEQP-EventType"));

        assertEquals("EndSession", results.get(2).getString("dEQP-EventType"));
    }

    @Test
    public void testPassedTestCaseWithoutLogData() throws IOException {
        appendToFile(QpaParser.TAG_BEGIN_TEST_CASE_RESULT + " dEQP-GLES3.info.version\n");
        // Spec: Test cases must contain valid XML stream wrapping the result
        appendToFile("<?xml version=\"1.0\"?>\n");
        appendToFile("<TestCaseResult Version=\"0.3.2\" CasePath=\"dEQP-GLES3.info.version\" CaseType=\"SelfValidate\">\n");
        appendToFile("<Result StatusCode=\"Pass\">Successfully passed</Result>\n");
        appendToFile("</TestCaseResult>\n");
        appendToFile(QpaParser.TAG_END_TEST_CASE_RESULT + "\n");

        TestDeqpInstrumentation instrumentation = new TestDeqpInstrumentation();
        parser = new QpaParser();
        parser.init(instrumentation, tempQpaFile.getAbsolutePath(), false);
        boolean gotData = parser.parse();

        assertTrue(gotData);
        List<Bundle> results = instrumentation.results;
        assertNotNull(results);
        assertEquals(3, results.size());

        assertEquals("BeginTestCase", results.get(0).getString("dEQP-EventType"));
        assertEquals("dEQP-GLES3.info.version", results.get(0).getString("dEQP-BeginTestCase-TestCasePath"));

        // With mLogDataEnabled = false, the raw XML should be skipped, yielding only the Result code
        assertEquals("TestCaseResult", results.get(1).getString("dEQP-EventType"));
        assertEquals("Pass", results.get(1).getString("dEQP-TestCaseResult-Code"));
        assertEquals("Successfully passed", results.get(1).getString("dEQP-TestCaseResult-Details"));

        assertEquals("EndTestCase", results.get(2).getString("dEQP-EventType"));
    }

    @Test
    public void testFailedTestCaseWithLogDataAndChunking() throws IOException {
        appendToFile(QpaParser.TAG_BEGIN_TEST_CASE_RESULT + " dEQP-GLES3.info.fail\n");

        StringBuilder largeLog = new StringBuilder();
        largeLog.append("<?xml version=\"1.0\"?>\n");
        largeLog.append("<TestCaseResult Version=\"0.3.2\" CasePath=\"dEQP-GLES3.info.fail\" CaseType=\"SelfValidate\">\n");
        for (int i = 0; i < 5000; i++) {
            largeLog.append("A");
        }
        largeLog.append("\n<Result StatusCode=\"Fail\">Failed badly</Result>\n");
        largeLog.append("</TestCaseResult>\n");
        appendToFile(largeLog.toString());
        appendToFile(QpaParser.TAG_END_TEST_CASE_RESULT + "\n");

        TestDeqpInstrumentation instrumentation = new TestDeqpInstrumentation();
        parser = new QpaParser();
        parser.init(instrumentation, tempQpaFile.getAbsolutePath(), true);
        boolean gotData = parser.parse();

        assertTrue(gotData);
        List<Bundle> results = instrumentation.results;
        assertNotNull(results);

        // Expected bundles: BeginTestCase, TestLogData(Chunk 1), TestLogData(Chunk 2), TestCaseResult, EndTestCase
        assertEquals(5, results.size());

        Bundle chunk1 = results.get(1);
        assertEquals("TestLogData", chunk1.getString("dEQP-EventType"));
        assertTrue(chunk1.getString("dEQP-TestLogData-Log").startsWith("<?xml version=\"1.0\""));
        assertEquals(4096, chunk1.getString("dEQP-TestLogData-Log").length()); // Max CHUNK_SIZE

        Bundle chunk2 = results.get(2);
        assertEquals("TestLogData", chunk2.getString("dEQP-EventType"));

        Bundle resultBundle = results.get(3);
        assertEquals("TestCaseResult", resultBundle.getString("dEQP-EventType"));
        assertEquals("Fail", resultBundle.getString("dEQP-TestCaseResult-Code"));
        assertEquals("Failed badly", resultBundle.getString("dEQP-TestCaseResult-Details"));
    }

    @Test
    public void testTerminateTestCase() throws IOException {
        appendToFile(QpaParser.TAG_BEGIN_TEST_CASE_RESULT + " dEQP-GLES3.info.crash\n");
        appendToFile("<?xml version=\"1.0\"?>\n");
        appendToFile("<TestCaseResult Version=\"0.3.2\" CasePath=\"dEQP-GLES3.info.crash\" CaseType=\"SelfValidate\">\n");
        appendToFile("<Text>Some logs before crash...</Text>\n");
        appendToFile(QpaParser.TAG_TERMINATE_TEST_CASE_RESULT + " Native crash occurred\n");

        TestDeqpInstrumentation instrumentation = new TestDeqpInstrumentation();
        parser = new QpaParser();
        parser.init(instrumentation, tempQpaFile.getAbsolutePath(), false);
        boolean gotData = parser.parse();

        assertTrue(gotData);
        List<Bundle> results = instrumentation.results;
        assertNotNull(results);
        assertEquals(3, results.size());

        assertEquals("BeginTestCase", results.get(0).getString("dEQP-EventType"));

        assertEquals("TestCaseResult", results.get(1).getString("dEQP-EventType"));
        assertEquals("Fail", results.get(1).getString("dEQP-TestCaseResult-Code")); // Default fallback
        assertEquals("Native crash occurred", results.get(1).getString("dEQP-TestCaseResult-Details"));

        assertEquals("TerminateTestCase", results.get(2).getString("dEQP-EventType"));
        assertEquals("Native crash occurred", results.get(2).getString("dEQP-TerminateTestCase-Reason"));
    }

    @Test
    public void testLiveParsingYieldsOnIncompleteTest() throws IOException {
        TestDeqpInstrumentation instrumentation = new TestDeqpInstrumentation();
        parser = new QpaParser();
        parser.init(instrumentation, tempQpaFile.getAbsolutePath(), false);

        // 1. Write start of test and opening XML but no end tag
        appendToFile(QpaParser.TAG_BEGIN_TEST_CASE_RESULT + " dEQP-GLES3.info.running\n");
        appendToFile("<?xml version=\"1.0\"?>\n");
        appendToFile("<TestCaseResult Version=\"0.3.2\" CasePath=\"dEQP-GLES3.info.running\" CaseType=\"SelfValidate\">\n");
        appendToFile("<Text>Log line 1</Text>\n");

        // Parser immediately sends BeginTestCase and buffers the XML logs until end tag
        boolean firstRead = parser.parse();
        assertTrue("Parser should read available lines successfully", firstRead);
        assertEquals(1, instrumentation.results.size());
        assertEquals("BeginTestCase", instrumentation.results.get(0).getString("dEQP-EventType"));

        // 2. Append the completion tags
        appendToFile("<Result StatusCode=\"Pass\">OK</Result>\n");
        appendToFile("</TestCaseResult>\n");
        appendToFile(QpaParser.TAG_END_TEST_CASE_RESULT + "\n");

        // Parser should now successfully finish test result parsing
        boolean secondRead = parser.parse();
        assertTrue("Parser should complete parsing on end tag", secondRead);
        List<Bundle> secondReadResults = instrumentation.results;
        assertEquals(3, secondReadResults.size());
        assertEquals("Pass", secondReadResults.get(1).getString("dEQP-TestCaseResult-Code"));
    }

    @Test
    public void testWarningOrUnsupportedStatusCode() throws IOException {
        // Spec uses strict statuses. Replacing invalid custom status test with NotSupported.
        appendToFile(QpaParser.TAG_BEGIN_TEST_CASE_RESULT + " dEQP-GLES3.info.unsupported_ext\n");
        appendToFile("<?xml version=\"1.0\"?>\n");
        appendToFile("<TestCaseResult Version=\"0.3.2\" CasePath=\"dEQP-GLES3.info.unsupported_ext\" CaseType=\"Capability\">\n");
        appendToFile("<Result StatusCode=\"NotSupported\">Extension not available</Result>\n");
        appendToFile("</TestCaseResult>\n");
        appendToFile(QpaParser.TAG_END_TEST_CASE_RESULT + "\n");

        TestDeqpInstrumentation instrumentation = new TestDeqpInstrumentation();
        parser = new QpaParser();
        parser.init(instrumentation, tempQpaFile.getAbsolutePath(), false);
        boolean gotData = parser.parse();

        assertTrue(gotData);
        List<Bundle> results = instrumentation.results;
        assertNotNull(results);
        assertEquals(3, results.size());
        assertEquals("TestCaseResult", results.get(1).getString("dEQP-EventType"));
        assertEquals("NotSupported", results.get(1).getString("dEQP-TestCaseResult-Code"));
    }

    @Test
    public void testNoStatusCodeFallback() throws IOException {
        appendToFile(QpaParser.TAG_BEGIN_TEST_CASE_RESULT + " dEQP-GLES3.info.nofallback\n");
        appendToFile("<?xml version=\"1.0\"?>\n");
        appendToFile("<TestCaseResult Version=\"0.3.2\" CasePath=\"dEQP-GLES3.info.nofallback\" CaseType=\"SelfValidate\">\n");
        appendToFile("<Result>No status code here</Result>\n");
        appendToFile("</TestCaseResult>\n");
        appendToFile(QpaParser.TAG_END_TEST_CASE_RESULT + "\n");

        TestDeqpInstrumentation instrumentation = new TestDeqpInstrumentation();
        parser = new QpaParser();
        parser.init(instrumentation, tempQpaFile.getAbsolutePath(), false);
        boolean gotData = parser.parse();

        assertTrue(gotData);
        List<Bundle> results = instrumentation.results;
        assertNotNull(results);
        assertEquals(3, results.size());
        assertEquals("TestCaseResult", results.get(1).getString("dEQP-EventType"));
        assertEquals("Fail", results.get(1).getString("dEQP-TestCaseResult-Code"));
    }

    @Test
    public void testLiveParsingWithPartialLineRead() throws IOException {
        TestDeqpInstrumentation instrumentation = new TestDeqpInstrumentation();
        parser = new QpaParser();
        parser.init(instrumentation, tempQpaFile.getAbsolutePath(), false);

        // 1. Write the begin tag and the first half of a log line
        appendToFile(QpaParser.TAG_BEGIN_TEST_CASE_RESULT + " dEQP-GLES3.info.partial\n");
        appendToFile("<?xml version=\"1.0\"?>\n");
        appendToFile("<TestCaseResult Version=\"0.3.2\" CasePath=\"dEQP-G"); // Notice: No newline character here

        boolean firstRead = parser.parse();
        assertTrue("Parser should parse complete lines", firstRead);
        assertEquals(1, instrumentation.results.size());

        // 2. Write the remaining half of the line and finish the test case
        appendToFile("LES3.info.partial\" CaseType=\"SelfValidate\">\n");
        appendToFile("<Result StatusCode=\"Pass\">OK</Result>\n");
        appendToFile("</TestCaseResult>\n");
        appendToFile(QpaParser.TAG_END_TEST_CASE_RESULT + "\n");

        // Parser should now successfully re-assemble the split line, flush, and return true
        boolean secondRead = parser.parse();
        assertTrue("Parser should return true once end tag and full lines are written", secondRead);
        List<Bundle> secondReadResults = instrumentation.results;
        assertEquals(3, secondReadResults.size());

        assertEquals("BeginTestCase", secondReadResults.get(0).getString("dEQP-EventType"));
        assertEquals("dEQP-GLES3.info.partial", secondReadResults.get(0).getString("dEQP-BeginTestCase-TestCasePath"));

        assertEquals("TestCaseResult", secondReadResults.get(1).getString("dEQP-EventType"));
        assertEquals("Pass", secondReadResults.get(1).getString("dEQP-TestCaseResult-Code"));

        assertEquals("EndTestCase", secondReadResults.get(2).getString("dEQP-EventType"));
    }

    @Test
    public void testFlexibleSessionInfoQuotes() throws IOException {
        appendToFile(QpaParser.TAG_SESSION_INFO + " name1 \"value1\"\n");
        appendToFile(QpaParser.TAG_SESSION_INFO + " name2 'value2'\n");
        appendToFile(QpaParser.TAG_SESSION_INFO + " name3 value3\n");

        TestDeqpInstrumentation instrumentation = new TestDeqpInstrumentation();
        parser = new QpaParser();
        parser.init(instrumentation, tempQpaFile.getAbsolutePath(), false);
        boolean gotData = parser.parse();

        assertTrue(gotData);
        List<Bundle> results = instrumentation.results;
        assertEquals(3, results.size());

        assertEquals("value1", results.get(0).getString("dEQP-SessionInfo-Value"));
        assertEquals("value2", results.get(1).getString("dEQP-SessionInfo-Value"));
        assertEquals("value3", results.get(2).getString("dEQP-SessionInfo-Value"));
    }
}
