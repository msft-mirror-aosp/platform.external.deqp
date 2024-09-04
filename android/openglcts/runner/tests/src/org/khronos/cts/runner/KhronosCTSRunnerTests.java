/*
 * Copyright (C) 2024 The Android Open Source Project
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
 */
package org.khronos.cts.runner;

import com.android.compatibility.common.tradefed.build.CompatibilityBuildHelper;
import com.android.ddmlib.IDevice;
import com.android.ddmlib.IShellOutputReceiver;
import com.android.tradefed.build.IFolderBuildInfo;
import com.android.tradefed.config.ConfigurationException;
import com.android.tradefed.config.OptionSetter;
import com.android.tradefed.device.DeviceNotAvailableException;
import com.android.tradefed.device.IManagedTestDevice;
import com.android.tradefed.device.ITestDevice;
import com.android.tradefed.metrics.proto.MetricMeasurement.Metric;
import com.android.tradefed.result.ITestInvocationListener;
import com.android.tradefed.result.TestDescription;
import com.android.tradefed.testtype.Abi;
import com.android.tradefed.testtype.IAbi;
import com.android.tradefed.util.AbiUtils;
import com.android.tradefed.util.FileUtil;

import com.drawelements.deqp.runner.DeqpTestRunner;

import junit.framework.TestCase;

import org.easymock.EasyMock;
import org.easymock.IAnswer;
import org.easymock.IMocksControl;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * Unit tests for {@link KhronosCTSRunner}.
 * Note: This file is a copy of DeqpTestRunnerTest.java with a few changed types.
 */
public class KhronosCTSRunnerTests extends TestCase {
    private static final IAbi ABI = new Abi("armeabi-v7a", "32");
    private static final String APP_DIR = "/sdcard/";
    private static final String CASE_LIST_FILE_NAME = "dEQP-TestCaseList.txt";
    private static final String LOG_FILE_NAME = "TestLog.qpa";
    private static final String TEST_RUN_FILE_AND_PARAM = "--deqp-caselist-resource=gles3-caselist.txt,--deqp-screen-rotation=unspecified,--deqp-surface-width=256,--deqp-surface-height=256,--deqp-watchdog=disable,--deqp-gl-config-name=rgba8888d24s8ms0,";
    private static final String TEST_FAILURE_MESSAGE_CONFIG = "=== with config --deqp-gl-config-name=rgba8888d24s8ms0 --deqp-screen-rotation=unspecified --deqp-surface-height=256 --deqp-surface-width=256 --deqp-watchdog=disable ===";
    private static final String TEST_ID_NAME = "KhronosGLCTS";

    private File mTestsDir = null;

    private boolean mLogData;

    public static class BuildHelperMock extends CompatibilityBuildHelper {
        private File mTestsDir = null;
        public BuildHelperMock(IFolderBuildInfo buildInfo, File testsDir) {
            super(buildInfo);
            mTestsDir = testsDir;
        }
        @Override
        public File getTestsDir() throws FileNotFoundException {
            return mTestsDir;
        }
    }

    private static CompatibilityBuildHelper getMockBuildHelper(File testsDir) {
        IFolderBuildInfo mockIFolderBuildInfo = EasyMock.createMock(IFolderBuildInfo.class);
        EasyMock.expect(mockIFolderBuildInfo.getBuildAttributes()).andReturn(new HashMap<>()).anyTimes();
        EasyMock.replay(mockIFolderBuildInfo);
        return new BuildHelperMock(mockIFolderBuildInfo, testsDir);
    }

    private static KhronosCTSBatchRunConfiguration parseRunParam(String runParam)
    {
        int index = 0;
        HashMap<String, String> runConfigParam = new HashMap<String, String>();
        while (index < runParam.length())
        {
            int nextParamArgValIndex = runParam.substring(index).indexOf(',');
            if (nextParamArgValIndex < 0)
            {
                break;
            }
            String nextParamArgVal = runParam.substring(index, index + nextParamArgValIndex);
            int argValDivIndex = nextParamArgVal.indexOf('=');
            String nextParamArg = nextParamArgVal.substring(0, argValDivIndex);
            String nextParamVal = nextParamArgVal.substring(argValDivIndex + 1);
            runConfigParam.put(nextParamArg, nextParamVal);
            index = index + nextParamArgValIndex+1;
        }
        return new KhronosCTSBatchRunConfiguration(runConfigParam);
    }

    private static KhronosCTSBatchRunConfiguration parseTestRunParams(String testRunParam) {
        final String testRunParamArgCaseListResource = "--deqp-caselist-resource";
        int indexOfCaseListFileBegin = testRunParam.indexOf(testRunParamArgCaseListResource);
        if (indexOfCaseListFileBegin == -1)
        {
            return new KhronosCTSBatchRunConfiguration(new HashMap<String, String>());
        }
        int indexOfCaseListFileEnd = testRunParam.substring(indexOfCaseListFileBegin).indexOf(',');
        String runParam = testRunParam.substring(indexOfCaseListFileEnd+1);
        return parseRunParam(runParam);
    }


    private static KhronosCTSRunner buildKhronosCTSRunner(Collection<TestDescription> tests, File testsDir) throws ConfigurationException, IOException {
        StringWriter testlist = new StringWriter();
        for (TestDescription test : tests) {
            testlist.write(test.getClassName() + "." + test.getTestName() + "\n");
        }
        return buildKhronosCTSRunner(testlist.toString(), testsDir);
    }

    private static KhronosCTSRunner buildKhronosCTSRunner(String testlist, File testsDir) throws ConfigurationException, IOException {
        KhronosCTSRunner runner = new KhronosCTSRunner();
        final File caselistsFile = new File(testsDir, "gles3-caselist.txt");
        FileUtil.writeToFile(testlist, caselistsFile);

        runner.setAbi(ABI);
        runner.setBuildHelper(getMockBuildHelper(testsDir));

        return runner;
    }

    private void runInstrumentationLineAndAnswer(ITestDevice mockDevice, IDevice mockIDevice,
            final String output) throws Exception {

        runInstrumentationLineAndAnswer(mockDevice, mockIDevice, null, null, output);
    }

    private void runInstrumentationLineAndAnswer(ITestDevice mockDevice, IDevice mockIDevice,
            final String testTrie, String cmd, final String output) throws Exception {
        if (cmd==null){
            StringBuilder khronosCTSCmdLine = new StringBuilder();
            khronosCTSCmdLine.append("--deqp-caselist-file=");
            khronosCTSCmdLine.append(APP_DIR + CASE_LIST_FILE_NAME);
            khronosCTSCmdLine.append(" ");
            khronosCTSCmdLine.append(parseTestRunParams(TEST_RUN_FILE_AND_PARAM).getId());
            if(!mLogData) {
                khronosCTSCmdLine.append(" ");
                khronosCTSCmdLine.append("--deqp-log-images=disable");
            }
            cmd = khronosCTSCmdLine.toString();
        }

        EasyMock.expect(mockDevice.executeShellCommand(EasyMock.eq("rm " + APP_DIR + CASE_LIST_FILE_NAME)))
                .andReturn("").once();

        EasyMock.expect(mockDevice.executeShellCommand(EasyMock.eq("rm " + APP_DIR + LOG_FILE_NAME)))
                .andReturn("").once();

        if (testTrie == null) {
            mockDevice.pushString((String)EasyMock.anyObject(), EasyMock.eq(APP_DIR + CASE_LIST_FILE_NAME));
        }
        else {
            mockDevice.pushString(testTrie + "\n", APP_DIR + CASE_LIST_FILE_NAME);
        }
        EasyMock.expectLastCall().andReturn(true).once();

        final String instrumentationName =
                "org.khronos.gl_cts/org.khronos.cts.testercore.KhronosCTSInstrumentation";

        final String command = String.format(
                "am instrument %s -w -e khronosCTSLogFileName \"%s\" -e khronosCTSCmdLine \"%s\" -e deqpLogData \"%s\" %s",
                AbiUtils.createAbiFlag(ABI.getName()), APP_DIR + LOG_FILE_NAME, cmd, mLogData, instrumentationName);

        EasyMock.expect(mockDevice.getIDevice()).andReturn(mockIDevice);
        mockIDevice.executeShellCommand(EasyMock.eq(command),
                EasyMock.<IShellOutputReceiver>notNull(), EasyMock.anyLong(),
                EasyMock.isA(TimeUnit.class));

        EasyMock.expectLastCall().andAnswer(new IAnswer<Object>() {
            @Override
            public Object answer() {
                IShellOutputReceiver receiver
                        = (IShellOutputReceiver)EasyMock.getCurrentArguments()[1];

                receiver.addOutput(output.getBytes(), 0, output.length());
                receiver.flush();

                return null;
            }
        });
    }

    static private String buildTestProcessOutput(List<TestDescription> tests) {
        /* MultiLineReceiver expects "\r\n" line ending. */
        final String outputHeader = "INSTRUMENTATION_STATUS: dEQP-SessionInfo-Name=releaseName\r\n"
                + "INSTRUMENTATION_STATUS: dEQP-EventType=SessionInfo\r\n"
                + "INSTRUMENTATION_STATUS: dEQP-SessionInfo-Value=2014.x\r\n"
                + "INSTRUMENTATION_STATUS_CODE: 0\r\n"
                + "INSTRUMENTATION_STATUS: dEQP-SessionInfo-Name=releaseId\r\n"
                + "INSTRUMENTATION_STATUS: dEQP-EventType=SessionInfo\r\n"
                + "INSTRUMENTATION_STATUS: dEQP-SessionInfo-Value=0xcafebabe\r\n"
                + "INSTRUMENTATION_STATUS_CODE: 0\r\n"
                + "INSTRUMENTATION_STATUS: dEQP-SessionInfo-Name=targetName\r\n"
                + "INSTRUMENTATION_STATUS: dEQP-EventType=SessionInfo\r\n"
                + "INSTRUMENTATION_STATUS: dEQP-SessionInfo-Value=android\r\n"
                + "INSTRUMENTATION_STATUS_CODE: 0\r\n"
                + "INSTRUMENTATION_STATUS: dEQP-EventType=BeginSession\r\n"
                + "INSTRUMENTATION_STATUS_CODE: 0\r\n";

        final String outputEnd = "INSTRUMENTATION_STATUS: dEQP-EventType=EndSession\r\n"
                + "INSTRUMENTATION_STATUS_CODE: 0\r\n"
                + "INSTRUMENTATION_CODE: 0\r\n";

        StringWriter output = new StringWriter();
        output.write(outputHeader);
        for (TestDescription test : tests) {
            output.write("INSTRUMENTATION_STATUS: dEQP-EventType=BeginTestCase\r\n");
            output.write("INSTRUMENTATION_STATUS: dEQP-BeginTestCase-TestCasePath=");
            output.write(test.getClassName());
            output.write(".");
            output.write(test.getTestName());
            output.write("\r\n");
            output.write("INSTRUMENTATION_STATUS_CODE: 0\r\n");
            output.write("INSTRUMENTATION_STATUS: dEQP-TestCaseResult-Code=Pass\r\n");
            output.write("INSTRUMENTATION_STATUS: dEQP-TestCaseResult-Details=Pass\r\n");
            output.write("INSTRUMENTATION_STATUS: dEQP-EventType=TestCaseResult\r\n");
            output.write("INSTRUMENTATION_STATUS_CODE: 0\r\n");
            output.write("INSTRUMENTATION_STATUS: dEQP-EventType=EndTestCase\r\n");
            output.write("INSTRUMENTATION_STATUS_CODE: 0\r\n");
        }
        output.write(outputEnd);
        return output.toString();
    }


    private void runGetTestsParamsInstrumentation(ITestDevice mockDevice, IDevice mockIDevice) throws Exception
    {

        final String getTestsParamsActivity =
            "org.khronos.gl_cts/org.khronos.cts.ES32GetTestParamActivity";

        final String testsParamsFileName =
            "/sdcard/cts-test-params.xml";

        final String instrumentationName =
            "org.khronos.gl_cts/org.khronos.cts.testercore.KhronosCTSInstrumentation";

        String command = String.format(
            "am instrument %s -w -e khronosCTSTestName \"%s\" -e khronosCTSTestParamFileName \"%s\" %s",
            AbiUtils.createAbiFlag(ABI.getName()),
            getTestsParamsActivity,
            testsParamsFileName,
            instrumentationName);

        final String output = "INSTRUMENTATION_STATUS: dEQP-EventType=BeginTestRunParamsCollection\r\n"
                            +"INSTRUMENTATION_STATUS_CODE: 0\r\n"
                            +"INSTRUMENTATION_STATUS: dEQP-EventType=BeginTestRunParams\r\n"
                            +"INSTRUMENTATION_STATUS: dEQP-TestRunParam="+TEST_RUN_FILE_AND_PARAM+"\r\n"
                            +"INSTRUMENTATION_STATUS_CODE: 0\r\n"
                            +"INSTRUMENTATION_STATUS: dEQP-EventType=EndTestRunParams\r\n"
                            +"INSTRUMENTATION_STATUS_CODE: 0\r\n"
                            +"INSTRUMENTATION_STATUS: dEQP-EventType=EndTestRunParamsCollection\r\n"
                            +"INSTRUMENTATION_STATUS_CODE: 0\r\n"
                            +"INSTRUMENTATION_CODE: 0\r\n";


        EasyMock.expect(mockDevice.getIDevice()).andReturn(mockIDevice);

        mockIDevice.executeShellCommand(EasyMock.eq(command),
                EasyMock.<IShellOutputReceiver>notNull(), EasyMock.anyLong(),
                EasyMock.isA(TimeUnit.class));

        EasyMock.expectLastCall().andAnswer(new IAnswer<Object>() {
            @Override
            public Object answer() {
                IShellOutputReceiver receiver
                        = (IShellOutputReceiver)EasyMock.getCurrentArguments()[1];

                receiver.addOutput(output.getBytes(), 0, output.length());
                receiver.flush();

                return null;
            }
        });
    }

    private static String getTestId() {
        return AbiUtils.createId(ABI.getName(), TEST_ID_NAME);
    }

    private void testFiltering(KhronosCTSRunner khronosCTSRunner,
                               String expectedTrie,
                               List<TestDescription> expectedTests) throws Exception {
        ITestDevice mockDevice = EasyMock.createMock(ITestDevice.class);
        IDevice mockIDevice = EasyMock.createMock(IDevice.class);
        ITestInvocationListener mockListener = EasyMock.createStrictMock(ITestInvocationListener.class);

        // Expect the calls twice: setupTestEnvironment() and teardownTestEnvironment()
        EasyMock.expect(mockDevice.executeShellCommand(EasyMock.eq("settings delete global angle_gl_driver_selection_pkgs"))).
            andReturn("").once();
        EasyMock.expect(mockDevice.executeShellCommand(EasyMock.eq("settings delete global angle_gl_driver_selection_values"))).
            andReturn("").once();
        EasyMock.expect(mockDevice.executeShellCommand(EasyMock.eq("settings delete global angle_gl_driver_selection_pkgs"))).
            andReturn("").once();
        EasyMock.expect(mockDevice.executeShellCommand(EasyMock.eq("settings delete global angle_gl_driver_selection_values"))).
            andReturn("").once();

        // Expect the runGetTestsParamsActivity() is called once, no matter if there is test to be ran
        runGetTestsParamsInstrumentation(mockDevice, mockIDevice);

        mockListener.testRunStarted(getTestId(), expectedTests.size());
        EasyMock.expectLastCall().once();

        boolean thereAreTests = !expectedTests.isEmpty();
        if (thereAreTests)
        {
            String testOut = buildTestProcessOutput(expectedTests);
            runInstrumentationLineAndAnswer(mockDevice, mockIDevice, expectedTrie, null, testOut);

            for (int i = 0; i < expectedTests.size(); i++) {
                mockListener.testStarted(EasyMock.eq(expectedTests.get(i)));
                EasyMock.expectLastCall().once();

                mockListener.testEnded(EasyMock.eq(expectedTests.get(i)),
                                       EasyMock.<HashMap<String, Metric>>notNull());

                EasyMock.expectLastCall().once();
            }
        }

        mockListener.testRunEnded(EasyMock.anyLong(), EasyMock.<HashMap<String, Metric>>notNull());
        EasyMock.expectLastCall().once();

        EasyMock.replay(mockDevice, mockIDevice);
        EasyMock.replay(mockListener);

        khronosCTSRunner.setDevice(mockDevice);
        khronosCTSRunner.run(mockListener);

        EasyMock.verify(mockListener);
        EasyMock.verify(mockDevice, mockIDevice);
    }

    /**
     * Test running multiple test cases
     */
    public void testRun_multipleTests() throws Exception {
        final TestDescription[] testIds = {
            new TestDescription("dEQP-GLES3.info", "vendor"),
            new TestDescription("dEQP-GLES3.info", "renderer"),
            new TestDescription("dEQP-GLES3.info", "version"),
            new TestDescription("dEQP-GLES3.info", "shading_language_version"),
            new TestDescription("dEQP-GLES3.info", "extensions"),
            new TestDescription("dEQP-GLES3.info", "render_target")
        };

        final String expectedTrie
                = "{dEQP-GLES3{info{vendor,renderer,version,shading_language_version,extensions,render_target}}}";

        List<TestDescription> allTests = new ArrayList<TestDescription>();
        for (TestDescription id : testIds) {
            allTests.add(id);
        }

        List<TestDescription> activeTests = new ArrayList<TestDescription>();
        for (TestDescription id: testIds) {
            activeTests.add(id);
        }

        KhronosCTSRunner khronosCTSRunner = buildKhronosCTSRunner(allTests, mTestsDir);

        OptionSetter setter = new OptionSetter(khronosCTSRunner);
        mLogData = false;
        setter.setOptionValue("deqp-log-result-details", mLogData ? "true" : "false");

        testFiltering(khronosCTSRunner, expectedTrie, activeTests);
    }

    public void testRun_trivialIncludeFilter() throws Exception {
        final TestDescription[] testIds = {
                new TestDescription("dEQP-GLES3.missing", "no"),
                new TestDescription("dEQP-GLES3.missing", "nope"),
                new TestDescription("dEQP-GLES3.missing", "donotwant"),
                new TestDescription("dEQP-GLES3.pick_me", "yes"),
                new TestDescription("dEQP-GLES3.pick_me", "ok"),
                new TestDescription("dEQP-GLES3.pick_me", "accepted"),
        };

        List<TestDescription> allTests = new ArrayList<TestDescription>();
        for (TestDescription id : testIds) {
            allTests.add(id);
        }

        List<TestDescription> activeTests = new ArrayList<TestDescription>();
        activeTests.add(testIds[3]);
        activeTests.add(testIds[4]);
        activeTests.add(testIds[5]);

        String expectedTrie = "{dEQP-GLES3{pick_me{yes,ok,accepted}}}";

        KhronosCTSRunner khronosCTSRunner = buildKhronosCTSRunner(allTests, mTestsDir);
        OptionSetter setter = new OptionSetter(khronosCTSRunner);
        setter.setOptionValue("include-filter", "dEQP-GLES3.pick_me#*");
        mLogData = false;
        setter.setOptionValue("deqp-log-result-details", mLogData ? "true" : "false");

        testFiltering(khronosCTSRunner, expectedTrie, activeTests);
    }

    public void testRun_trivialExcludeFilter() throws Exception {
        final TestDescription[] testIds = {
                new TestDescription("dEQP-GLES3.missing", "no"),
                new TestDescription("dEQP-GLES3.missing", "nope"),
                new TestDescription("dEQP-GLES3.missing", "donotwant"),
                new TestDescription("dEQP-GLES3.pick_me", "yes"),
                new TestDescription("dEQP-GLES3.pick_me", "ok"),
                new TestDescription("dEQP-GLES3.pick_me", "accepted"),
        };

        List<TestDescription> allTests = new ArrayList<TestDescription>();
        for (TestDescription id : testIds) {
            allTests.add(id);
        }

        List<TestDescription> activeTests = new ArrayList<TestDescription>();
        activeTests.add(testIds[3]);
        activeTests.add(testIds[4]);
        activeTests.add(testIds[5]);

        String expectedTrie = "{dEQP-GLES3{pick_me{yes,ok,accepted}}}";

        KhronosCTSRunner khronosCTSRunner = buildKhronosCTSRunner(allTests, mTestsDir);
        OptionSetter setter = new OptionSetter(khronosCTSRunner);
        setter.setOptionValue("exclude-filter", "dEQP-GLES3.missing#*");
        mLogData = false;
        setter.setOptionValue("deqp-log-result-details", mLogData ? "true" : "false");

        testFiltering(khronosCTSRunner, expectedTrie, activeTests);
    }

    public void testRun_includeAndExcludeFilter() throws Exception {
        final TestDescription[] testIds = {
                new TestDescription("dEQP-GLES3.group1", "foo"),
                new TestDescription("dEQP-GLES3.group1", "nope"),
                new TestDescription("dEQP-GLES3.group1", "donotwant"),
                new TestDescription("dEQP-GLES3.group2", "foo"),
                new TestDescription("dEQP-GLES3.group2", "yes"),
                new TestDescription("dEQP-GLES3.group2", "thoushallnotpass"),
        };

        List<TestDescription> allTests = new ArrayList<TestDescription>();
        for (TestDescription id : testIds) {
            allTests.add(id);
        }

        List<TestDescription> activeTests = new ArrayList<TestDescription>();
        activeTests.add(testIds[4]);

        String expectedTrie = "{dEQP-GLES3{group2{yes}}}";

        KhronosCTSRunner khronosCTSRunner = buildKhronosCTSRunner(allTests, mTestsDir);

        OptionSetter setter = new OptionSetter(khronosCTSRunner);
        mLogData = false;
        setter.setOptionValue("deqp-log-result-details", mLogData ? "true" : "false");

        Set<String> includes = new HashSet<>();
        includes.add("dEQP-GLES3.group2#*");
        khronosCTSRunner.addAllIncludeFilters(includes);

        Set<String> excludes = new HashSet<>();
        excludes.add("*foo");
        excludes.add("*thoushallnotpass");
        khronosCTSRunner.addAllExcludeFilters(excludes);
        testFiltering(khronosCTSRunner, expectedTrie, activeTests);
    }

    public void testRun_includeAll() throws Exception {
        final TestDescription[] testIds = {
                new TestDescription("dEQP-GLES3.group1", "mememe"),
                new TestDescription("dEQP-GLES3.group1", "yeah"),
                new TestDescription("dEQP-GLES3.group1", "takeitall"),
                new TestDescription("dEQP-GLES3.group2", "jeba"),
                new TestDescription("dEQP-GLES3.group2", "yes"),
                new TestDescription("dEQP-GLES3.group2", "granted"),
        };

        List<TestDescription> allTests = new ArrayList<TestDescription>();
        for (TestDescription id : testIds) {
            allTests.add(id);
        }

        String expectedTrie = "{dEQP-GLES3{group1{mememe,yeah,takeitall},group2{jeba,yes,granted}}}";

        KhronosCTSRunner khronosCTSRunner = buildKhronosCTSRunner(allTests, mTestsDir);

        OptionSetter setter = new OptionSetter(khronosCTSRunner);
        mLogData = false;
        setter.setOptionValue("deqp-log-result-details", mLogData ? "true" : "false");

        khronosCTSRunner.addIncludeFilter("*");
        testFiltering(khronosCTSRunner, expectedTrie, allTests);
    }

    public void testRun_excludeAll() throws Exception {
        final TestDescription[] testIds = {
                new TestDescription("dEQP-GLES3.group1", "no"),
                new TestDescription("dEQP-GLES3.group1", "nope"),
                new TestDescription("dEQP-GLES3.group1", "nottoday"),
                new TestDescription("dEQP-GLES3.group2", "banned"),
                new TestDescription("dEQP-GLES3.group2", "notrecognized"),
                new TestDescription("dEQP-GLES3.group2", "-2"),
        };

        List<TestDescription> allTests = new ArrayList<TestDescription>();
        for (TestDescription id : testIds) {
            allTests.add(id);
        }

        KhronosCTSRunner khronosCTSRunner = buildKhronosCTSRunner(allTests, mTestsDir);
        OptionSetter setter = new OptionSetter(khronosCTSRunner);
        mLogData = false;
        setter.setOptionValue("deqp-log-result-details", mLogData ? "true" : "false");
        khronosCTSRunner.addExcludeFilter("*");
        String expectedTrie = "";

        List<TestDescription> activeTests = new ArrayList<TestDescription>();
        testFiltering(khronosCTSRunner, expectedTrie, activeTests);
    }

    public void testDotToHashConversionInFilters() throws Exception {
        final TestDescription[] testIds = {
                new TestDescription("dEQP-GLES3.missing", "no"),
                new TestDescription("dEQP-GLES3.pick_me", "donotwant"),
                new TestDescription("dEQP-GLES3.pick_me", "yes")
        };

        List<TestDescription> allTests = new ArrayList<TestDescription>();
        for (TestDescription id : testIds) {
            allTests.add(id);
        }

        List<TestDescription> activeTests = new ArrayList<TestDescription>();
        activeTests.add(testIds[2]);

        String expectedTrie = "{dEQP-GLES3{pick_me{yes}}}";

        KhronosCTSRunner khronosCTSRunner = buildKhronosCTSRunner(allTests, mTestsDir);
        OptionSetter setter = new OptionSetter(khronosCTSRunner);
        mLogData = false;
        setter.setOptionValue("deqp-log-result-details", mLogData ? "true" : "false");
        khronosCTSRunner.addIncludeFilter("dEQP-GLES3.pick_me.yes");
        testFiltering(khronosCTSRunner, expectedTrie, activeTests);
    }

    /**
     * Test running a unexecutable test.
     */
    public void testRun_unexecutableTests() throws Exception {
        final String instrumentationAnswerNoExecs =
                "INSTRUMENTATION_STATUS: dEQP-SessionInfo-Name=releaseName\r\n"
                + "INSTRUMENTATION_STATUS: dEQP-EventType=SessionInfo\r\n"
                + "INSTRUMENTATION_STATUS: dEQP-SessionInfo-Value=2014.x\r\n"
                + "INSTRUMENTATION_STATUS_CODE: 0\r\n"
                + "INSTRUMENTATION_STATUS: dEQP-SessionInfo-Name=releaseId\r\n"
                + "INSTRUMENTATION_STATUS: dEQP-EventType=SessionInfo\r\n"
                + "INSTRUMENTATION_STATUS: dEQP-SessionInfo-Value=0xcafebabe\r\n"
                + "INSTRUMENTATION_STATUS_CODE: 0\r\n"
                + "INSTRUMENTATION_STATUS: dEQP-SessionInfo-Name=targetName\r\n"
                + "INSTRUMENTATION_STATUS: dEQP-EventType=SessionInfo\r\n"
                + "INSTRUMENTATION_STATUS: dEQP-SessionInfo-Value=android\r\n"
                + "INSTRUMENTATION_STATUS_CODE: 0\r\n"
                + "INSTRUMENTATION_STATUS: dEQP-EventType=BeginSession\r\n"
                + "INSTRUMENTATION_STATUS_CODE: 0\r\n"
                + "INSTRUMENTATION_STATUS: dEQP-EventType=EndSession\r\n"
                + "INSTRUMENTATION_STATUS_CODE: 0\r\n"
                + "INSTRUMENTATION_CODE: 0\r\n";

        final TestDescription[] testIds = {
                new TestDescription("dEQP-GLES3.missing", "no"),
                new TestDescription("dEQP-GLES3.missing", "nope"),
                new TestDescription("dEQP-GLES3.missing", "donotwant"),
        };

        final String[] testPaths = {
                "dEQP-GLES3.missing.no",
                "dEQP-GLES3.missing.nope",
                "dEQP-GLES3.missing.donotwant",
        };

        ITestDevice mockDevice = EasyMock.createMock(ITestDevice.class);
        ITestInvocationListener mockListener
                = EasyMock.createStrictMock(ITestInvocationListener.class);
        IDevice mockIDevice = EasyMock.createMock(IDevice.class);

        Collection<TestDescription> allTests = new ArrayList<TestDescription>();

        for (TestDescription id : testIds) {
            allTests.add(id);
        }

        KhronosCTSRunner khronosCTSRunner = buildKhronosCTSRunner(allTests, mTestsDir);
        OptionSetter setter = new OptionSetter(khronosCTSRunner);
        mLogData = false;
        setter.setOptionValue("deqp-log-result-details", mLogData ? "true" : "false");

        // first try
        runInstrumentationLineAndAnswer(mockDevice, mockIDevice,
                "{dEQP-GLES3{missing{no,nope,donotwant}}}", null, instrumentationAnswerNoExecs);

        // splitting begins
        runInstrumentationLineAndAnswer(mockDevice, mockIDevice,
                "{dEQP-GLES3{missing{no}}}", null, instrumentationAnswerNoExecs);
        runInstrumentationLineAndAnswer(mockDevice, mockIDevice,
                "{dEQP-GLES3{missing{nope,donotwant}}}", null, instrumentationAnswerNoExecs);
        runInstrumentationLineAndAnswer(mockDevice, mockIDevice,
                "{dEQP-GLES3{missing{nope}}}", null, instrumentationAnswerNoExecs);
        runInstrumentationLineAndAnswer(mockDevice, mockIDevice,
                "{dEQP-GLES3{missing{donotwant}}}", null, instrumentationAnswerNoExecs);

        mockListener.testRunStarted(getTestId(), testPaths.length);
        EasyMock.expectLastCall().once();

        // Expect the calls twice: setupTestEnvironment() and teardownTestEnvironment()
        EasyMock.expect(mockDevice.executeShellCommand(EasyMock.eq("settings delete global angle_gl_driver_selection_pkgs"))).
            andReturn("").once();
        EasyMock.expect(mockDevice.executeShellCommand(EasyMock.eq("settings delete global angle_gl_driver_selection_values"))).
            andReturn("").once();
        EasyMock.expect(mockDevice.executeShellCommand(EasyMock.eq("settings delete global angle_gl_driver_selection_pkgs"))).
            andReturn("").once();
        EasyMock.expect(mockDevice.executeShellCommand(EasyMock.eq("settings delete global angle_gl_driver_selection_values"))).
            andReturn("").once();

        // Expect the runGetTestsParamsActivity() is called once, no matter if there is test to be ran
        runGetTestsParamsInstrumentation(mockDevice, mockIDevice);

        for (int i = 0; i < testPaths.length; i++) {
            mockListener.testStarted(EasyMock.eq(testIds[i]));
            EasyMock.expectLastCall().once();

            mockListener.testFailed(EasyMock.eq(testIds[i]),
                    EasyMock.eq(TEST_FAILURE_MESSAGE_CONFIG+"\n"
                    + "Abort: Test cannot be executed"));
            EasyMock.expectLastCall().once();

            mockListener.testEnded(EasyMock.eq(testIds[i]),
                    EasyMock.<HashMap<String, Metric>>notNull());
            EasyMock.expectLastCall().once();
        }

        mockListener.testRunEnded(EasyMock.anyLong(), EasyMock.<HashMap<String, Metric>>notNull());
        EasyMock.expectLastCall().once();

        EasyMock.replay(mockDevice, mockIDevice);
        EasyMock.replay(mockListener);

        khronosCTSRunner.setDevice(mockDevice);
        khronosCTSRunner.run(mockListener);

        EasyMock.verify(mockListener);
        EasyMock.verify(mockDevice, mockIDevice);
    }

        /**
     * Test that result code produces correctly pass or fail.
     */
    private void testResultCode(final String resultCode, boolean pass) throws Exception {
        final TestDescription testId = new TestDescription("dEQP-GLES3.info", "version");
        final String testPath = "dEQP-GLES3.info.version";
        final String testTrie = "{dEQP-GLES3{info{version}}}";

        /* MultiLineReceiver expects "\r\n" line ending. */
        final String output = "INSTRUMENTATION_STATUS: dEQP-SessionInfo-Name=releaseName\r\n"
                + "INSTRUMENTATION_STATUS: dEQP-EventType=SessionInfo\r\n"
                + "INSTRUMENTATION_STATUS: dEQP-SessionInfo-Value=2014.x\r\n"
                + "INSTRUMENTATION_STATUS_CODE: 0\r\n"
                + "INSTRUMENTATION_STATUS: dEQP-SessionInfo-Name=releaseId\r\n"
                + "INSTRUMENTATION_STATUS: dEQP-EventType=SessionInfo\r\n"
                + "INSTRUMENTATION_STATUS: dEQP-SessionInfo-Value=0xcafebabe\r\n"
                + "INSTRUMENTATION_STATUS_CODE: 0\r\n"
                + "INSTRUMENTATION_STATUS: dEQP-SessionInfo-Name=targetName\r\n"
                + "INSTRUMENTATION_STATUS: dEQP-EventType=SessionInfo\r\n"
                + "INSTRUMENTATION_STATUS: dEQP-SessionInfo-Value=android\r\n"
                + "INSTRUMENTATION_STATUS_CODE: 0\r\n"
                + "INSTRUMENTATION_STATUS: dEQP-EventType=BeginSession\r\n"
                + "INSTRUMENTATION_STATUS_CODE: 0\r\n"
                + "INSTRUMENTATION_STATUS: dEQP-EventType=BeginTestCase\r\n"
                + "INSTRUMENTATION_STATUS: dEQP-BeginTestCase-TestCasePath=" + testPath + "\r\n"
                + "INSTRUMENTATION_STATUS_CODE: 0\r\n"
                + "INSTRUMENTATION_STATUS: dEQP-TestCaseResult-Code=" + resultCode + "\r\n"
                + "INSTRUMENTATION_STATUS: dEQP-TestCaseResult-Details=Detail" + resultCode + "\r\n"
                + "INSTRUMENTATION_STATUS: dEQP-EventType=TestCaseResult\r\n"
                + "INSTRUMENTATION_STATUS_CODE: 0\r\n"
                + "INSTRUMENTATION_STATUS: dEQP-EventType=EndTestCase\r\n"
                + "INSTRUMENTATION_STATUS_CODE: 0\r\n"
                + "INSTRUMENTATION_STATUS: dEQP-EventType=EndSession\r\n"
                + "INSTRUMENTATION_STATUS_CODE: 0\r\n"
                + "INSTRUMENTATION_CODE: 0\r\n";

        ITestDevice mockDevice = EasyMock.createMock(ITestDevice.class);
        ITestInvocationListener mockListener
                = EasyMock.createStrictMock(ITestInvocationListener.class);
        IDevice mockIDevice = EasyMock.createMock(IDevice.class);

        Collection<TestDescription> allTests = new ArrayList<TestDescription>();
        allTests.add(testId);

        KhronosCTSRunner khronosCTSRunner = buildKhronosCTSRunner(allTests, mTestsDir);

        runInstrumentationLineAndAnswer(mockDevice, mockIDevice, testTrie, null, output);

        mockListener.testRunStarted(getTestId(), 1);
        EasyMock.expectLastCall().once();

        // Expect the runGetTestsParamsActivity() is called once, no matter if there is test to be ran
        runGetTestsParamsInstrumentation(mockDevice, mockIDevice);

        // Expect the calls twice: setupTestEnvironment() and teardownTestEnvironment()
        EasyMock.expect(mockDevice.executeShellCommand(EasyMock.eq("settings delete global angle_gl_driver_selection_pkgs"))).
            andReturn("").once();
        EasyMock.expect(mockDevice.executeShellCommand(EasyMock.eq("settings delete global angle_gl_driver_selection_values"))).
            andReturn("").once();
        EasyMock.expect(mockDevice.executeShellCommand(EasyMock.eq("settings delete global angle_gl_driver_selection_pkgs"))).
            andReturn("").once();
        EasyMock.expect(mockDevice.executeShellCommand(EasyMock.eq("settings delete global angle_gl_driver_selection_values"))).
            andReturn("").once();

        mockListener.testStarted(EasyMock.eq(testId));
        EasyMock.expectLastCall().once();

        if (!pass) {
            mockListener.testFailed(testId,
                    TEST_FAILURE_MESSAGE_CONFIG+"\n"
                    + resultCode + ": Detail" + resultCode);

            EasyMock.expectLastCall().once();
        }

        mockListener.testEnded(EasyMock.eq(testId), EasyMock.<HashMap<String, Metric>>notNull());
        EasyMock.expectLastCall().once();

        mockListener.testRunEnded(EasyMock.anyLong(), EasyMock.<HashMap<String, Metric>>notNull());
        EasyMock.expectLastCall().once();

        EasyMock.replay(mockDevice, mockIDevice);
        EasyMock.replay(mockListener);

        khronosCTSRunner.setDevice(mockDevice);
        khronosCTSRunner.run(mockListener);

        EasyMock.verify(mockListener);
        EasyMock.verify(mockDevice, mockIDevice);
    }

    /**
     * Test Pass result code
     */
    public void testRun_resultPass() throws Exception {
        testResultCode("Pass", true);
    }

    /**
     * Test dEQP Fail result code.
     */
    public void testRun_resultFail() throws Exception {
        testResultCode("Fail", false);
    }

    /**
     * Test dEQP NotSupported result code.
     */
    public void testRun_resultNotSupported() throws Exception {
        testResultCode("NotSupported", true);
    }

    /**
     * Test dEQP QualityWarning result code.
     */
    public void testRun_resultQualityWarning() throws Exception {
        testResultCode("QualityWarning", true);
    }

    /**
     * Test dEQP CompatibilityWarning result code.
     */
    public void testRun_resultCompatibilityWarning() throws Exception {
        testResultCode("CompatibilityWarning", true);
    }

    /**
     * Test dEQP ResourceError result code.
     */
    public void testRun_resultResourceError() throws Exception {
        testResultCode("ResourceError", false);
    }

    /**
     * Test dEQP InternalError result code.
     */
    public void testRun_resultInternalError() throws Exception {
        testResultCode("InternalError", false);
    }

    /**
     * Test dEQP Crash result code.
     */
    public void testRun_resultCrash() throws Exception {
        testResultCode("Crash", false);
    }

    /**
     * Test dEQP Timeout result code.
     */
    public void testRun_resultTimeout() throws Exception {
        testResultCode("Timeout", false);
    }

    /**
     * Test interface to mock Tradefed device types.
     */
    public static interface RecoverableTestDevice extends ITestDevice, IManagedTestDevice {
    }

    private static enum RecoveryEvent {
        PROGRESS,
        FAIL_CONNECTION_REFUSED,
        FAIL_LINK_KILLED,
    }

    private void runRecoveryWithPattern(KhronosCTSRunner.Recovery recovery, RecoveryEvent[] events)
            throws DeviceNotAvailableException {
        for (RecoveryEvent event : events) {
            switch (event) {
                case PROGRESS:
                    recovery.onExecutionProgressed();
                    break;
                case FAIL_CONNECTION_REFUSED:
                    recovery.recoverConnectionRefused();
                    break;
                case FAIL_LINK_KILLED:
                    recovery.recoverComLinkKilled();
                    break;
            }
        }
    }

    private void setRecoveryExpectationWait(KhronosCTSRunner.ISleepProvider mockSleepProvider) {
        mockSleepProvider.sleep(EasyMock.gt(0));
        EasyMock.expectLastCall().once();
    }

    private void setRecoveryExpectationKillProcess(RecoverableTestDevice mockDevice,
            KhronosCTSRunner.ISleepProvider mockSleepProvider) throws DeviceNotAvailableException {
        EasyMock.expect(mockDevice.executeShellCommand(EasyMock.contains("ps"))).
                andReturn("root 1234 org.khronos.cts").once();

        EasyMock.expect(mockDevice.executeShellCommand(EasyMock.eq("kill -9 1234"))).
                andReturn("").once();

        // Recovery checks if kill failed
        mockSleepProvider.sleep(EasyMock.gt(0));
        EasyMock.expectLastCall().once();
        EasyMock.expect(mockDevice.executeShellCommand(EasyMock.contains("ps"))).
                andReturn("").once();
    }

    private void setRecoveryExpectationRecovery(RecoverableTestDevice mockDevice)
            throws DeviceNotAvailableException {
        EasyMock.expect(mockDevice.recoverDevice()).andReturn(true).once();
    }

    private void setRecoveryExpectationReboot(RecoverableTestDevice mockDevice)
            throws DeviceNotAvailableException {
        mockDevice.reboot();
        EasyMock.expectLastCall().once();
    }


    private int setRecoveryExpectationOfAConnFailure(RecoverableTestDevice mockDevice, int numConsecutiveErrors)
            throws DeviceNotAvailableException {
        switch (numConsecutiveErrors) {
            case 0:
            case 1:
                setRecoveryExpectationRecovery(mockDevice);
                return 2;
            case 2:
                setRecoveryExpectationReboot(mockDevice);
                return 3;
            default:
                return 4;
        }
    }

    private int setRecoveryExpectationOfAComKilled(RecoverableTestDevice mockDevice,
            KhronosCTSRunner.ISleepProvider mockSleepProvider, int numConsecutiveErrors)
            throws DeviceNotAvailableException {
        switch (numConsecutiveErrors) {
            case 0:
                setRecoveryExpectationWait(mockSleepProvider);
                setRecoveryExpectationKillProcess(mockDevice, mockSleepProvider);
                return 1;
            case 1:
                setRecoveryExpectationRecovery(mockDevice);
                setRecoveryExpectationKillProcess(mockDevice, mockSleepProvider);
                return 2;
            case 2:
                setRecoveryExpectationReboot(mockDevice);
                return 3;
            default:
                return 4;
        }
    }

    private void setRecoveryExpectationsOfAPattern(RecoverableTestDevice mockDevice,
            KhronosCTSRunner.ISleepProvider mockSleepProvider, RecoveryEvent[] events)
            throws DeviceNotAvailableException {
        int numConsecutiveErrors = 0;
        for (RecoveryEvent event : events) {
            switch (event) {
                case PROGRESS:
                    numConsecutiveErrors = 0;
                    break;
                case FAIL_CONNECTION_REFUSED:
                    numConsecutiveErrors = setRecoveryExpectationOfAConnFailure(mockDevice, numConsecutiveErrors);
                    break;
                case FAIL_LINK_KILLED:
                    numConsecutiveErrors = setRecoveryExpectationOfAComKilled(mockDevice,
                            mockSleepProvider, numConsecutiveErrors);
                    break;
            }
        }
    }

    /**
     * Test dEQP runner recovery state machine.
     */
    private void testRecoveryWithPattern(boolean expectSuccess, RecoveryEvent...pattern)
            throws Exception {
        KhronosCTSRunner.Recovery recovery = new KhronosCTSRunner.Recovery();
        IMocksControl orderedControl = EasyMock.createStrictControl();
        RecoverableTestDevice mockDevice = orderedControl.createMock(RecoverableTestDevice.class);
        EasyMock.expect(mockDevice.getSerialNumber()).andStubReturn("SERIAL");
        KhronosCTSRunner.ISleepProvider mockSleepProvider =
                orderedControl.createMock(KhronosCTSRunner.ISleepProvider.class);

        setRecoveryExpectationsOfAPattern(mockDevice, mockSleepProvider, pattern);

        orderedControl.replay();

        recovery.setDevice(mockDevice);
        recovery.setSleepProvider(mockSleepProvider);
        try {
            runRecoveryWithPattern(recovery, pattern);
            if (!expectSuccess) {
                fail("Expected DeviceNotAvailableException");
            }
        } catch (DeviceNotAvailableException ex) {
            if (expectSuccess) {
                fail("Did not expect DeviceNotAvailableException");
            }
        }

        orderedControl.verify();
    }

    public void testRecovery_NoEvents() throws Exception {
        testRecoveryWithPattern(true);
    }

    public void testRecovery_AllOk() throws Exception {
        testRecoveryWithPattern(true, RecoveryEvent.PROGRESS, RecoveryEvent.PROGRESS);
    }

    // conn fail patterns

    public void testRecovery_OneConnectionFailureBegin() throws Exception {
        testRecoveryWithPattern(true, RecoveryEvent.FAIL_CONNECTION_REFUSED,
                RecoveryEvent.PROGRESS);
    }

    public void testRecovery_TwoConnectionFailuresBegin() throws Exception {
        testRecoveryWithPattern(true, RecoveryEvent.FAIL_CONNECTION_REFUSED,
                RecoveryEvent.FAIL_CONNECTION_REFUSED, RecoveryEvent.PROGRESS);
    }

    public void testRecovery_ThreeConnectionFailuresBegin() throws Exception {
        testRecoveryWithPattern(false, RecoveryEvent.FAIL_CONNECTION_REFUSED,
                RecoveryEvent.FAIL_CONNECTION_REFUSED, RecoveryEvent.FAIL_CONNECTION_REFUSED);
    }

    public void testRecovery_OneConnectionFailureMid() throws Exception {
        testRecoveryWithPattern(true, RecoveryEvent.PROGRESS,
                RecoveryEvent.FAIL_CONNECTION_REFUSED, RecoveryEvent.PROGRESS);
    }

    public void testRecovery_TwoConnectionFailuresMid() throws Exception {
        testRecoveryWithPattern(true, RecoveryEvent.PROGRESS,
                RecoveryEvent.FAIL_CONNECTION_REFUSED, RecoveryEvent.FAIL_CONNECTION_REFUSED,
                RecoveryEvent.PROGRESS);
    }

    public void testRecovery_ThreeConnectionFailuresMid() throws Exception {
        testRecoveryWithPattern(false, RecoveryEvent.PROGRESS,
                RecoveryEvent.FAIL_CONNECTION_REFUSED, RecoveryEvent.FAIL_CONNECTION_REFUSED,
                RecoveryEvent.FAIL_CONNECTION_REFUSED, RecoveryEvent.PROGRESS);
    }

    // link fail patterns

    public void testRecovery_OneLinkFailureBegin() throws Exception {
        testRecoveryWithPattern(true, RecoveryEvent.FAIL_LINK_KILLED,
                RecoveryEvent.PROGRESS);
    }

        public void testRecovery_TwoLinkFailuresBegin() throws Exception {
        testRecoveryWithPattern(true, RecoveryEvent.FAIL_LINK_KILLED,
                RecoveryEvent.FAIL_LINK_KILLED, RecoveryEvent.PROGRESS);
    }

    public void testRecovery_ThreeLinkFailuresBegin() throws Exception {
        testRecoveryWithPattern(true, RecoveryEvent.FAIL_LINK_KILLED,
                RecoveryEvent.FAIL_LINK_KILLED, RecoveryEvent.FAIL_LINK_KILLED,
                RecoveryEvent.PROGRESS);
    }

    public void testRecovery_FourLinkFailuresBegin() throws Exception {
        testRecoveryWithPattern(false, RecoveryEvent.FAIL_LINK_KILLED,
                RecoveryEvent.FAIL_LINK_KILLED, RecoveryEvent.FAIL_LINK_KILLED,
                RecoveryEvent.FAIL_LINK_KILLED);
    }

    public void testRecovery_OneLinkFailureMid() throws Exception {
        testRecoveryWithPattern(true, RecoveryEvent.PROGRESS,
                RecoveryEvent.FAIL_LINK_KILLED, RecoveryEvent.PROGRESS);
    }

    public void testRecovery_TwoLinkFailuresMid() throws Exception {
        testRecoveryWithPattern(true, RecoveryEvent.PROGRESS,
                RecoveryEvent.FAIL_LINK_KILLED, RecoveryEvent.FAIL_LINK_KILLED,
                RecoveryEvent.PROGRESS);
    }

    public void testRecovery_ThreeLinkFailuresMid() throws Exception {
        testRecoveryWithPattern(true, RecoveryEvent.PROGRESS,
                RecoveryEvent.FAIL_LINK_KILLED, RecoveryEvent.FAIL_LINK_KILLED,
                RecoveryEvent.FAIL_LINK_KILLED, RecoveryEvent.PROGRESS);
    }

    public void testRecovery_FourLinkFailuresMid() throws Exception {
        testRecoveryWithPattern(false, RecoveryEvent.PROGRESS, RecoveryEvent.FAIL_LINK_KILLED,
                RecoveryEvent.FAIL_LINK_KILLED, RecoveryEvent.FAIL_LINK_KILLED,
                RecoveryEvent.FAIL_LINK_KILLED);
    }

    // mixed patterns

    public void testRecovery_MixedFailuresProgressBetween() throws Exception {
        testRecoveryWithPattern(true,
                RecoveryEvent.PROGRESS, RecoveryEvent.FAIL_LINK_KILLED,
                RecoveryEvent.PROGRESS, RecoveryEvent.FAIL_CONNECTION_REFUSED,
                RecoveryEvent.PROGRESS, RecoveryEvent.FAIL_LINK_KILLED,
                RecoveryEvent.PROGRESS, RecoveryEvent.FAIL_CONNECTION_REFUSED,
                RecoveryEvent.PROGRESS);
    }

    public void testRecovery_MixedFailuresNoProgressBetween() throws Exception {
        testRecoveryWithPattern(true,
                RecoveryEvent.PROGRESS, RecoveryEvent.FAIL_LINK_KILLED,
                RecoveryEvent.FAIL_CONNECTION_REFUSED, RecoveryEvent.FAIL_LINK_KILLED,
                RecoveryEvent.PROGRESS);
    }

        /**
     * Test recovery if process cannot be killed
     */
    public void testRecovery_unkillableProcess () throws Exception {
        KhronosCTSRunner.Recovery recovery = new KhronosCTSRunner.Recovery();
        IMocksControl orderedControl = EasyMock.createStrictControl();
        RecoverableTestDevice mockDevice = orderedControl.createMock(RecoverableTestDevice.class);
        KhronosCTSRunner.ISleepProvider mockSleepProvider =
                orderedControl.createMock(KhronosCTSRunner.ISleepProvider.class);

        // recovery attempts to kill the process after a timeout
        mockSleepProvider.sleep(EasyMock.gt(0));
        EasyMock.expect(mockDevice.executeShellCommand(EasyMock.contains("ps"))).
                andReturn("root 1234 com.drawelement.deqp").once();
        EasyMock.expect(mockDevice.executeShellCommand(EasyMock.eq("kill -9 1234"))).
                andReturn("").once();

        // Recovery checks if kill failed
        mockSleepProvider.sleep(EasyMock.gt(0));
        EasyMock.expectLastCall().once();
        EasyMock.expect(mockDevice.executeShellCommand(EasyMock.contains("ps"))).
                andReturn("root 1234 com.drawelement.deqp").once();

        // Recovery resets the connection
        EasyMock.expect(mockDevice.recoverDevice()).andReturn(true);

        // and attempts to kill the process again
        EasyMock.expect(mockDevice.executeShellCommand(EasyMock.contains("ps"))).
                andReturn("root 1234 com.drawelement.deqp").once();
        EasyMock.expect(mockDevice.executeShellCommand(EasyMock.eq("kill -9 1234"))).
                andReturn("").once();

        // Recovery checks if kill failed
        mockSleepProvider.sleep(EasyMock.gt(0));
        EasyMock.expectLastCall().once();
        EasyMock.expect(mockDevice.executeShellCommand(EasyMock.contains("ps"))).
                andReturn("root 1234 com.drawelement.deqp").once();

        // recovery reboots the device
        mockDevice.reboot();
        EasyMock.expectLastCall().once();

        orderedControl.replay();
        recovery.setDevice(mockDevice);
        recovery.setSleepProvider(mockSleepProvider);
        recovery.recoverComLinkKilled();
        orderedControl.verify();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void setUp() throws Exception {
        super.setUp();
        mTestsDir = FileUtil.createTempDir("khronos-cts-runner-test-cases");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void tearDown() throws Exception {
        FileUtil.recursiveDelete(mTestsDir);
        super.tearDown();
    }
}
