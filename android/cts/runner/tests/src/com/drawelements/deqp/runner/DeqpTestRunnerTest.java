/*
 * Copyright (C) 2014 The Android Open Source Project
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
package com.drawelements.deqp.runner;

import com.android.compatibility.common.tradefed.build.CompatibilityBuildHelper;
import com.android.compatibility.common.tradefed.targetprep.IncrementalDeqpPreparer;
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
import com.android.tradefed.result.error.InfraErrorIdentifier;
import com.android.tradefed.testtype.Abi;
import com.android.tradefed.testtype.IAbi;
import com.android.tradefed.testtype.IRemoteTest;
import com.android.tradefed.testtype.IRuntimeHintProvider;
import com.android.tradefed.util.AbiUtils;
import com.android.tradefed.util.FileUtil;
import com.android.tradefed.util.IRunUtil;
import com.android.tradefed.util.RunInterruptedException;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import junit.framework.TestCase;
import org.easymock.EasyMock;
import org.easymock.IAnswer;
import org.easymock.IMocksControl;

/**
 * Unit tests for {@link DeqpTestRunner}.
 */
public class DeqpTestRunnerTest extends TestCase {
    private static final IAbi ABI = new Abi("armeabi-v7a", "32");
    private static final String APP_DIR = "/sdcard/";
    private static final String CASE_LIST_FILE_NAME = "dEQP-TestCaseList.txt";
    private static final String LOG_FILE_NAME = "TestLog.qpa";
    private static final String INSTRUMENTATION_NAME =
        "com.drawelements.deqp/com.drawelements.deqp.testercore.DeqpInstrumentation";
    private static final String QUERY_INSTRUMENTATION_NAME =
        "com.drawelements.deqp/com.drawelements.deqp.platformutil.DeqpPlatformCapabilityQueryInstrumentation";
    private static final String ONLY_LANDSCAPE_FEATURES =
        "feature:" + DeqpTestRunner.FEATURE_LANDSCAPE;
    private static final String ALL_FEATURES =
        ONLY_LANDSCAPE_FEATURES +
        "\nfeature:" + DeqpTestRunner.FEATURE_PORTRAIT;

    private static String ASSUMPTION_FAILURE_MESSAGE = "Assumption Failure";
    private static String PASS_MESSAGE = "Pass";

    private static final TestDescription[] SAMPLE_TEST_IDS = {
        new TestDescription("dEQP-GLES3.missing", "no"),
        new TestDescription("dEQP-GLES3.missing", "nope"),
        new TestDescription("dEQP-GLES3.missing", "donotwant"),
        new TestDescription("dEQP-GLES3.pick_me", "yes"),
        new TestDescription("dEQP-GLES3.pick_me", "ok"),
        new TestDescription("dEQP-GLES3.pick_me", "accepted"),
    };

    private static final TestDescription[] GLES3_INFO_TEST_IDS = {
        new TestDescription("dEQP-GLES3.info", "vendor"),
        new TestDescription("dEQP-GLES3.info", "renderer"),
        new TestDescription("dEQP-GLES3.info", "version"),
        new TestDescription("dEQP-GLES3.info", "shading_language_version"),
        new TestDescription("dEQP-GLES3.info", "extensions"),
        new TestDescription("dEQP-GLES3.info", "render_target"),
    };

    private File mTestsDir = null;
    private ITestDevice mockDevice;
    private IDevice mockIDevice;
    private ITestInvocationListener mockListener;

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

    /**
     * {@inheritDoc}
     */
    @Override
    protected void setUp() throws Exception {
        super.setUp();
        mTestsDir = FileUtil.createTempDir("deqp-test-cases");
        mockDevice = EasyMock.createMock(ITestDevice.class);
        mockIDevice = EasyMock.createMock(IDevice.class);
        mockListener = EasyMock.createStrictMock(ITestInvocationListener.class);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void tearDown() throws Exception {
        FileUtil.recursiveDelete(mTestsDir);
        super.tearDown();
    }

    private static DeqpTestRunner
    buildGlesTestRunner(int majorVersion, int minorVersion,
        Collection<TestDescription> tests, File testsDir)
        throws ConfigurationException, IOException {
        return buildGlesTestRunner(majorVersion, minorVersion, tests, testsDir, false,
                                   new ArrayList<TestDescription>());
    }

    private static DeqpTestRunner
    buildGlesTestRunner(int majorVersion, int minorVersion, Collection<TestDescription> tests,
                        File testsDir, boolean incrementalDeqpEnabled,
                        Collection<TestDescription> incrementalTests)
        throws ConfigurationException, IOException {

        StringWriter testlist = new StringWriter();
        for (TestDescription test : tests) {
            testlist.write(test.getClassName() + "." + test.getTestName() + "\n");
        }
        StringWriter incrementalTestlist = new StringWriter();
        for (TestDescription test : incrementalTests) {
            incrementalTestlist.write(test.getClassName() + "." + test.getTestName() + "\n");
        }
        return buildGlesTestRunner(majorVersion, minorVersion, testlist.toString(), testsDir,
                                   incrementalDeqpEnabled, incrementalTestlist.toString());
    }

    private static CompatibilityBuildHelper getMockBuildHelper(File testsDir) {
        IFolderBuildInfo mockIFolderBuildInfo =
            EasyMock.createMock(IFolderBuildInfo.class);
        EasyMock.expect(mockIFolderBuildInfo.getBuildAttributes())
            .andReturn(new HashMap<>())
            .anyTimes();
        EasyMock.replay(mockIFolderBuildInfo);
        return new BuildHelperMock(mockIFolderBuildInfo, testsDir);
    }

    private static DeqpTestRunner
    buildGlesTestRunner(int majorVersion, int minorVersion, String testlist, File testsDir,
                        boolean incrementalDeqpEnabled, String incrementalTestlist)
        throws ConfigurationException, IOException {

        DeqpTestRunner runner = new DeqpTestRunner();
        OptionSetter setter = new OptionSetter(runner);

        String deqpPackage =
            "dEQP-GLES" + majorVersion +
            (minorVersion > 0 ? Integer.toString(minorVersion) : "");

        final File caselistsFile = new File(testsDir, "gles3-caselist.txt");
        FileUtil.writeToFile(testlist, caselistsFile);

        if (!incrementalTestlist.isEmpty()) {
            final File incrementalCaselistsFile = new File(testsDir,
                "gles3-incremental-caselist.txt");
            FileUtil.writeToFile(incrementalTestlist, incrementalCaselistsFile);
            setter.setOptionValue(
                "incremental-deqp-include-file", incrementalCaselistsFile.getName());
        }

        setter.setOptionValue("deqp-package", deqpPackage);
        setter.setOptionValue("deqp-gl-config-name", "rgba8888d24s8");
        setter.setOptionValue("deqp-caselist-file", caselistsFile.getName());
        setter.setOptionValue("deqp-screen-rotation", "unspecified");
        setter.setOptionValue("deqp-surface-type", "window");
        setter.setOptionValue("enable-incremental-deqp", String.valueOf(incrementalDeqpEnabled));
        runner.setAbi(ABI);
        runner.setBuildHelper(getMockBuildHelper(testsDir));

        return runner;
    }

    private static String getTestId(DeqpTestRunner runner) {
        return AbiUtils.createId(ABI.getName(), runner.getPackageName());
    }

    private static String getCommandLine(String rotation) {
        return String.format(
            "--deqp-caselist-file=%s --deqp-gl-config-name=rgba8888d24s8 "
                + "--deqp-screen-rotation=%s "
                + "--deqp-surface-type=window "
                + "--deqp-log-images=disable "
                + "--deqp-watchdog=enable",
            APP_DIR + CASE_LIST_FILE_NAME, rotation);
    }

    private static String getCommandLine() {
        return getCommandLine("unspecified");
    }

    /**
     * Test version of OpenGL ES.
     */
    private void testGlesVersion(int requiredMajorVersion,
                                 int requiredMinorVersion, int majorVersion,
                                 int minorVersion, String resultCode)
        throws Exception {
        final TestDescription testId = new TestDescription(
            "dEQP-GLES" + Integer.toString(requiredMajorVersion) +
                Integer.toString(requiredMinorVersion) + ".info",
            "version");

        final String testTrie =
            "{dEQP-GLES" + Integer.toString(requiredMajorVersion) +
            Integer.toString(requiredMinorVersion) + "{info{version}}}";

        String resultDetails = resultCode;
        if (resultCode.equals(ASSUMPTION_FAILURE_MESSAGE)) {
            resultDetails =
                DeqpTestRunner.ASSUMPTION_FAILURE_DEQP_LEVEL_LOG_MESSAGE;
        }

        Collection<TestDescription> tests = new ArrayList<TestDescription>();
        tests.add(testId);

        final String output = buildTestProcessOutput(tests, resultCode, "Detail" + resultDetails);
        DeqpTestRunner deqpTest = buildGlesTestRunner(
            requiredMajorVersion, requiredMinorVersion, tests, mTestsDir);

        expectGlVersion(majorVersion, minorVersion);

        if (majorVersion > requiredMajorVersion ||
            (majorVersion == requiredMajorVersion &&
             minorVersion >= requiredMinorVersion)) {

            expectAngleSetupAndTeardown();

            expectRenderConfigQuery(requiredMajorVersion,
                                    requiredMinorVersion);

            String commandLine = getCommandLine();

            runInstrumentationLineAndAnswer(testTrie,
                                            commandLine, output);
        }

        expectTestRunStarted(deqpTest, 1);

        mockListener.testStarted(EasyMock.eq(testId));
        EasyMock.expectLastCall().once();

        if (resultCode.equals(ASSUMPTION_FAILURE_MESSAGE)) {
            mockListener.testAssumptionFailure(
                EasyMock.eq(testId),
                EasyMock.eq(
                    DeqpTestRunner.ASSUMPTION_FAILURE_DEQP_LEVEL_LOG_MESSAGE));
            EasyMock.expectLastCall().once();
        }

        mockListener.testEnded(EasyMock.eq(testId),
                               EasyMock.<HashMap<String, Metric>>notNull());
        EasyMock.expectLastCall().once();

        expectTestRunEnded();

        runAndVerifyTest(deqpTest);
    }



    /**
     * Test that result code produces correctly pass or fail.
     */
    private void testResultCode(final String resultCode, boolean pass)
        throws Exception {
        final TestDescription testId =
            new TestDescription("dEQP-GLES3.info", "version");
        final String testTrie = "{dEQP-GLES3{info{version}}}";


        Collection<TestDescription> tests = new ArrayList<TestDescription>();
        tests.add(testId);

        final String output = buildTestProcessOutput(tests, resultCode, "Detail" + resultCode);

        DeqpTestRunner deqpTest = buildGlesTestRunner(3, 0, tests, mTestsDir);

        expectGlVersion(3, 0);

        expectRenderConfigQuery(3, 0);

        String commandLine = getCommandLine();

        runInstrumentationLineAndAnswer(testTrie,
                                        commandLine, output);

        expectTestRunStarted(deqpTest, 1);

        expectAngleSetupAndTeardown();

        mockListener.testStarted(EasyMock.eq(testId));
        EasyMock.expectLastCall().once();

        if (!pass) {
            mockListener.testFailed(
                testId,
                "=== with config {glformat=rgba8888d24s8,rotation=unspecified,surfacetype=window,required=false} ===\n" +
                    resultCode + ": Detail" + resultCode);

            EasyMock.expectLastCall().once();
        }

        mockListener.testEnded(EasyMock.eq(testId),
                               EasyMock.<HashMap<String, Metric>>notNull());
        EasyMock.expectLastCall().once();

        expectTestRunEnded();

        runAndVerifyTest(deqpTest);
    }

    /**
     * Test running multiple test cases.
     */
    public void testRun_multipleTests() throws Exception {
        /* MultiLineReceiver expects "\r\n" line ending. */
        List<TestDescription> tests = Arrays.asList(GLES3_INFO_TEST_IDS);
        final String output = buildTestProcessOutput(tests);

        final String testTrie =
            "{dEQP-GLES3{info{vendor,renderer,version,shading_language_version,extensions,render_target}}}";

        DeqpTestRunner deqpTest = buildGlesTestRunner(3, 0, tests, mTestsDir);

        expectGlVersion(3, 0);

        expectRenderConfigQuery(3, 0);

        String commandLine = getCommandLine();

        runInstrumentationLineAndAnswer(testTrie,
                                        commandLine, output);

        expectRunAndVerifyTest(deqpTest, tests);
    }

    static private String buildTestProcessOutput(Collection<TestDescription> tests) {
        return buildTestProcessOutput(tests, "Pass", "Pass");
    }

    static private String buildTestProcessOutput(Collection<TestDescription> tests, String resultCode, String resultDetails) {
        /* MultiLineReceiver expects "\r\n" line ending. */
        final String outputHeader =
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
            + "INSTRUMENTATION_STATUS_CODE: 0\r\n";

        final String outputEnd =
            "INSTRUMENTATION_STATUS: dEQP-EventType=EndSession\r\n"
            + "INSTRUMENTATION_STATUS_CODE: 0\r\n"
            + "INSTRUMENTATION_CODE: 0\r\n";

        StringWriter output = new StringWriter();
        output.write(outputHeader);
        for (TestDescription test : tests) {
            output.write(
                "INSTRUMENTATION_STATUS: dEQP-EventType=BeginTestCase\r\n");
            output.write(
                "INSTRUMENTATION_STATUS: dEQP-BeginTestCase-TestCasePath=");
            output.write(test.getClassName());
            output.write(".");
            output.write(test.getTestName());
            output.write("\r\n");
            output.write("INSTRUMENTATION_STATUS_CODE: 0\r\n");
            output.write(
                "INSTRUMENTATION_STATUS: dEQP-TestCaseResult-Code=" + resultCode + "\r\n");
            output.write(
                "INSTRUMENTATION_STATUS: dEQP-TestCaseResult-Details=" + resultDetails + "\r\n");
            output.write(
                "INSTRUMENTATION_STATUS: dEQP-EventType=TestCaseResult\r\n");
            output.write("INSTRUMENTATION_STATUS_CODE: 0\r\n");
            output.write(
                "INSTRUMENTATION_STATUS: dEQP-EventType=EndTestCase\r\n");
            output.write("INSTRUMENTATION_STATUS_CODE: 0\r\n");
        }
        output.write(outputEnd);
        return output.toString();
    }

    private void testFiltering(DeqpTestRunner deqpTest, String expectedTrie,
                               List<TestDescription> expectedTests)
        throws Exception {
        expectGlVersion(3, 0);

        boolean thereAreTests = !expectedTests.isEmpty();

        if (thereAreTests) {
            expectRenderConfigQuery(3, 0);
            String testOut = buildTestProcessOutput(expectedTests);
            runInstrumentationLineAndAnswer(testOut);
        }

        expectRunAndVerifyTest(deqpTest, expectedTests);
    }

    public void testRun_trivialIncludeFilter() throws Exception {
        List<TestDescription> allTests = Arrays.asList(SAMPLE_TEST_IDS);
        List<TestDescription> activeTests = Arrays.asList(
            SAMPLE_TEST_IDS[3], SAMPLE_TEST_IDS[4], SAMPLE_TEST_IDS[5]);

        String expectedTrie = "{dEQP-GLES3{pick_me{yes,ok,accepted}}}";

        DeqpTestRunner deqpTest =
            buildGlesTestRunner(3, 0, allTests, mTestsDir);
        deqpTest.addIncludeFilter("dEQP-GLES3.pick_me#*");
        testFiltering(deqpTest, expectedTrie, activeTests);
    }

    public void testRun_trivialExcludeFilter() throws Exception {
        List<TestDescription> allTests = Arrays.asList(SAMPLE_TEST_IDS);
        List<TestDescription> activeTests = Arrays.asList(
            SAMPLE_TEST_IDS[3], SAMPLE_TEST_IDS[4], SAMPLE_TEST_IDS[5]);

        String expectedTrie = "{dEQP-GLES3{pick_me{yes,ok,accepted}}}";

        DeqpTestRunner deqpTest =
            buildGlesTestRunner(3, 0, allTests, mTestsDir);
        deqpTest.addExcludeFilter("dEQP-GLES3.missing#*");
        testFiltering(deqpTest, expectedTrie, activeTests);
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

        List<TestDescription> allTests = Arrays.asList(testIds);

        List<TestDescription> activeTests = new ArrayList<TestDescription>();
        activeTests.add(testIds[4]);

        String expectedTrie = "{dEQP-GLES3{group2{yes}}}";

        DeqpTestRunner deqpTest =
            buildGlesTestRunner(3, 0, allTests, mTestsDir);

        Set<String> includes = new HashSet<>();
        includes.add("dEQP-GLES3.group2#*");
        deqpTest.addAllIncludeFilters(includes);

        Set<String> excludes = new HashSet<>();
        excludes.add("*foo");
        excludes.add("*thoushallnotpass");
        deqpTest.addAllExcludeFilters(excludes);
        testFiltering(deqpTest, expectedTrie, activeTests);
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

        List<TestDescription> allTests = Arrays.asList(testIds);

        String expectedTrie =
            "{dEQP-GLES3{group1{mememe,yeah,takeitall},group2{jeba,yes,granted}}}";

        DeqpTestRunner deqpTest =
            buildGlesTestRunner(3, 0, allTests, mTestsDir);
        deqpTest.addIncludeFilter("*");
        testFiltering(deqpTest, expectedTrie, allTests);
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

        List<TestDescription> allTests = Arrays.asList(testIds);

        DeqpTestRunner deqpTest =
            buildGlesTestRunner(3, 0, allTests, mTestsDir);
        deqpTest.addExcludeFilter("*");
        expectTestRunStarted(deqpTest, 0);
        expectTestRunEnded();

        runAndVerifyTest(deqpTest);
    }

    public void testRun_incrementalDeqpAttributeSet() throws Exception {
        final TestDescription[] testIds = {
            new TestDescription("dEQP-GLES3.group1", "no"),
            new TestDescription("dEQP-GLES3.group1", "nope"),
            new TestDescription("dEQP-GLES3.group1", "nottoday"),
            new TestDescription("dEQP-GLES3.group2", "banned"),
            new TestDescription("dEQP-GLES3.group2", "notrecognized"),
            new TestDescription("dEQP-GLES3.group2", "-2"),
        };

        List<TestDescription> allTests = Arrays.asList(testIds);

        DeqpTestRunner deqpTest =
            buildGlesTestRunner(3, 0, allTests, mTestsDir);

        HashMap attributes = new HashMap<>();
        attributes.put(IncrementalDeqpPreparer.INCREMENTAL_DEQP_ATTRIBUTE_NAME,
            "");
        IFolderBuildInfo mockBuildInfo =
            EasyMock.createMock(IFolderBuildInfo.class);
        EasyMock.expect(mockBuildInfo.getBuildAttributes())
            .andReturn(attributes)
            .atLeastOnce();
        CompatibilityBuildHelper helper =
            new BuildHelperMock(mockBuildInfo, mTestsDir);
        deqpTest.setBuildHelper(helper);
        EasyMock.replay(mockBuildInfo);

        expectTestRunStarted(deqpTest, 0);
        expectTestRunEnded();

        runAndVerifyTest(deqpTest);
    }

    public void testRun_incrementalDeqpEnabled() throws Exception {
        final TestDescription[] testIds = {
            new TestDescription("dEQP-GLES3.non-incremental-deqp",
                "should-skip-1"),
            new TestDescription("dEQP-GLES3.non-incremental-deqp",
                "should-skip-2"),
            new TestDescription("dEQP-GLES3.non-incremental-deqp",
                "should-skip-3"),
            new TestDescription("dEQP-GLES3.incremental-deqp", "should-run-1"),
            new TestDescription("dEQP-GLES3.incremental-deqp", "should-run-2"),
            new TestDescription("dEQP-GLES3.incremental-deqp", "should-run-3"),
        };

        List<TestDescription> allTests = new ArrayList<>();
        Collections.addAll(allTests, testIds);

        List<TestDescription> incrementalTests = new ArrayList<>();
        incrementalTests.add(testIds[3]);
        incrementalTests.add(testIds[4]);
        incrementalTests.add(testIds[5]);

        final String output = buildTestProcessOutput(incrementalTests);
        DeqpTestRunner deqpTest =
            buildGlesTestRunner(3, 0, allTests, mTestsDir, true, incrementalTests);

        String testTrie =
            "{dEQP-GLES3{incremental-deqp{should-run-1,should-run-2,should-run-3}}}";

        expectGlVersion(3, 0);

        expectRenderConfigQuery(3, 0);

        String commandLine = getCommandLine();

        runInstrumentationLineAndAnswer(testTrie,
            commandLine, output);

        expectRunAndVerifyTest(deqpTest, incrementalTests);
    }

    /**
     * Test running a unexecutable test.
     */
    public void testRun_unexecutableTests() throws Exception {
        final String instrumentationAnswerNoExecs = buildTestProcessOutput(Collections.emptyList());

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


        List<TestDescription> tests = Arrays.asList(testIds);

        DeqpTestRunner deqpTest = buildGlesTestRunner(3, 0, tests, mTestsDir);

        expectGlVersion(3, 0);

        expectRenderConfigQuery(3, 0);

        String commandLine = getCommandLine();

        // first try
        runInstrumentationLineAndAnswer(
            "{dEQP-GLES3{missing{no,nope,donotwant}}}",
            commandLine, instrumentationAnswerNoExecs);

        // splitting begins
        runInstrumentationLineAndAnswer(
            "{dEQP-GLES3{missing{no}}}", commandLine,
            instrumentationAnswerNoExecs);
        runInstrumentationLineAndAnswer(
            "{dEQP-GLES3{missing{nope,donotwant}}}",
            commandLine, instrumentationAnswerNoExecs);
        runInstrumentationLineAndAnswer(
            "{dEQP-GLES3{missing{nope}}}", commandLine,
            instrumentationAnswerNoExecs);
        runInstrumentationLineAndAnswer(
            "{dEQP-GLES3{missing{donotwant}}}",
            commandLine, instrumentationAnswerNoExecs);

        expectTestRunStarted(deqpTest, testPaths.length);

        expectAngleSetupAndTeardown();

        for (int i = 0; i < testPaths.length; i++) {
            mockListener.testStarted(EasyMock.eq(testIds[i]));
            EasyMock.expectLastCall().once();

            mockListener.testFailed(
                EasyMock.eq(testIds[i]),
                EasyMock.eq(
                    "=== with config {glformat=rgba8888d24s8,rotation=unspecified,surfacetype=window,required=false} ===\n"
                    + "Abort: Test cannot be executed"));
            EasyMock.expectLastCall().once();

            mockListener.testEnded(EasyMock.eq(testIds[i]),
                                   EasyMock.<HashMap<String, Metric>>notNull());
            EasyMock.expectLastCall().once();
        }

        expectTestRunEnded();

        runAndVerifyTest(deqpTest);
    }

    /**
     * Test that test are left unexecuted if pm list query fails
     */
    public void testRun_queryPmListFailure() throws Exception {
        final TestDescription testId =
            new TestDescription("dEQP-GLES3.orientation", "test");

        Collection<TestDescription> tests = new ArrayList<TestDescription>();
        tests.add(testId);

        DeqpTestRunner deqpTest = buildGlesTestRunner(3, 0, tests, mTestsDir);
        OptionSetter setter = new OptionSetter(deqpTest);
        // Note: If the rotation is the default unspecified, features are not
        // queried at all
        setter.setOptionValue("deqp-screen-rotation", "90");

        expectGlVersion(3, 0);

        EasyMock.expect(mockDevice.executeShellCommand("pm list features"))
            .andReturn("not a valid format");

        expectTestRunStarted(deqpTest, 1);
        expectAngleSetup();

        expectTestRunEnded();

        runAndVerifyTest(deqpTest);
    }

    /**
     * Test that test are left unexecuted if renderablity query fails
     */
    public void testRun_queryRenderabilityFailure() throws Exception {
        final TestDescription testId =
            new TestDescription("dEQP-GLES3.orientation", "test");


        Collection<TestDescription> tests = new ArrayList<TestDescription>();
        tests.add(testId);

        DeqpTestRunner deqpTest = buildGlesTestRunner(3, 0, tests, mTestsDir);

        expectGlVersion(3, 0);

        expectRenderConfigQueryAndReturn(
            "--deqp-gl-config-name=rgba8888d24s8 "
                + "--deqp-screen-rotation=unspecified "
                + "--deqp-surface-type=window "
                + "--deqp-gl-major-version=3 "
                + "--deqp-gl-minor-version=0",
            "Maybe?");

        expectTestRunStarted(deqpTest, 1);

        expectAngleSetup();
        expectTestRunEnded();

        runAndVerifyTest(deqpTest);
    }

    /**
     * Test that orientation is supplied to runner correctly
     */
    private void testOrientation(final String rotation,
                                 final String featureString) throws Exception {
        final TestDescription testId =
            new TestDescription("dEQP-GLES3.orientation", "test");
        final String testTrie = "{dEQP-GLES3{orientation{test}}}";


        Collection<TestDescription> tests = new ArrayList<TestDescription>();
        tests.add(testId);

        final String output = buildTestProcessOutput(tests);
        DeqpTestRunner deqpTest = buildGlesTestRunner(3, 0, tests, mTestsDir);
        OptionSetter setter = new OptionSetter(deqpTest);
        setter.setOptionValue("deqp-screen-rotation", rotation);

        expectGlVersion(3, 0);

        if (!rotation.equals(BatchRunConfiguration.ROTATION_UNSPECIFIED)) {
            EasyMock.expect(mockDevice.executeShellCommand("pm list features"))
                .andReturn(featureString);
        }

        final boolean isPortraitOrientation =
            rotation.equals(BatchRunConfiguration.ROTATION_PORTRAIT) ||
            rotation.equals(BatchRunConfiguration.ROTATION_REVERSE_PORTRAIT);
        final boolean isLandscapeOrientation =
            rotation.equals(BatchRunConfiguration.ROTATION_LANDSCAPE) ||
            rotation.equals(BatchRunConfiguration.ROTATION_REVERSE_LANDSCAPE);
        final boolean executable =
            rotation.equals(BatchRunConfiguration.ROTATION_UNSPECIFIED) ||
            (isPortraitOrientation &&
             featureString.contains(DeqpTestRunner.FEATURE_PORTRAIT)) ||
            (isLandscapeOrientation &&
             featureString.contains(DeqpTestRunner.FEATURE_LANDSCAPE));

        if (executable) {
            expectRenderConfigQuery(
                String.format(
                    "--deqp-gl-config-name=rgba8888d24s8 --deqp-screen-rotation=%s "
                        +
                        "--deqp-surface-type=window --deqp-gl-major-version=3 "
                        + "--deqp-gl-minor-version=0",
                    rotation));

            String commandLine = getCommandLine(rotation);

            runInstrumentationLineAndAnswer(testTrie,
                                            commandLine, output);
        }

        expectRunAndVerifyTest(deqpTest, Collections.singletonList(testId));
    }

    /**
     * Test OpeGL ES3 tests on device with OpenGL ES2.
     */
    public void testRun_require30DeviceVersion20() throws Exception {
        testGlesVersion(3, 0, 2, 0, ASSUMPTION_FAILURE_MESSAGE);
    }

    /**
     * Test OpeGL ES3.1 tests on device with OpenGL ES2.
     */
    public void testRun_require31DeviceVersion20() throws Exception {
        testGlesVersion(3, 1, 2, 0, ASSUMPTION_FAILURE_MESSAGE);
    }

    /**
     * Test OpeGL ES3 tests on device with OpenGL ES3.
     */
    public void testRun_require30DeviceVersion30() throws Exception {
        testGlesVersion(3, 0, 3, 0, PASS_MESSAGE);
    }

    /**
     * Test OpeGL ES3.1 tests on device with OpenGL ES3.
     */
    public void testRun_require31DeviceVersion30() throws Exception {
        testGlesVersion(3, 1, 3, 0, ASSUMPTION_FAILURE_MESSAGE);
    }

    /**
     * Test OpeGL ES3 tests on device with OpenGL ES3.1.
     */
    public void testRun_require30DeviceVersion31() throws Exception {
        testGlesVersion(3, 0, 3, 1, PASS_MESSAGE);
    }

    /**
     * Test OpeGL ES3.1 tests on device with OpenGL ES3.1.
     */
    public void testRun_require31DeviceVersion31() throws Exception {
        testGlesVersion(3, 1, 3, 1, PASS_MESSAGE);
    }

    /**
     * Test dEQP Pass result code.
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
     * Test dEQP Orientation
     */
    public void testRun_orientationLandscape() throws Exception {
        testOrientation("90", ALL_FEATURES);
    }

    /**
     * Test dEQP Orientation
     */
    public void testRun_orientationPortrait() throws Exception {
        testOrientation("0", ALL_FEATURES);
    }

    /**
     * Test dEQP Orientation
     */
    public void testRun_orientationReverseLandscape() throws Exception {
        testOrientation("270", ALL_FEATURES);
    }

    /**
     * Test dEQP Orientation
     */
    public void testRun_orientationReversePortrait() throws Exception {
        testOrientation("180", ALL_FEATURES);
    }

    /**
     * Test dEQP Orientation
     */
    public void testRun_orientationUnspecified() throws Exception {
        testOrientation("unspecified", ALL_FEATURES);
    }

    /**
     * Test dEQP Orientation with limited features
     */
    public void testRun_orientationUnspecifiedLimitedFeatures()
        throws Exception {
        testOrientation("unspecified", ONLY_LANDSCAPE_FEATURES);
    }

    /**
     * Test dEQP Orientation with limited features
     */
    public void testRun_orientationLandscapeLimitedFeatures() throws Exception {
        testOrientation("90", ONLY_LANDSCAPE_FEATURES);
    }

    /**
     * Test dEQP Orientation with limited features
     */
    public void testRun_orientationPortraitLimitedFeatures() throws Exception {
        testOrientation("0", ONLY_LANDSCAPE_FEATURES);
    }

    /**
     * Test dEQP unsupported pixel format
     */
    public void testRun_unsupportedPixelFormat() throws Exception {
        final String pixelFormat = "rgba5658d16m4";
        final TestDescription testId =
            new TestDescription("dEQP-GLES3.pixelformat", "test");


        Collection<TestDescription> tests = new ArrayList<TestDescription>();
        tests.add(testId);

        DeqpTestRunner deqpTest = buildGlesTestRunner(3, 0, tests, mTestsDir);
        OptionSetter setter = new OptionSetter(deqpTest);
        setter.setOptionValue("deqp-gl-config-name", pixelFormat);

        expectGlVersion(3, 0);

        expectRenderConfigQueryAndReturn(
            String.format(
                "--deqp-gl-config-name=%s --deqp-screen-rotation=unspecified "
                    + "--deqp-surface-type=window "
                    + "--deqp-gl-major-version=3 "
                    + "--deqp-gl-minor-version=0",
                pixelFormat),
            "No");

        expectRunAndVerifyTest(deqpTest, Collections.singletonList(testId));
    }

    /**
     * Test interface to mock Tradefed device types.
     */
    public static interface RecoverableTestDevice
        extends ITestDevice, IManagedTestDevice {}

    private static enum RecoveryEvent {
        PROGRESS,
        FAIL_CONNECTION_REFUSED,
        FAIL_LINK_KILLED,
    }

    private void runRecoveryWithPattern(DeqpTestRunner.Recovery recovery,
                                        RecoveryEvent[] events)
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

    private void setRecoveryExpectationWait(
        DeqpTestRunner.ISleepProvider mockSleepProvider) {
        mockSleepProvider.sleep(EasyMock.gt(0));
        EasyMock.expectLastCall().once();
    }

    private void setRecoveryExpectationKillProcess(
        DeqpTestRunner.ISleepProvider mockSleepProvider)
        throws DeviceNotAvailableException {
        expectShellCommandContains("ps", "root 1234 com.drawelement.deqp");
        expectShellCommand("kill -9 1234", "");

        // Recovery checks if kill failed
        mockSleepProvider.sleep(EasyMock.gt(0));
        EasyMock.expectLastCall().once();
        expectShellCommandContains("ps", "");
    }

    private void
    setRecoveryExpectationRecovery()
        throws DeviceNotAvailableException {
        EasyMock.expect(((RecoverableTestDevice) mockDevice).recoverDevice()).andReturn(true).once();
    }

    private void setRecoveryExpectationReboot()
        throws DeviceNotAvailableException {
        mockDevice.reboot();
        EasyMock.expectLastCall().once();
    }

    private int setRecoveryExpectationOfAConnFailure(
        DeqpTestRunner.ISleepProvider mockSleepProvider,
        int numConsecutiveErrors) throws DeviceNotAvailableException {
        switch (numConsecutiveErrors) {
        case 0:
        case 1:
            setRecoveryExpectationRecovery();
            return 2;
        case 2:
            setRecoveryExpectationReboot();
            return 3;
        default:
            return 4;
        }
    }

    private int setRecoveryExpectationOfAComKilled(
        DeqpTestRunner.ISleepProvider mockSleepProvider,
        int numConsecutiveErrors) throws DeviceNotAvailableException {
        switch (numConsecutiveErrors) {
        case 0:
            setRecoveryExpectationWait(mockSleepProvider);
            setRecoveryExpectationKillProcess(mockSleepProvider);
            return 1;
        case 1:
            setRecoveryExpectationRecovery();
            setRecoveryExpectationKillProcess(mockSleepProvider);
            return 2;
        case 2:
            setRecoveryExpectationReboot();
            return 3;
        default:
            return 4;
        }
    }

    private void setRecoveryExpectationsOfAPattern(
        DeqpTestRunner.ISleepProvider mockSleepProvider, RecoveryEvent[] events)
        throws DeviceNotAvailableException {
        int numConsecutiveErrors = 0;
        for (RecoveryEvent event : events) {
            switch (event) {
            case PROGRESS:
                numConsecutiveErrors = 0;
                break;
            case FAIL_CONNECTION_REFUSED:
                numConsecutiveErrors = setRecoveryExpectationOfAConnFailure(
                    mockSleepProvider, numConsecutiveErrors);
                break;
            case FAIL_LINK_KILLED:
                numConsecutiveErrors = setRecoveryExpectationOfAComKilled(
                    mockSleepProvider, numConsecutiveErrors);
                break;
            }
        }
    }

    /**
     * Test dEQP runner recovery state machine.
     */
    private void testRecoveryWithPattern(boolean expectSuccess,
                                         RecoveryEvent... pattern)
        throws Exception {
        DeqpTestRunner.Recovery recovery = new DeqpTestRunner.Recovery();
        IMocksControl orderedControl = EasyMock.createStrictControl();
        mockDevice = orderedControl.createMock(RecoverableTestDevice.class);
        EasyMock.expect(mockDevice.getSerialNumber()).andStubReturn("SERIAL");
        DeqpTestRunner.ISleepProvider mockSleepProvider =
            orderedControl.createMock(DeqpTestRunner.ISleepProvider.class);

        setRecoveryExpectationsOfAPattern(mockSleepProvider,
                                          pattern);

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

    // basic patterns

    public void testRecovery_NoEvents() throws Exception {
        testRecoveryWithPattern(true);
    }

    public void testRecovery_AllOk() throws Exception {
        testRecoveryWithPattern(true, RecoveryEvent.PROGRESS,
                                RecoveryEvent.PROGRESS);
    }

    // conn fail patterns

    public void testRecovery_OneConnectionFailureBegin() throws Exception {
        testRecoveryWithPattern(true, RecoveryEvent.FAIL_CONNECTION_REFUSED,
                                RecoveryEvent.PROGRESS);
    }

    public void testRecovery_TwoConnectionFailuresBegin() throws Exception {
        testRecoveryWithPattern(true, RecoveryEvent.FAIL_CONNECTION_REFUSED,
                                RecoveryEvent.FAIL_CONNECTION_REFUSED,
                                RecoveryEvent.PROGRESS);
    }

    public void testRecovery_ThreeConnectionFailuresBegin() throws Exception {
        testRecoveryWithPattern(false, RecoveryEvent.FAIL_CONNECTION_REFUSED,
                                RecoveryEvent.FAIL_CONNECTION_REFUSED,
                                RecoveryEvent.FAIL_CONNECTION_REFUSED);
    }

    public void testRecovery_OneConnectionFailureMid() throws Exception {
        testRecoveryWithPattern(true, RecoveryEvent.PROGRESS,
                                RecoveryEvent.FAIL_CONNECTION_REFUSED,
                                RecoveryEvent.PROGRESS);
    }

    public void testRecovery_TwoConnectionFailuresMid() throws Exception {
        testRecoveryWithPattern(
            true, RecoveryEvent.PROGRESS, RecoveryEvent.FAIL_CONNECTION_REFUSED,
            RecoveryEvent.FAIL_CONNECTION_REFUSED, RecoveryEvent.PROGRESS);
    }

    public void testRecovery_ThreeConnectionFailuresMid() throws Exception {
        testRecoveryWithPattern(false, RecoveryEvent.PROGRESS,
                                RecoveryEvent.FAIL_CONNECTION_REFUSED,
                                RecoveryEvent.FAIL_CONNECTION_REFUSED,
                                RecoveryEvent.FAIL_CONNECTION_REFUSED);
    }

    // link fail patterns

    public void testRecovery_OneLinkFailureBegin() throws Exception {
        testRecoveryWithPattern(true, RecoveryEvent.FAIL_LINK_KILLED,
                                RecoveryEvent.PROGRESS);
    }

    public void testRecovery_TwoLinkFailuresBegin() throws Exception {
        testRecoveryWithPattern(true, RecoveryEvent.FAIL_LINK_KILLED,
                                RecoveryEvent.FAIL_LINK_KILLED,
                                RecoveryEvent.PROGRESS);
    }

    public void testRecovery_ThreeLinkFailuresBegin() throws Exception {
        testRecoveryWithPattern(true, RecoveryEvent.FAIL_LINK_KILLED,
                                RecoveryEvent.FAIL_LINK_KILLED,
                                RecoveryEvent.FAIL_LINK_KILLED,
                                RecoveryEvent.PROGRESS);
    }

    public void testRecovery_FourLinkFailuresBegin() throws Exception {
        testRecoveryWithPattern(false, RecoveryEvent.FAIL_LINK_KILLED,
                                RecoveryEvent.FAIL_LINK_KILLED,
                                RecoveryEvent.FAIL_LINK_KILLED,
                                RecoveryEvent.FAIL_LINK_KILLED);
    }

    public void testRecovery_OneLinkFailureMid() throws Exception {
        testRecoveryWithPattern(true, RecoveryEvent.PROGRESS,
                                RecoveryEvent.FAIL_LINK_KILLED,
                                RecoveryEvent.PROGRESS);
    }

    public void testRecovery_TwoLinkFailuresMid() throws Exception {
        testRecoveryWithPattern(
            true, RecoveryEvent.PROGRESS, RecoveryEvent.FAIL_LINK_KILLED,
            RecoveryEvent.FAIL_LINK_KILLED, RecoveryEvent.PROGRESS);
    }

    public void testRecovery_ThreeLinkFailuresMid() throws Exception {
        testRecoveryWithPattern(
            true, RecoveryEvent.PROGRESS, RecoveryEvent.FAIL_LINK_KILLED,
            RecoveryEvent.FAIL_LINK_KILLED, RecoveryEvent.FAIL_LINK_KILLED,
            RecoveryEvent.PROGRESS);
    }

    public void testRecovery_FourLinkFailuresMid() throws Exception {
        testRecoveryWithPattern(
            false, RecoveryEvent.PROGRESS, RecoveryEvent.FAIL_LINK_KILLED,
            RecoveryEvent.FAIL_LINK_KILLED, RecoveryEvent.FAIL_LINK_KILLED,
            RecoveryEvent.FAIL_LINK_KILLED);
    }

    // mixed patterns

    public void testRecovery_MixedFailuresProgressBetween() throws Exception {
        testRecoveryWithPattern(
            true, RecoveryEvent.PROGRESS, RecoveryEvent.FAIL_LINK_KILLED,
            RecoveryEvent.PROGRESS, RecoveryEvent.FAIL_CONNECTION_REFUSED,
            RecoveryEvent.PROGRESS, RecoveryEvent.FAIL_LINK_KILLED,
            RecoveryEvent.PROGRESS, RecoveryEvent.FAIL_CONNECTION_REFUSED,
            RecoveryEvent.PROGRESS);
    }

    public void testRecovery_MixedFailuresNoProgressBetween() throws Exception {
        testRecoveryWithPattern(
            true, RecoveryEvent.PROGRESS, RecoveryEvent.FAIL_LINK_KILLED,
            RecoveryEvent.FAIL_CONNECTION_REFUSED,
            RecoveryEvent.FAIL_LINK_KILLED, RecoveryEvent.PROGRESS);
    }

    /**
     * Test recovery if process cannot be killed
     */
    public void testRecovery_unkillableProcess() throws Exception {
        DeqpTestRunner.Recovery recovery = new DeqpTestRunner.Recovery();
        IMocksControl orderedControl = EasyMock.createStrictControl();
        mockDevice = orderedControl.createMock(RecoverableTestDevice.class);
        DeqpTestRunner.ISleepProvider mockSleepProvider =
            orderedControl.createMock(DeqpTestRunner.ISleepProvider.class);

        // recovery attempts to kill the process after a timeout
        mockSleepProvider.sleep(EasyMock.gt(0));
        expectShellCommandContains("ps", "root 1234 com.drawelement.deqp");
        expectShellCommand("kill -9 1234", "");

        // Recovery checks if kill failed
        mockSleepProvider.sleep(EasyMock.gt(0));
        EasyMock.expectLastCall().once();
        expectShellCommandContains("ps", "root 1234 com.drawelement.deqp");

        // Recovery resets the connection
        EasyMock.expect(((RecoverableTestDevice) mockDevice).recoverDevice()).andReturn(true);

        // and attempts to kill the process again
        expectShellCommandContains("ps", "root 1234 com.drawelement.deqp");
        expectShellCommand("kill -9 1234", "");

        // Recovery checks if kill failed
        mockSleepProvider.sleep(EasyMock.gt(0));
        EasyMock.expectLastCall().once();
        expectShellCommandContains("ps", "root 1234 com.drawelement.deqp");

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
     * Test external interruption before batch run.
     */
    public void testInterrupt_killBeforeBatch() throws Exception {
        final TestDescription testId =
            new TestDescription("dEQP-GLES3.interrupt", "test");

        Collection<TestDescription> tests = new ArrayList<TestDescription>();
        tests.add(testId);

        IRunUtil mockRunUtil = EasyMock.createMock(IRunUtil.class);

        DeqpTestRunner deqpTest = buildGlesTestRunner(3, 0, tests, mTestsDir);

        deqpTest.setDevice(mockDevice);
        deqpTest.setRunUtil(mockRunUtil);

        expectGlVersion(3, 0);

        expectRenderConfigQuery(
            "--deqp-gl-config-name=rgba8888d24s8 --deqp-screen-rotation=unspecified "
                + "--deqp-surface-type=window --deqp-gl-major-version=3 "
                + "--deqp-gl-minor-version=0");

        mockRunUtil.sleep(0);
        EasyMock.expectLastCall().andThrow(new RunInterruptedException(
            "message", InfraErrorIdentifier.TRADEFED_SHUTTING_DOWN));

        expectTestRunStarted(deqpTest, 1);
        expectAngleSetup();
        expectTestRunEnded();

        runAndVerifyTestInterrupted(deqpTest, mockRunUtil);
    }

    private void
    runShardedTest(TestDescription[] testIds,
                   ArrayList<ArrayList<TestDescription>> testsForShard)
        throws Exception {
        List<TestDescription> tests = Arrays.asList(testIds);
        DeqpTestRunner runner = buildGlesTestRunner(3, 0, tests, mTestsDir);
        ArrayList<IRemoteTest> shards = (ArrayList<IRemoteTest>)runner.split();

        for (int shardIndex = 0; shardIndex < shards.size(); shardIndex++) {
            EasyMock.reset(mockDevice, mockIDevice, mockListener);
            DeqpTestRunner shard = (DeqpTestRunner)shards.get(shardIndex);
            shard.setBuildHelper(getMockBuildHelper(mTestsDir));

            ArrayList<TestDescription> shardTests =
                testsForShard.get(shardIndex);

            expectGlVersion(3, 0);
            expectRenderConfigQuery(3, 0);

            String testOut = buildTestProcessOutput(shardTests);
            // NOTE: This assumes that there won't be multiple batches per
            // shard!
            runInstrumentationLineAndAnswer(testOut);

            expectRunAndVerifyTest(shard, shardTests);
        }
    }

    public void testSharding_smallTrivial() throws Exception {
        ArrayList<ArrayList<TestDescription>> shardedTests = new ArrayList<>();
        ArrayList<TestDescription> shardOne = new ArrayList<>(Arrays.asList(GLES3_INFO_TEST_IDS));
        shardedTests.add(shardOne);
        runShardedTest(GLES3_INFO_TEST_IDS, shardedTests);
    }

    public void testSharding_twoShards() throws Exception {
        final int TEST_COUNT = 1237;
        final int SHARD_SIZE = 1000;

        ArrayList<TestDescription> testIds = new ArrayList<>(TEST_COUNT);
        for (int i = 0; i < TEST_COUNT; i++) {
            testIds.add(new TestDescription("dEQP-GLES3.funny.group",
                                            String.valueOf(i)));
        }

        ArrayList<ArrayList<TestDescription>> shardedTests = new ArrayList<>();
        ArrayList<TestDescription> shard = new ArrayList<>();
        for (int i = 0; i < testIds.size(); i++) {
            if (i == SHARD_SIZE) {
                shardedTests.add(shard);
                shard = new ArrayList<>();
            }
            shard.add(testIds.get(i));
        }
        shardedTests.add(shard);
        runShardedTest(testIds.toArray(new TestDescription[testIds.size()]),
                       shardedTests);
    }

    public void testSharding_empty() throws Exception {
        DeqpTestRunner runner = buildGlesTestRunner(
            3, 0, new ArrayList<TestDescription>(), mTestsDir);
        ArrayList<IRemoteTest> shards = (ArrayList<IRemoteTest>)runner.split();
        // Returns null when cannot be sharded.
        assertNull(shards);
    }

    /**
     * Test external interruption in testFailed().
     */
    public void testInterrupt_killReportTestFailed() throws Exception {
        final TestDescription testId =
            new TestDescription("dEQP-GLES3.interrupt", "test");
        final String testTrie = "{dEQP-GLES3{interrupt{test}}}";

        Collection<TestDescription> tests = new ArrayList<TestDescription>();
        tests.add(testId);

        final String output = buildTestProcessOutput(tests, "Fail", "Fail");

        IRunUtil mockRunUtil = EasyMock.createMock(IRunUtil.class);

        DeqpTestRunner deqpTest = buildGlesTestRunner(3, 0, tests, mTestsDir);

        deqpTest.setDevice(mockDevice);
        deqpTest.setRunUtil(mockRunUtil);

        expectGlVersion(3, 0);

        expectRenderConfigQuery(
            "--deqp-gl-config-name=rgba8888d24s8 --deqp-screen-rotation=unspecified "
                + "--deqp-surface-type=window --deqp-gl-major-version=3 "
                + "--deqp-gl-minor-version=0");

        mockRunUtil.sleep(0);
        EasyMock.expectLastCall().once();

        String commandLine = getCommandLine();

        runInstrumentationLineAndAnswer(testTrie,
                                        commandLine, output);

        expectTestRunStarted(deqpTest, 1);

        expectAngleSetup();

        mockListener.testStarted(EasyMock.eq(testId));
        EasyMock.expectLastCall().once();

        mockListener.testFailed(EasyMock.eq(testId),
                                EasyMock.<String>notNull());
        EasyMock.expectLastCall().andThrow(new RunInterruptedException(
            "message", InfraErrorIdentifier.TRADEFED_SHUTTING_DOWN));

        expectTestRunEnded();
        runAndVerifyTestInterrupted(deqpTest, mockRunUtil);
    }

    public void testRuntimeHint_optionSet() throws Exception {
        final String testTrie =
            "{dEQP-GLES3{info{vendor,renderer,version,shading_language_version,extensions,render_target}}}";

        List<TestDescription> tests = Arrays.asList(GLES3_INFO_TEST_IDS);

        final String output = buildTestProcessOutput(tests);
        DeqpTestRunner deqpTest = buildGlesTestRunner(3, 0, tests, mTestsDir);
        OptionSetter setter = new OptionSetter(deqpTest);
        final long runtimeMs = 123456;
        setter.setOptionValue("runtime-hint", String.valueOf(runtimeMs));
        assertEquals("Wrong expected runtime - option not passed cleanly",
                     runtimeMs, deqpTest.getRuntimeHint());

        // Try running the tests as well. The unit tests do not set the hint be
        // default, so that case is covered.

        expectGlVersion(3, 0);

        expectRenderConfigQuery(3, 0);

        String commandLine = getCommandLine();

        runInstrumentationLineAndAnswer(testTrie,
                                        commandLine, output);

        expectRunAndVerifyTest(deqpTest, tests);
    }

    public void testRuntimeHint_optionSetSharded() throws Exception {
        final int TEST_COUNT = 1237;
        final int SHARD_SIZE = 1000;

        ArrayList<TestDescription> testIds = new ArrayList<>(TEST_COUNT);
        for (int i = 0; i < TEST_COUNT; i++) {
            testIds.add(new TestDescription("dEQP-GLES3.funny.group",
                                            String.valueOf(i)));
        }

        DeqpTestRunner deqpTest = buildGlesTestRunner(3, 0, testIds, mTestsDir);
        OptionSetter setter = new OptionSetter(deqpTest);
        final long fullRuntimeMs = testIds.size() * 100;
        setter.setOptionValue("runtime-hint", String.valueOf(fullRuntimeMs));

        ArrayList<IRemoteTest> shards =
            (ArrayList<IRemoteTest>)deqpTest.split();
        assertEquals("First shard's time not proportional to test count",
                     (fullRuntimeMs * SHARD_SIZE) / TEST_COUNT,
                     ((IRuntimeHintProvider)shards.get(0)).getRuntimeHint());
        assertEquals("Second shard's time not proportional to test count",
                     (fullRuntimeMs * (TEST_COUNT - SHARD_SIZE)) / TEST_COUNT,
                     ((IRuntimeHintProvider)shards.get(1)).getRuntimeHint());
    }

    public void testRuntimeHint_optionNotSet() throws Exception {
        List<TestDescription> tests = Arrays.asList(GLES3_INFO_TEST_IDS);

        DeqpTestRunner deqpTest = buildGlesTestRunner(3, 0, tests, mTestsDir);

        long runtime = deqpTest.getRuntimeHint();
        assertTrue("Runtime for tests must be positive", runtime > 0);
        assertTrue("Runtime for tests must be reasonable",
                   runtime < (1000 * 10)); // Must be done in 10s
    }

    private void runInstrumentationLineAndAnswer(final String output)
        throws Exception {
        String cmd = getCommandLine();
        runInstrumentationLineAndAnswer(null, cmd, output);
    }



    private void
    runInstrumentationLineAndAnswer(final String testTrie, final String cmd,
                                    final String output) throws Exception {
        expectRemoveFile(APP_DIR + CASE_LIST_FILE_NAME);
        expectRemoveFile(APP_DIR + LOG_FILE_NAME);

        String remotePath = APP_DIR + CASE_LIST_FILE_NAME;
        if (testTrie != null) {
            expectPushString(testTrie + "\n", remotePath);
        } else {
            expectPushString((String)EasyMock.anyObject(), EasyMock.eq(remotePath));
        }

        String logFilename = APP_DIR + LOG_FILE_NAME;
        expectInstrumentationCommand(logFilename, cmd, output);
    }

    private void
    runInstrumentationLineAndAnswerParallel(final List<TestDescription> tests, final String cmd,
                                            final String output, int expectedParallelBatches) throws Exception {
        expectRemoveFolder(DeqpTestRunner.APP_DIR_PARALLEL_CASELISTS);
        expectRemoveFolder(DeqpTestRunner.APP_DIR_PARALLEL_LOGS);
        expectCreateDirectory(DeqpTestRunner.APP_DIR_PARALLEL_CASELISTS);
        expectCreateDirectory(DeqpTestRunner.APP_DIR_PARALLEL_LOGS);

        final int batchSize = 1000;
        for (int i = 0; i < expectedParallelBatches; i++) {
            String remotePath = DeqpTestRunner.APP_DIR_PARALLEL_CASELISTS + "dEQP-part" + (i + 1) + ".txt";
            if (tests != null) {
                List<TestDescription> subList = tests.subList(i * batchSize, Math.min((i + 1) * batchSize, tests.size()));
                String expectedTrie = DeqpTestRunner.generateTestCaseTrie(subList);
                expectPushString(EasyMock.eq(expectedTrie + "\n"),
                                 EasyMock.eq(remotePath));
            } else {
                expectPushString((String)EasyMock.anyObject(),
                                 EasyMock.eq(remotePath));
            }
        }

        String logFilename = DeqpTestRunner.APP_DIR_PARALLEL_LOGS;
        expectInstrumentationCommand(logFilename, cmd, output);
    }

    static private void writeStringsToFile(File target, Set<String> strings)
        throws IOException {
        try (PrintWriter out = new PrintWriter(new FileWriter(target))) {
            out.print(String.join(System.lineSeparator(), strings));
            out.println();
        }
    }

    private void addFilterFileForOption(DeqpTestRunner test,
                                        Set<String> filters, String option)
        throws IOException, ConfigurationException {
        String filterFile = option + ".txt";
        writeStringsToFile(new File(mTestsDir, filterFile), filters);
        OptionSetter setter = new OptionSetter(test);
        setter.setOptionValue(option, filterFile);
    }

    public void testIncludeFilterFile() throws Exception {
        List<TestDescription> allTests = Arrays.asList(SAMPLE_TEST_IDS);
        List<TestDescription> activeTests = Arrays.asList(
            SAMPLE_TEST_IDS[3], SAMPLE_TEST_IDS[4], SAMPLE_TEST_IDS[5]);

        String expectedTrie = "{dEQP-GLES3{pick_me{yes,ok,accepted}}}";

        DeqpTestRunner deqpTest =
            buildGlesTestRunner(3, 0, allTests, mTestsDir);
        Set<String> includes = new HashSet<>();
        includes.add("dEQP-GLES3.pick_me#*");
        addFilterFileForOption(deqpTest, includes, "include-filter-file");
        testFiltering(deqpTest, expectedTrie, activeTests);
    }

    public void testMissingIncludeFilterFile() throws Exception {
        List<TestDescription> allTests = Arrays.asList(
            SAMPLE_TEST_IDS[3], SAMPLE_TEST_IDS[4], SAMPLE_TEST_IDS[5]);

        String expectedTrie = "{dEQP-GLES3{pick_me{yes,ok,accepted}}}";

        DeqpTestRunner deqpTest =
            buildGlesTestRunner(3, 0, allTests, mTestsDir);
        OptionSetter setter = new OptionSetter(deqpTest);
        setter.setOptionValue("include-filter-file", "not-a-file.txt");
        try {
            testFiltering(deqpTest, expectedTrie, allTests);
            fail("Test execution should have aborted with exception.");
        } catch (RuntimeException e) {
        }
    }

    public void testExcludeFilterFile() throws Exception {
        List<TestDescription> allTests = Arrays.asList(SAMPLE_TEST_IDS);
        List<TestDescription> activeTests = Arrays.asList(
            SAMPLE_TEST_IDS[3], SAMPLE_TEST_IDS[4], SAMPLE_TEST_IDS[5]);

        String expectedTrie = "{dEQP-GLES3{pick_me{yes,ok,accepted}}}";

        DeqpTestRunner deqpTest =
            buildGlesTestRunner(3, 0, allTests, mTestsDir);
        Set<String> excludes = new HashSet<>();
        excludes.add("dEQP-GLES3.missing#*");
        addFilterFileForOption(deqpTest, excludes, "exclude-filter-file");
        testFiltering(deqpTest, expectedTrie, activeTests);
    }

    public void testFilterComboWithFiles() throws Exception {
        final TestDescription[] testIds = {
            new TestDescription("dEQP-GLES3.group1", "footah"),
            new TestDescription("dEQP-GLES3.group1", "foo"),
            new TestDescription("dEQP-GLES3.group1", "nope"),
            new TestDescription("dEQP-GLES3.group1", "nonotwant"),
            new TestDescription("dEQP-GLES3.group2", "foo"),
            new TestDescription("dEQP-GLES3.group2", "yes"),
            new TestDescription("dEQP-GLES3.group2", "thoushallnotpass"),
        };

        List<TestDescription> allTests = Arrays.asList(testIds);

        List<TestDescription> activeTests = new ArrayList<TestDescription>();
        activeTests.add(testIds[0]);
        activeTests.add(testIds[5]);

        String expectedTrie = "{dEQP-GLES3{group1{footah}group2{yes}}}";

        DeqpTestRunner deqpTest =
            buildGlesTestRunner(3, 0, allTests, mTestsDir);

        Set<String> includes = new HashSet<>();
        includes.add("dEQP-GLES3.group2#*");
        deqpTest.addAllIncludeFilters(includes);

        Set<String> fileIncludes = new HashSet<>();
        fileIncludes.add("dEQP-GLES3.group1#no*");
        fileIncludes.add("dEQP-GLES3.group1#foo*");
        addFilterFileForOption(deqpTest, fileIncludes, "include-filter-file");

        Set<String> fileExcludes = new HashSet<>();
        fileExcludes.add("*foo");
        fileExcludes.add("*thoushallnotpass");
        addFilterFileForOption(deqpTest, fileExcludes, "exclude-filter-file");

        deqpTest.addExcludeFilter("dEQP-GLES3.group1#no*");

        testFiltering(deqpTest, expectedTrie, activeTests);
    }

    public void testDotToHashConversionInFilters() throws Exception {
        final TestDescription[] testIds = {
            new TestDescription("dEQP-GLES3.missing", "no"),
            new TestDescription("dEQP-GLES3.pick_me", "donotwant"),
            new TestDescription("dEQP-GLES3.pick_me", "yes")};

        List<TestDescription> allTests = Arrays.asList(testIds);

        List<TestDescription> activeTests = new ArrayList<TestDescription>();
        activeTests.add(testIds[2]);

        String expectedTrie = "{dEQP-GLES3{pick_me{yes}}}";

        DeqpTestRunner deqpTest =
            buildGlesTestRunner(3, 0, allTests, mTestsDir);
        deqpTest.addIncludeFilter("dEQP-GLES3.pick_me.yes");
        testFiltering(deqpTest, expectedTrie, activeTests);
    }

    private static int calculateDeqpLevel(int year, int month, int day) {
        return (year << 16) + (month << 8) + day;
    }

    private static int calculateVulkanVersion(int variant, int major, int minor, int patch) {
        return (variant << 29) | (major << 22) | (minor << 12) | patch;
    }

    private void runDeqpLevelTest(int deqpLevel, boolean isHandheld, boolean enableNonHandheld, boolean expectSuccess) throws Exception {
        // Define the target test and a future caselist file date (2026).
        final TestDescription testId = new TestDescription("dEQP-GLES3.info", "version");
        final String testTrie = "{dEQP-GLES3{info{version}}}";
        final String caselistFile = "dEQP-GLES3-main-2026-03-01.txt";

        // Expected output from the simulated instrumentation run, indicating a successful pass.
        final String expectedInstrumentationOutput = buildTestProcessOutput(Collections.singletonList(testId));


        // Write the test case into the caselist file.
        String testlist = testId.getClassName() + "." + testId.getTestName() + "\n";
        FileUtil.writeToFile(testlist, new File(mTestsDir, caselistFile));

        // Configure the DeqpTestRunner.
        DeqpTestRunner deqpTest = new DeqpTestRunner();
        OptionSetter setter = new OptionSetter(deqpTest);
        setter.setOptionValue("deqp-package", "dEQP-GLES3");
        setter.setOptionValue("deqp-gl-config-name", "rgba8888d24s8");
        setter.setOptionValue("deqp-caselist-file", caselistFile);
        if (enableNonHandheld) {
            setter.setOptionValue("enable-deqp-outside-grf-non-handheld", "true");
        }
        deqpTest.setAbi(ABI);
        deqpTest.setBuildHelper(getMockBuildHelper(mTestsDir));

        // Mock basic OpenGL ES version requirement.
        expectGlVersion(3, 0);

        String featureString = "feature:" + DeqpTestRunner.FEATURE_OPENGLES_DEQP_LEVEL + "=" + deqpLevel;
        if (isHandheld) {
            featureString = "feature:" + DeqpTestRunner.FEATURE_TOUCHSCREEN + "\n" + featureString;
        }
        EasyMock.expect(mockDevice.executeShellCommand("pm list features")).andReturn(featureString);

        if (expectSuccess) {
            // Setup mock expectations for environment preparation and cleanup.
            expectAngleSetupAndTeardown();

            // Mock the renderability query.
            expectRenderConfigQuery(3, 0);

            // Define the exact command line expected to be executed on the device.
            String commandLine = getCommandLine();

            runInstrumentationLineAndAnswer(testTrie, commandLine, expectedInstrumentationOutput);
        }

        expectTestRunStarted(deqpTest, 1);
        mockListener.testStarted(EasyMock.eq(testId));
        EasyMock.expectLastCall().once();

        if (!expectSuccess) {
            mockListener.testIgnored(EasyMock.eq(testId));
            EasyMock.expectLastCall().once();
        }

        mockListener.testEnded(EasyMock.eq(testId), EasyMock.<HashMap<String, Metric>>notNull());
        EasyMock.expectLastCall().once();
        expectTestRunEnded();

        runAndVerifyTest(deqpTest);
    }

    /**
     * Tests that a handheld device with a dEQP level of 2025 or higher will unconditionally run tests
     * from a future caselist.
     * <p>
     * This verifies the Google Requirements Freeze (GRF) override policy. Devices claiming a dEQP level
     * of 2025 or higher should execute the test cases even if the
     * provided caselist date (e.g., 2026) is newer than the device's claimed level, provided the device
     * is considered "handheld" (has a touchscreen).
     *
     * @throws Exception if an error occurs during mock setup or test execution
     */
    public void testRun_deqpLevel2025UnconditionalHandheld() throws Exception {
        final int deqpLevel2025 = calculateDeqpLevel(2025, 3, 1);
        runDeqpLevelTest(deqpLevel2025, true /* handheld */, false /* enableNonHandheld */, true /* expectSuccess */);
    }

    /**
     * Tests that a non-handheld device with a dEQP level of 2025 or higher will unconditionally run tests
     * from a future caselist when the {@code enable-deqp-non-handheld} option is enabled.
     * <p>
     * This verifies that the GRF override policy (normally restricted to handheld devices) can be forced
     * to apply to non-handheld devices (devices without a touchscreen) by setting the runner option
     * {@code enable-deqp-non-handheld} to true.
     *
     * @throws Exception if an error occurs during mock setup or test execution
     */
    public void testRun_deqpLevel2025UnconditionalNonHandheld() throws Exception {
        final int deqpLevel2025 = calculateDeqpLevel(2025, 3, 1);
        runDeqpLevelTest(deqpLevel2025, false /* handheld */, true /* enableNonHandheld */, true /* expectSuccess */);
    }

    /**
     * Tests that a device with a dEQP level of 2024 or below correctly skips tests from a future caselist.
     * <p>
     * This verifies that the GRF override policy is *not* triggered for older devices. If a device claims
     * a dEQP level (e.g., 2024) that is older than the requirement for the provided caselist date (e.g., 2025),
     * and the device does not meet the 2025 GRF threshold, the tests should be bypassed and reported as ignored
     * rather than being executed.
     *
     * @throws Exception if an error occurs during mock setup or test execution
     */
    public void testRun_deqpLevel2024FutureCaselistIgnored() throws Exception {
        final int deqpLevel2024 = calculateDeqpLevel(2024, 3, 1);
        runDeqpLevelTest(deqpLevel2024, true /* handheld */, false /* enableNonHandheld */, false /* expectSuccess */);
    }

    private void runSpecificOptionsTest(String specificLevel, boolean countOnly, boolean expectMatch) throws Exception {
        final TestDescription testId = new TestDescription("dEQP-GLES3.info", "version");
        final String caselistFile = "dEQP-GLES3-main-2025-03-01.txt";
        final String testTrie = "{dEQP-GLES3{info{version}}}";

        final String expectedInstrumentationOutput = buildTestProcessOutput(Collections.singletonList(testId));

        String testlist = testId.getClassName() + "." + testId.getTestName() + "\n";
        FileUtil.writeToFile(testlist, new File(mTestsDir, caselistFile));

        DeqpTestRunner deqpTest = new DeqpTestRunner();
        OptionSetter setter = new OptionSetter(deqpTest);
        setter.setOptionValue("deqp-package", "dEQP-GLES3");
        setter.setOptionValue("deqp-gl-config-name", "rgba8888d24s8");
        setter.setOptionValue("deqp-caselist-file", caselistFile);
        if (specificLevel != null) {
            setter.setOptionValue("run-specific-deqp-level", specificLevel);
        }
        if (countOnly) {
            setter.setOptionValue("deqp-tests-count-only", "true");
        }
        deqpTest.setAbi(ABI);
        deqpTest.setBuildHelper(getMockBuildHelper(mTestsDir));

        if (!countOnly) {
            expectGlVersion(3, 0);

            if (expectMatch) {
                expectAngleSetupAndTeardown();

                expectRenderConfigQuery(3, 0);

                String commandLine = getCommandLine();

                runInstrumentationLineAndAnswer(testTrie, commandLine, expectedInstrumentationOutput);
            }

            expectTestRunStarted(deqpTest, 1);
            mockListener.testStarted(EasyMock.eq(testId));
            EasyMock.expectLastCall().once();

            if (!expectMatch) {
                mockListener.testIgnored(EasyMock.eq(testId));
                EasyMock.expectLastCall().once();
            }

            mockListener.testEnded(EasyMock.eq(testId), EasyMock.<HashMap<String, Metric>>notNull());
            EasyMock.expectLastCall().once();
            expectTestRunEnded();
        }

        runAndVerifyTest(deqpTest);
    }

    /**
     * Tests that custom configuration options are correctly copied to new runner shards
     * when a {@link DeqpTestRunner} is split for parallel execution.
     * <p>
     * This ensures that any command-line arguments (like specific dEQP levels or count-only flags)
     * passed to the original runner are preserved in the individual shard runners.
     *
     * @throws Exception if an error occurs during runner setup, reflection, or test execution
     */
    public void testCopyOptions() throws Exception {
        // Create a minimal set of dummy tests to allow the runner to be initialized
        Collection<TestDescription> tests = Collections.singletonList(
            new TestDescription("dEQP-GLES3.info", "version")
        );

        // Initialize the main DeqpTestRunner and set our custom command-line options
        DeqpTestRunner runner = buildGlesTestRunner(3, 0, tests, mTestsDir);
        OptionSetter setter = new OptionSetter(runner);
        setter.setOptionValue("run-specific-deqp-level", "2020");
        setter.setOptionValue("deqp-tests-count-only", "true");
        setter.setOptionValue("deqp-test-events-reporting-mode", DeqpTestRunner.REPORTING_MODE_NATIVE_LOG_PARSER);

        // Trigger the sharding process, which creates new runner instances based on the original
        ArrayList<IRemoteTest> shards = (ArrayList<IRemoteTest>)runner.split();
        assertNotNull(shards);
        assertEquals(1, shards.size());

        DeqpTestRunner shard = (DeqpTestRunner)shards.get(0);

        assertEquals("2020", shard.getRunSpecificDeqpLevel());
        assertTrue(shard.getDeqpTestsCountOnly());
        assertEquals(DeqpTestRunner.REPORTING_MODE_NATIVE_LOG_PARSER, shard.getEventReportingMode());
    }

    /**
     * Verifies that setting the 'deqp-tests-count-only' flag successfully prevents the
     * actual execution of dEQP tests.
     * <p>
     * When this flag is true, the test runner is expected to parse the test list and count
     * the tests, but completely abort before communicating with the device instrumentation
     * or sending any execution events to the test listener.
     *
     * @throws Exception if an error occurs during mock setup or test execution
     */
    public void testRun_deqpTestsCountOnly() throws Exception {
        runSpecificOptionsTest(null, true, false);
    }

    /**
     * Verifies that the test runner executes the dEQP caselist normally when the user forces
     * execution for a specific year and that year matches the year embedded in the caselist filename.
     * <p>
     * This tests the 'run-specific-deqp-level' flag. In this scenario, we enforce the level to 2025
     * and use a caselist file named for 2025. This should bypass standard device capability checks
     * and directly run the tests.
     *
     * @throws Exception if an error occurs during mock setup or test execution
     */
    public void testRun_runSpecificDeqpLevelMatch() throws Exception {
        runSpecificOptionsTest("2025", false, true);
    }

    /**
     * Verifies that the test runner explicitly skips and ignores tests when the user forces
     * execution for a specific year, but that year does NOT match the year embedded in the
     * caselist filename.
     * <p>
     * This tests the mismatch scenario of the 'run-specific-deqp-level' flag. In this case,
     * we force the level to 2024, but provide a caselist for 2025. The runner is expected
     * to safely bypass the execution and report the tests as ignored.
     *
     * @throws Exception if an error occurs during mock setup or test execution
     */
    public void testRun_runSpecificDeqpLevelMismatch() throws Exception {
        runSpecificOptionsTest("2024", false, false);
    }

    /**
     * Test Vulkan tests on device with Vulkan supported.
     */
    public void testRun_vulkanSupported() throws Exception {
        final TestDescription testId = new TestDescription("dEQP-VK.info", "version");
        final String caselistFile = "dEQP-VK-main-2025-03-01.txt";
        final String testTrie = "{dEQP-VK{info{version}}}";

        final String expectedInstrumentationOutput = buildTestProcessOutput(Collections.singletonList(testId));


        String testlist = testId.getClassName() + "." + testId.getTestName() + "\n";
        FileUtil.writeToFile(testlist, new File(mTestsDir, caselistFile));

        DeqpTestRunner deqpTest = new DeqpTestRunner();
        OptionSetter setter = new OptionSetter(deqpTest);
        setter.setOptionValue("deqp-package", "dEQP-VK");
        setter.setOptionValue("deqp-caselist-file", caselistFile);
        deqpTest.setAbi(ABI);
        deqpTest.setBuildHelper(getMockBuildHelper(mTestsDir));


        final int deqpLevel2025 = calculateDeqpLevel(2025, 3, 1);
        final int vulkanHardwareVersion = calculateVulkanVersion(0, 1, 4, 0);
        expectVulkanVersion(deqpLevel2025, vulkanHardwareVersion);

        String commandLine = String.format(
            "--deqp-caselist-file=%s --deqp-screen-rotation=unspecified "
                + "--deqp-surface-type=window --deqp-log-images=disable "
                + "--deqp-watchdog=enable",
            APP_DIR + CASE_LIST_FILE_NAME);

        runInstrumentationLineAndAnswer(testTrie, commandLine, expectedInstrumentationOutput);

        expectRunAndVerifyTest(deqpTest, Collections.singletonList(testId));
    }

    private DeqpTestRunner setupTestRunner(List<TestDescription> tests, boolean enableParallelRun) throws Exception {
        return setupTestRunner(tests, enableParallelRun, true);
    }

    private DeqpTestRunner setupTestRunner(List<TestDescription> tests, boolean enableParallelRun, boolean isHandheld) throws Exception {
        DeqpTestRunner deqpTest = buildGlesTestRunner(3, 0, tests, mTestsDir);

        if (enableParallelRun) {
            OptionSetter setter = new OptionSetter(deqpTest);
            setter.setOptionValue("enable-deqp-parallel-run", "true");
        }

        expectGlVersion(3, 0);
        expectRenderConfigQuery(3, 0);

        String featureString = "feature:" + DeqpTestRunner.FEATURE_OPENGLES_DEQP_LEVEL + "=132580097";
        if (isHandheld) {
            featureString = "feature:" + DeqpTestRunner.FEATURE_TOUCHSCREEN + "\n" + featureString;
        } else {
            featureString = "feature:" + DeqpTestRunner.FEATURE_LEANBACK + "\n" + featureString;
        }
        EasyMock.expect(mockDevice.executeShellCommand("pm list features")).andReturn(featureString).anyTimes();

        return deqpTest;
    }

    /**
     * Test running in sequential mode when the parallel execution option is disabled.
     * <p>
     * Verifies that even with a large test count (5000 tests), the runner falls back
     * to executing the tests sequentially in multiple batches (5 batches of 1000 tests each)
     * using the default single-batch instrumentation and caselist path.
     *
     * @throws Exception if an error occurs during mock setup or execution
     */
    public void testRun_multipleBatches() throws Exception {
        final int numTests = 5000;
        List<TestDescription> tests = generateTestList(numTests);

        DeqpTestRunner deqpTest = setupTestRunner(tests, false);

        // Mock 5 batch executions sequentially
        final int batchSize = 1000;
        for (int i = 0; i < numTests; i += batchSize) {
            List<TestDescription> subList = tests.subList(i, Math.min(i + batchSize, numTests));
            String subOutput = buildTestProcessOutput(subList);
            runInstrumentationLineAndAnswer(null, getCommandLine(), subOutput);
        }

        expectRunAndVerifyTest(deqpTest, tests);
    }

    /**
     * Test running in parallel mode when the test count is below the parallel execution threshold.
     * <p>
     * Verifies that when the parallel run option is enabled, but the input test size (1 test)
     * is below the threshold (5000 tests), the runner transparently falls back to sequential
     * execution to avoid setup overhead.
     *
     * @throws Exception if an error occurs during mock setup or execution
     */
    public void testRun_parallelModeEnabled_testCountBelowThreshold() throws Exception {
        final TestDescription testId = new TestDescription("dEQP-GLES3.info", "version");
        List<TestDescription> tests = Collections.singletonList(testId);

        DeqpTestRunner deqpTest = setupTestRunner(tests, true);
        OptionSetter setter = new OptionSetter(deqpTest);
        setter.setOptionValue("deqp-test-events-reporting-mode", DeqpTestRunner.REPORTING_MODE_NATIVE_LOG_PARSER);

        String output = buildTestProcessOutput(tests);
        runInstrumentationLineAndAnswer(null, getCommandLine(), output);

        expectRunAndVerifyTest(deqpTest, tests);
    }

    /**
     * Test running in parallel mode when the test count is at or above the parallel execution threshold.
     * <p>
     * Verifies that when the parallel run option is enabled and the test count is large enough
     * (5000 tests), the runner successfully slices and pushes all batches to the device at once,
     * executing them in parallel and using the parallel logs directory.
     *
     * @throws Exception if an error occurs during mock setup or execution
     */
    public void testRun_parallelModeEnabled_testCountAboveThreshold() throws Exception {
        final int numTests = 5000;
        List<TestDescription> tests = generateTestList(numTests);

        DeqpTestRunner deqpTest = setupTestRunner(tests, true);
        OptionSetter setter = new OptionSetter(deqpTest);
        setter.setOptionValue("deqp-test-events-reporting-mode", DeqpTestRunner.REPORTING_MODE_NATIVE_LOG_PARSER);

        String output = buildTestProcessOutput(tests);
        String parallelCmd = "--deqp-gl-config-name=rgba8888d24s8 --deqp-screen-rotation=unspecified --deqp-surface-type=window --deqp-log-images=disable --deqp-watchdog=enable";

        runInstrumentationLineAndAnswerParallel(tests, parallelCmd, output, 5);

        expectRunAndVerifyTest(deqpTest, tests);
    }

    /**
     * Test running when parallel mode is enabled and test count is above threshold,
     * but the target device is non-handheld (e.g. TV).
     * <p>
     * Verifies that when running on a non-handheld device, the runner transparently falls back to
     * sequential execution even if parallel run is enabled and test count is above threshold.
     */
    public void testRun_parallelModeEnabled_nonHandheldDevice_fallsBackToSequential() throws Exception {
        final int numTests = 5000;
        List<TestDescription> tests = generateTestList(numTests);

        DeqpTestRunner deqpTest = setupTestRunner(tests, true, false);
        OptionSetter setter = new OptionSetter(deqpTest);
        setter.setOptionValue("deqp-test-events-reporting-mode", DeqpTestRunner.REPORTING_MODE_NATIVE_LOG_PARSER);

        final int batchSize = 1000;
        for (int i = 0; i < numTests; i += batchSize) {
            List<TestDescription> subList = tests.subList(i, Math.min(i + batchSize, numTests));
            String subOutput = buildTestProcessOutput(subList);
            runInstrumentationLineAndAnswer(null, getCommandLine(), subOutput);
        }

        expectRunAndVerifyTest(deqpTest, tests);
    }

    /**
     * Test that an invalid test event reporting mode throws an IllegalArgumentException.
     * <p>
     * Verifies that if an unsupported reporting mode is explicitly set via options,
     * the test runner immediately fails during validation.
     */
    public void testRun_invalidReportingMode_throwsIllegalArgumentException() throws Exception {
        final TestDescription testId = new TestDescription("dEQP-GLES3.info", "version");
        List<TestDescription> tests = Collections.singletonList(testId);

        DeqpTestRunner deqpTest = setupTestRunner(tests, true);
        deqpTest.setDevice(mockDevice);

        OptionSetter setter = new OptionSetter(deqpTest);
        setter.setOptionValue("deqp-test-events-reporting-mode", "invalid-parser");

        mockListener.testRunStarted(EasyMock.anyObject(), EasyMock.eq(1));
        EasyMock.expectLastCall().once();
        mockListener.testRunEnded(EasyMock.anyLong(), EasyMock.<HashMap<String, Metric>>notNull());
        EasyMock.expectLastCall().once();

        expectAngleSetup();
        EasyMock.replay(mockDevice, mockListener);

        try {
            deqpTest.run(mockListener);
            fail("Expected IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            // Expected
        }
    }

    private void runAndVerifyTest(DeqpTestRunner deqpTest) throws Exception {
        EasyMock.replay(mockDevice, mockIDevice, mockListener);
        deqpTest.setDevice(mockDevice);
        deqpTest.run(mockListener);
        EasyMock.verify(mockListener, mockDevice, mockIDevice);
    }

    private void runAndVerifyTestInterrupted(DeqpTestRunner deqpTest, IRunUtil mockRunUtil)
        throws Exception {
        EasyMock.replay(mockDevice, mockIDevice, mockListener);
        EasyMock.replay(mockRunUtil);
        try {
            deqpTest.run(mockListener);
            fail("expected RunInterruptedException");
        } catch (RunInterruptedException ex) {
            // expected
        }
        EasyMock.verify(mockRunUtil, mockListener, mockDevice, mockIDevice);
    }

    private List<TestDescription> generateTestList(int numTests) {
        List<TestDescription> tests = new ArrayList<>();
        for (int i = 0; i < numTests; i++) {
            tests.add(new TestDescription("dEQP-GLES3.info", "test" + i));
        }
        return tests;
    }

    private void expectShellCommand(String command, String output) throws DeviceNotAvailableException {
        EasyMock.expect(mockDevice.executeShellCommand(EasyMock.eq(command)))
            .andReturn(output)
            .once();
    }

    private void expectShellCommandContains(String substring, String output) throws DeviceNotAvailableException {
        EasyMock.expect(mockDevice.executeShellCommand(EasyMock.contains(substring)))
            .andReturn(output)
            .once();
    }

    private void expectAngleSetup() throws Exception {
        expectShellCommand("settings delete global angle_gl_driver_selection_pkgs", "");
        expectShellCommand("settings delete global angle_gl_driver_selection_values", "");
    }

    private void expectAngleSetupAndTeardown() throws Exception {
        // Expect the calls twice: setupTestEnvironment() and teardownTestEnvironment()
        expectAngleSetup();
        expectAngleSetup();
    }

    private void expectGlVersion(int majorVersion, int minorVersion) throws Exception {
        int version = (majorVersion << 16) | minorVersion;
        EasyMock.expect(mockDevice.getProperty("ro.opengles.version"))
            .andReturn(Integer.toString(version))
            .atLeastOnce();
    }

    private void expectVulkanVersion(int deqpLevel, int vulkanHardwareVersion) throws Exception {
        int majorVersion = (vulkanHardwareVersion >> 22) & 0x7F;
        assertEquals("Only Vulkan version 1.0 is supported in this mock", 1, majorVersion);

        String featureString = "feature:" + DeqpTestRunner.FEATURE_VULKAN_LEVEL + "=" + majorVersion + "\n"
                + "feature:" + DeqpTestRunner.FEATURE_VULKAN_DEQP_LEVEL + "=" + deqpLevel + "\n"
                + "feature:" + DeqpTestRunner.FEATURE_VULKAN_HARDWARE_VERSION  + "=" + vulkanHardwareVersion;
        EasyMock.expect(mockDevice.executeShellCommand("pm list features"))
            .andReturn(featureString)
            .atLeastOnce();
    }

    private void expectRenderConfigQuery(int majorVersion, int minorVersion)
        throws Exception {
        expectRenderConfigQuery(
            String.format("--deqp-gl-config-name=rgba8888d24s8 "
                              + "--deqp-screen-rotation=unspecified "
                              + "--deqp-surface-type=window "
                              + "--deqp-gl-major-version=%d "
                              + "--deqp-gl-minor-version=%d",
                          majorVersion, minorVersion));
    }

    private void expectRenderConfigQuery(String commandLine) throws Exception {
        expectRenderConfigQueryAndReturn(commandLine, "Yes");
    }

    private void expectRenderConfigQueryAndReturn(String commandLine, String output)
        throws Exception {
        final String queryOutput =
            "INSTRUMENTATION_RESULT: Supported=" + output + "\r\n"
            + "INSTRUMENTATION_CODE: 0\r\n";
        final String command = String.format(
            "am instrument %s -w -e deqpQueryType renderConfigSupported -e deqpCmdLine "
                + "\"%s\" %s",
            AbiUtils.createAbiFlag(ABI.getName()), commandLine,
            QUERY_INSTRUMENTATION_NAME);

        mockDevice.executeShellCommand(
            EasyMock.eq(command), EasyMock.<IShellOutputReceiver>notNull());

        EasyMock.expectLastCall().andAnswer(new IAnswer<Object>() {
            @Override
            public Object answer() {
                IShellOutputReceiver receiver =
                    (IShellOutputReceiver)EasyMock.getCurrentArguments()[1];

                receiver.addOutput(queryOutput.getBytes(), 0,
                                   queryOutput.length());
                receiver.flush();

                return null;
            }
        });
    }

    private void expectRemoveFile(String path) throws Exception {
        expectShellCommand("rm " + path, "");
    }

    private void expectRemoveFolder(String path) throws Exception {
        expectShellCommand("rm -rf " + path, "");
    }

    private void expectCreateDirectory(String path) throws Exception {
        expectShellCommand("mkdir -p " + path, "");
    }

    private void expectPushString(String content, String remotePath) throws Exception {
        mockDevice.pushString(content, remotePath);
        EasyMock.expectLastCall().andReturn(true).once();
    }

    private void expectInstrumentationCommand(String logFilename, String cmd, final String output) throws Exception {
        String command = String.format(
            "am instrument %s -w -e deqpLogFilename \"%s\" -e deqpCmdLine \"%s\" "
                + "-e deqpLogData \"%s\" -e deqpEventReportingMode \"%s\" %s",
            AbiUtils.createAbiFlag(ABI.getName()), logFilename, cmd,
            false, DeqpTestRunner.REPORTING_MODE_NATIVE_LOG_PARSER, INSTRUMENTATION_NAME);

        mockDevice.executeShellV2CommandNoRecovery(
            EasyMock.eq(command), EasyMock.<IShellOutputReceiver>notNull(),
            EasyMock.anyLong(), EasyMock.isA(TimeUnit.class));

        EasyMock.expectLastCall().andAnswer(new IAnswer<Object>() {
            @Override
            public Object answer() {
                IShellOutputReceiver receiver =
                    (IShellOutputReceiver)EasyMock.getCurrentArguments()[1];

                receiver.addOutput(output.getBytes(), 0, output.length());
                receiver.flush();

                return null;
            }
        });
    }

    private void expectTestRunStarted(DeqpTestRunner deqpTest, int testCount) {
        mockListener.testRunStarted(getTestId(deqpTest), testCount);
        EasyMock.expectLastCall().once();
    }

    private void expectTestRunEnded() {
        mockListener.testRunEnded(EasyMock.anyLong(), EasyMock.<HashMap<String, Metric>>notNull());
        EasyMock.expectLastCall().once();
    }

    private void expectListenerTests(Collection<TestDescription> tests) {
        for (TestDescription testId : tests) {
            mockListener.testStarted(EasyMock.eq(testId));
            EasyMock.expectLastCall().once();
            mockListener.testEnded(EasyMock.eq(testId), EasyMock.<HashMap<String, Metric>>notNull());
            EasyMock.expectLastCall().once();
        }
    }

    private void expectRunAndVerifyTest(DeqpTestRunner deqpTest, Collection<TestDescription> tests) throws Exception {
        expectTestRunStarted(deqpTest, tests.size());
        expectAngleSetupAndTeardown();
        expectListenerTests(tests);
        expectTestRunEnded();
        runAndVerifyTest(deqpTest);
    }
}