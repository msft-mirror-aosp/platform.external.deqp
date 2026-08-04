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
import org.easymock.EasyMock;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.easymock.EasyMock.*;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
public class LogParserWorkerTest {

    private LogParser mockParser;
    private BlockingQueue<TestEvent> testEventQueue;
    private File tempFile;

    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();

    @Before
    public void setUp() throws IOException {
        mockParser = createMock(LogParser.class);
        testEventQueue = new LinkedBlockingQueue<>();
        tempFile = tempFolder.newFile("test_log.qpa");
    }

    @Test
    public void testConstructor_nullParserThrows() {
        try {
            new LogParserWorker(null, testEventQueue, tempFile.getAbsolutePath(), true, null);
            fail("Should throw IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            assertEquals("parser cannot be null", e.getMessage());
        }
    }

    @Test
    public void testConstructor_nullQueueThrows() {
        try {
            new LogParserWorker(mockParser, null, tempFile.getAbsolutePath(), true, null);
            fail("Should throw IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            assertEquals("testEventQueue cannot be null", e.getMessage());
        }
    }

    @Test
    public void testConstructor_nullLogFileThrows() {
        try {
            new LogParserWorker(mockParser, testEventQueue, null, true, null);
            fail("Should throw IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            assertEquals("logFile cannot be null", e.getMessage());
        }
    }

    @Test
    public void testRun_normalFlow() throws Exception {
        mockParser.init(isA(TestSessionEventsAccumulator.class), eq(tempFile.getAbsolutePath()), eq(true));
        expectLastCall().once();
        LogParserWorker.TimingSettings config = new LogParserWorker.TimingSettings(
            /* noActivitySleepMs = */ 1,
            /* noDataSleepMs = */ 2,
            /* noDataTimeoutMs = */ 5,
            LogParserWorker.TimingSettings.DEFAULT_FILE_READY_TIMEOUT_MS);
        LogParserWorker worker = new LogParserWorker(mockParser, testEventQueue,
            tempFile.getAbsolutePath(), true, null, config);

        // Phase 1 parsing loop
        expect(mockParser.parse()).andReturn(true).once();
        expect(mockParser.parse()).andAnswer(new org.easymock.IAnswer<Boolean>() {
            @Override
            public Boolean answer() throws Throwable {
                worker.onTestProcessFinished();
                return false;
            }
        }).once();

        // Phase 2 post-mortem loop (draining remaining buffer)
        expect(mockParser.parse()).andReturn(false).atLeastOnce();

        mockParser.deinit();
        expectLastCall().once();

        replay(mockParser);

        worker.run();

        verify(mockParser);
    }

    @Test
    public void testRun_waitLoop_blocksUntilFileExists() throws Exception {
        final File nonExistentFile = new File(tempFolder.getRoot(), "missing.qpa");
        assertFalse(nonExistentFile.exists());

        mockParser.init(isA(TestSessionEventsAccumulator.class), eq(nonExistentFile.getAbsolutePath()),
            eq(true));
        expectLastCall().once();

        expect(mockParser.parse()).andReturn(false).once();

        // Post-mortem
        expect(mockParser.parse()).andReturn(false).anyTimes();

        mockParser.deinit();
        expectLastCall().once();

        replay(mockParser);

        LogParserWorker.TimingSettings config = new LogParserWorker.TimingSettings(1, 1, 0,
            LogParserWorker.TimingSettings.DEFAULT_FILE_READY_TIMEOUT_MS);
        LogParserWorker worker = new LogParserWorker(mockParser, testEventQueue,
            nonExistentFile.getAbsolutePath(), true, null, config);

        Thread thread = new Thread(worker);
        thread.start();

        // Wait a bit to ensure it is looping/waiting
        Thread.sleep(50);
        assertTrue(thread.isAlive());

        // Create the file to unblock it
        assertTrue(nonExistentFile.createNewFile());

        // Signal process death to break parse loop
        worker.onTestProcessFinished();

        // Wait for thread to finish
        thread.join(2000);
        assertFalse(thread.isAlive());

        verify(mockParser);
    }

    @Test
    public void testRun_interruptedDuringWait_exitsCleanly() throws Exception {
        final File nonExistentFile = new File(tempFolder.getRoot(), "interrupted_missing.qpa");

        mockParser.deinit();
        expectLastCall().once();

        replay(mockParser);

        LogParserWorker worker = new LogParserWorker(mockParser, testEventQueue,
            nonExistentFile.getAbsolutePath(), true, null);

        Thread thread = new Thread(worker);
        thread.start();

        // Wait a bit, then interrupt
        Thread.sleep(50);
        assertTrue(thread.isAlive());

        thread.interrupt();

        thread.join(2000);
        assertFalse(thread.isAlive());

        verify(mockParser);
    }

    @Test
    public void testRun_parserException_callsDeinit() throws Exception {
        mockParser.init(isA(TestSessionEventsAccumulator.class), eq(tempFile.getAbsolutePath()), eq(true));
        expectLastCall().once();

        // parse() throws exception
        expect(mockParser.parse()).andThrow(new IOException("Simulated parse error")).once();

        mockParser.deinit();
        expectLastCall().once();

        replay(mockParser);

        LogParserWorker worker = new LogParserWorker(mockParser, testEventQueue,
            tempFile.getAbsolutePath(), true, null);
        worker.run();

        verify(mockParser);
    }
}
