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

import com.android.ddmlib.MultiLineReceiver;
import com.drawelements.deqp.runner.DeqpTestRunner;
import com.drawelements.deqp.runner.BatchRunConfiguration;
import com.android.tradefed.config.OptionClass;
import com.android.tradefed.result.ITestInvocationListener;
import com.android.tradefed.device.DeviceNotAvailableException;
import com.android.tradefed.util.AbiUtils;
import com.android.tradefed.util.FileUtil;
import com.android.tradefed.util.RunInterruptedException;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import com.android.tradefed.metrics.proto.MetricMeasurement.Metric;
import com.android.tradefed.log.LogUtil.CLog;
import com.android.tradefed.result.ByteArrayInputStreamSource;
import com.android.tradefed.result.LogDataType;
import com.android.tradefed.result.TestDescription;
import java.util.regex.Pattern;

@OptionClass(alias="khronos-gl-cts-test-runner")
public class KhronosCTSRunner extends DeqpTestRunner {
    private List<String> mTestRunParams = null;

    private static final String KHRONOS_CTS_ONDEVICE_PKG = "org.khronos.gl_cts";

    private static final String TEST_ID_NAME = "KhronosGLCTS";

    private Map<TestDescription, Set<KhronosCTSBatchRunConfiguration>> mTestInstances = null;

    private final KhronosCTSTestInstanceResultListener mInstanceListener = new KhronosCTSTestInstanceResultListener();

    /**
     * Test instance listerer and invocation result forwarded.
     * Declared as private nested class of KhronosCTSRunner because
     * it can access the private KhronosCTSRunner members such as
     * mRemainingTests and mPendingResults
     */
    private class KhronosCTSTestInstanceResultListener extends DeqpTestRunner.TestInstanceResultListener {
        private KhronosCTSBatchRunConfiguration mKhronosCTSRunConfig;
        private class KhronosCTSPendingResult {
            boolean allInstancesPassed;
            Map<KhronosCTSBatchRunConfiguration, String> testLogs;
            Map<KhronosCTSBatchRunConfiguration, String> errorMessages;
            Set<KhronosCTSBatchRunConfiguration> remainingConfigs;
        }
        private final Map<TestDescription, KhronosCTSPendingResult> mKhronosCTSPendingResults = new HashMap<>();

        @Override
        public void setCurrentConfig(BatchRunConfiguration runConfig) {
            mKhronosCTSRunConfig = (KhronosCTSBatchRunConfiguration)runConfig;
        }

        /**
         * Forward result to Tradefed Test Invocation Listener
         */
        @Override
        protected void forwardFinalizedPendingResult(TestDescription testId) {
            if (mRemainingTests.contains(testId)) {
                final KhronosCTSPendingResult result = mKhronosCTSPendingResults.get(testId);
                mKhronosCTSPendingResults.remove(testId);
                mRemainingTests.remove(testId);
                // Forward results to the sink
                mSink.testStarted(testId);
                // Test Log
                if (mLogData) {
                    for (Map.Entry<KhronosCTSBatchRunConfiguration, String> entry :
                            result.testLogs.entrySet()) {
                        final ByteArrayInputStreamSource source
                            = new ByteArrayInputStreamSource(entry.getValue().getBytes());
                        mSink.testLog(testId.getClassName() + "." + testId.getTestName() + "@"
                            + entry.getKey().getId(), LogDataType.XML, source);
                        source.close();
                    }
                }
                // Error message
                if (!result.allInstancesPassed) {
                    final StringBuilder errorLog = new StringBuilder();
                    for (Map.Entry<KhronosCTSBatchRunConfiguration, String> entry :
                        result.errorMessages.entrySet()) {
                        if (errorLog.length() > 0) {
                            errorLog.append('\n');
                        }
                        errorLog.append(String.format("=== with config %s ===\n",
                            entry.getKey().getId()));
                        errorLog.append(entry.getValue());
                    }
                    mSink.testFailed(testId, errorLog.toString());
                }
                final HashMap<String, Metric> emptyMap = new HashMap<>();
                mSink.testEnded(testId, emptyMap);
            }
        }

        /**
         * Declare existence of a test and instances
         */
        public void setTestInstancesExperiment(TestDescription testId, Set<KhronosCTSBatchRunConfiguration> configs) {

            // Test instances cannot change at runtime, ignore if we have already set this
            if (!mKhronosCTSPendingResults.containsKey(testId)) {
                final KhronosCTSPendingResult pendingResult = new KhronosCTSPendingResult();
                pendingResult.allInstancesPassed = true;
                pendingResult.testLogs = new LinkedHashMap<>();
                pendingResult.errorMessages = new LinkedHashMap<>();
                pendingResult.remainingConfigs = new HashSet<>(configs); // avoid mutating argument
                mKhronosCTSPendingResults.put(testId, pendingResult);
            }
        }

        /**
         * Query if test instance has not yet been executed
         */
        @Override
        public boolean isPendingTestInstance(TestDescription testId,
                BatchRunConfiguration config) {
            KhronosCTSBatchRunConfiguration khronosCTSBatchConfig = (KhronosCTSBatchRunConfiguration)config;
            final KhronosCTSPendingResult result = mKhronosCTSPendingResults.get(testId);
            if (result == null) {
                // test is not in the current working batch of the runner, i.e. it cannot be
                // "partially" completed.
                if (!mRemainingTests.contains(testId)) {
                    // The test has been fully executed. Not pending.
                    return false;
                } else {
                    // Test has not yet been executed. Check if such instance exists
                    return mTestInstances.get(testId).contains(khronosCTSBatchConfig);
                }
            } else {
                // could be partially completed, check this particular config
                return result.remainingConfigs.contains(config);
            }
        }

        /**
         * Fake failure of an instance with current config
         */
        @Override
        public void abortTest(TestDescription testId, String errorMessage) {
            final KhronosCTSPendingResult result = mKhronosCTSPendingResults.get(testId);
            // Mark as executed
            result.allInstancesPassed = false;
            result.errorMessages.put(mKhronosCTSRunConfig, errorMessage);
            result.remainingConfigs.remove(mKhronosCTSRunConfig);
            // Pending result finished, report result
            if (result.remainingConfigs.isEmpty()) {
                forwardFinalizedPendingResult(testId);
            }
            if (testId.equals(mCurrentTestId)) {
                mCurrentTestId = null;
            }
        }

        /**
         * Handles beginning of dEQP testcase.
         */
        @Override
        protected void handleBeginTestCase(Map<String, String> values) {
            mCurrentTestId = pathToIdentifier(values.get("dEQP-BeginTestCase-TestCasePath"));
            mCurrentTestLog = "";
            mGotTestResult = false;
            // mark instance as started
            if (mKhronosCTSPendingResults.get(mCurrentTestId) != null) {
                mKhronosCTSPendingResults.get(mCurrentTestId).remainingConfigs.remove(mKhronosCTSRunConfig);
            } else {
                CLog.w("Got unexpected start of %s", mCurrentTestId);
            }
        }

        /**
         * Handles end of dEQP testcase.
         */
        @Override
        protected void handleEndTestCase(Map<String, String> values) {
            final KhronosCTSPendingResult result = mKhronosCTSPendingResults.get(mCurrentTestId);
            if (result != null) {
                if (!mGotTestResult) {
                    result.allInstancesPassed = false;
                    result.errorMessages.put(mKhronosCTSRunConfig, INCOMPLETE_LOG_MESSAGE);
                }
                if (mLogData && mCurrentTestLog != null && mCurrentTestLog.length() > 0) {
                    result.testLogs.put(mKhronosCTSRunConfig, mCurrentTestLog);
                }
                // Pending result finished, report result
                if (result.remainingConfigs.isEmpty()) {
                    forwardFinalizedPendingResult(mCurrentTestId);
                }
            } else {
                CLog.w("Got unexpected end of %s", mCurrentTestId);
            }
            mCurrentTestId = null;
        }

        /**
         * Handles dEQP testcase result.
         */
        @Override
        protected void handleTestCaseResult(Map<String, String> values) {
            String code = values.get("dEQP-TestCaseResult-Code");
            String details = values.get("dEQP-TestCaseResult-Details");
            if (mKhronosCTSPendingResults.get(mCurrentTestId) == null) {
                CLog.w("Got unexpected result for %s", mCurrentTestId);
                mGotTestResult = true;
                return;
            }
            if (code.compareTo("Pass") == 0) {
                mGotTestResult = true;
            } else if (code.compareTo("NotSupported") == 0) {
                mGotTestResult = true;
            } else if (code.compareTo("QualityWarning") == 0) {
                mGotTestResult = true;
            } else if (code.compareTo("CompatibilityWarning") == 0) {
                mGotTestResult = true;
            } else if (code.compareTo("Fail") == 0 || code.compareTo("ResourceError") == 0
                    || code.compareTo("InternalError") == 0 || code.compareTo("Crash") == 0
                    || code.compareTo("Timeout") == 0) {
                mKhronosCTSPendingResults.get(mCurrentTestId).allInstancesPassed = false;
                mKhronosCTSPendingResults.get(mCurrentTestId)
                        .errorMessages.put(mKhronosCTSRunConfig, code + ": " + details);
                mGotTestResult = true;
            } else {
                String codeError = "Unknown result code: " + code;
                mKhronosCTSPendingResults.get(mCurrentTestId).allInstancesPassed = false;
                mKhronosCTSPendingResults.get(mCurrentTestId)
                        .errorMessages.put(mKhronosCTSRunConfig, codeError + ": " + details);
                mGotTestResult = true;
            }
        }

        /**
         * Handles terminated dEQP testcase.
         */
        @Override
        protected void handleTestCaseTerminate(Map<String, String> values) {
            final KhronosCTSPendingResult result = mKhronosCTSPendingResults.get(mCurrentTestId);
            if (result != null) {
                String reason = values.get("dEQP-TerminateTestCase-Reason");
                mKhronosCTSPendingResults.get(mCurrentTestId).allInstancesPassed = false;
                mKhronosCTSPendingResults.get(mCurrentTestId)
                        .errorMessages.put(mKhronosCTSRunConfig, "Terminated: " + reason);
                // Pending result finished, report result
                if (result.remainingConfigs.isEmpty()) {
                    forwardFinalizedPendingResult(mCurrentTestId);
                }
            } else {
                CLog.w("Got unexpected termination of %s", mCurrentTestId);
            }
            mCurrentTestId = null;
            mGotTestResult = true;
        }

        private void handleTestRunParams(Map<String, String> values) {
            mTestRunParams.add(values.get("dEQP-TestRunParam"));
        }

        /**
         * Handles new instrumentation status message.
         */
        @Override
        public void handleStatus(Map<String, String> values) {
            String eventType = values.get("dEQP-EventType");
            if (eventType == null) {
                return;
            }
            if (eventType.compareTo("BeginSession") == 0) {
                handleBeginSession(values);
            } else if (eventType.compareTo("EndSession") == 0) {
                handleEndSession(values);
            } else if (eventType.compareTo("BeginTestCase") == 0) {
                handleBeginTestCase(values);
            } else if (eventType.compareTo("EndTestCase") == 0) {
                handleEndTestCase(values);
            } else if (eventType.compareTo("TestCaseResult") == 0) {
                handleTestCaseResult(values);
            } else if (eventType.compareTo("TerminateTestCase") == 0) {
                handleTestCaseTerminate(values);
            } else if (eventType.compareTo("TestLogData") == 0) {
                handleTestLogData(values);
            } else if (eventType.compareTo("BeginTestRunParams") == 0) {
                handleTestRunParams(values);
            }
        }

        /**
         * Signal listener that batch ended and forget incomplete results.
         */
        @Override
        public void endBatch() {
            // end open test if when stream ends
            if (mCurrentTestId != null) {
                // Current instance was removed from remainingConfigs when case
                // started. Mark current instance as pending.
                if (mKhronosCTSPendingResults.get(mCurrentTestId) != null) {
                    mKhronosCTSPendingResults.get(mCurrentTestId).remainingConfigs.add(mKhronosCTSRunConfig);
                } else {
                    CLog.w("Got unexpected internal state of %s", mCurrentTestId);
                }
            }
            mCurrentTestId = null;
        }
    }

    private static class KhronosCTSTestBatch extends DeqpTestRunner.TestBatch{
        private KhronosCTSBatchRunConfiguration mConfig;

        @Override
        public KhronosCTSBatchRunConfiguration getTestBatchConfig() {return mConfig;}

        @Override
        public void setTestBatchConfig(BatchRunConfiguration config){
            mConfig = (KhronosCTSBatchRunConfiguration)config;
        }
    }

    /**
     * KhronosCTS instrumentation parser
     */
    private static class KhronosCTSInstrumentationParser extends DeqpTestRunner.InstrumentationParser {

        public KhronosCTSInstrumentationParser(TestInstanceResultListener listener) {
            assert (listener instanceof KhronosCTSTestInstanceResultListener);
            mListener = listener;
        }
    }

    // Constructor
    public KhronosCTSRunner(){

    }

    @Override
    protected TestInstanceResultListener getInstanceListener() {return mInstanceListener;}

    @Override
    protected int getNumRemainingInstances() {
        int retVal = 0;
        for (TestDescription testId : mRemainingTests) {
            // If case is in current working set, sum only not yet executed instances.
            // If case is not in current working set, sum all instances (since they are not yet
            // executed).
            if (mInstanceListener.mKhronosCTSPendingResults.containsKey(testId)) {
                retVal += mInstanceListener.mKhronosCTSPendingResults.get(testId).remainingConfigs.size();
            } else {
                retVal += mTestInstances.get(testId).size();
            }
        }
        return retVal;
    }

    @Override
    protected String getId() {
        return AbiUtils.createId(mAbi.getName(), TEST_ID_NAME);
    }

    @Override
    protected String getRunConfigDisplayCmdLine(BatchRunConfiguration runConfig)
    {
        assert(runConfig instanceof KhronosCTSBatchRunConfiguration);
        return runConfig.getId();
    }

    private Set<KhronosCTSBatchRunConfiguration> getTestRunConfigs(TestDescription testId) {
        return mTestInstances.get(testId);
    }

    /**
    * Checks if a given test should be removed from the test KhronosCTS test run
    */
    private static boolean removeTestFromFilters(TestDescription test,
                                    List<String> includeFilters,
                                    List<String> excludeFilters) {
        // We could filter faster by building the test case tree.
        // Let's see if this is fast enough.
        Set<String> includeStrings = getNonPatternFilters(includeFilters);
        Set<String> excludeStrings = getNonPatternFilters(excludeFilters);
        List<Pattern> includePatterns = getPatternFilters(includeFilters);
        List<Pattern> excludePatterns = getPatternFilters(excludeFilters);
        if (excludeStrings.contains(test.toString())) {
            return true;
        }
        boolean includesExist = !includeStrings.isEmpty() || !includePatterns.isEmpty();
        boolean testIsIncluded = includeStrings.contains(test.toString())
                    || matchesAny(test, includePatterns);
        if ((includesExist && !testIsIncluded) || matchesAny(test, excludePatterns)) {
            // if this test isn't included and other tests are,
            // or if test matches exclude pattern, exclude test
            return true;
        }
        return false;
    }

    /**
     * Executes given test batch on a device
     */
    @Override
    protected void executeTestRunBatch(TestBatch batch) throws DeviceNotAvailableException
    {
        assert(batch instanceof KhronosCTSTestBatch);
        final String instrumentationName =
                "org.khronos.gl_cts/org.khronos.cts.testercore.KhronosCTSInstrumentation";
                final StringBuilder khronosCTSCmdLine = new StringBuilder();
        khronosCTSCmdLine.append("--deqp-caselist-file=");
        khronosCTSCmdLine.append(APP_DIR + CASE_LIST_FILE_NAME);
        khronosCTSCmdLine.append(" ");
        khronosCTSCmdLine.append(getRunConfigDisplayCmdLine(batch.getTestBatchConfig()));
        // If we are not logging data, do not bother outputting the images from the test exe.
        if (!mLogData) {
            khronosCTSCmdLine.append(" --deqp-log-images=disable");
        }
        final String command = String.format(
                "am instrument %s -w -e khronosCTSLogFileName \"%s\" -e khronosCTSCmdLine \"%s\" -e deqpLogData \"%s\" %s",
                AbiUtils.createAbiFlag(mAbi.getName()), APP_DIR + LOG_FILE_NAME, khronosCTSCmdLine.toString(), mLogData, instrumentationName);
        final KhronosCTSInstrumentationParser parser = new KhronosCTSInstrumentationParser(getInstanceListener());
        // attempt full run once
        executeTestRunBatchRun(batch, instrumentationName, command, parser);

        // split remaining tests to two sub batches and execute both. This will
        // terminate since executeTestRunBatchRun will always progress for a
        // batch of size 1.
        final ArrayList<TestDescription> pendingTests = new ArrayList<>();

        for (TestDescription test : batch.getTestBatchTestDescriptionList()) {
            if (getInstanceListener().isPendingTestInstance(test, batch.getTestBatchConfig())) {
                pendingTests.add(test);
            }
        }
        final int divisorNdx = pendingTests.size() / 2;
        final List<TestDescription> headList = pendingTests.subList(0, divisorNdx);
        final List<TestDescription> tailList = pendingTests.subList(divisorNdx, pendingTests.size());
        // head
        for (;;) {
            TestBatch subBatch = selectRunBatch(headList, batch.getTestBatchConfig());
            if (subBatch == null) {
                break;
            }
            executeTestRunBatch(subBatch);
        }
        // tail
        for (;;) {
            TestBatch subBatch = selectRunBatch(tailList, batch.getTestBatchConfig());
            if (subBatch == null) {
                break;
            }
            executeTestRunBatch(subBatch);
        }
        if (getBatchNumPendingCases(batch) != 0) {
            throw new AssertionError("executeTestRunBatch postcondition failed");
        }
    }

    /**
     * Runs a TestBatch by executing it on a device
     */
    @Override
    protected void runTestRunBatch(TestBatch batch) throws DeviceNotAvailableException{
        // prepare instance listener
        assert(batch instanceof KhronosCTSTestBatch);
        getInstanceListener().setCurrentConfig(batch.getTestBatchConfig());
        for (TestDescription test: batch.getTestBatchTestDescriptionList()) {
            mInstanceListener.setTestInstancesExperiment(test, getTestRunConfigs(test));
        }
        executeTestRunBatch(batch);
    }

    /**
     * Creates a KhronosCTSTestBatch from the given tests or null if not tests remaining.
     *
     *  @param pool List of tests to select from
     *  @param requiredConfig Select only instances with pending requiredConfig, or null to select
     *         any run configuration.
     */
    @Override
    protected TestBatch selectRunBatch(Collection<TestDescription> pool,
            BatchRunConfiguration requiredConfig) {
        assert(requiredConfig instanceof KhronosCTSBatchRunConfiguration);
        // select one test (leading test) that is going to be executed and then pack along as many
        // other compatible instances as possible.
        TestDescription leadingTest = null;
        for (TestDescription test : pool) {
            if (!mRemainingTests.contains(test)) {
                continue;
            }
            if (requiredConfig != null && !getInstanceListener().isPendingTestInstance(test, requiredConfig)) {
                continue;
            }
            leadingTest = test;
            break;
        }
        // no remaining tests?
        if (leadingTest == null) {
            return null;
        }
        BatchRunConfiguration leadingTestConfig = null;
        if (requiredConfig != null) {
            leadingTestConfig = requiredConfig;
        } else {
            for (KhronosCTSBatchRunConfiguration runConfig : getTestRunConfigs(leadingTest)) {
                if (getInstanceListener().isPendingTestInstance(leadingTest, runConfig)) {
                    leadingTestConfig = runConfig;
                    break;
                }
            }
        }
        // test pending <=> test has a pending config
        if (leadingTestConfig == null) {
            throw new AssertionError("search postcondition failed");
        }
        final int leadingInstability = getTestInstabilityRating(leadingTest);
        final KhronosCTSTestBatch runBatch = new KhronosCTSTestBatch();
        runBatch.setTestBatchConfig(leadingTestConfig);
        List<TestDescription> runBatchTests = new ArrayList<>();
        runBatchTests.add(leadingTest);
        for (TestDescription test : pool) {
            if (test == leadingTest) {
                // do not re-select the leading tests
                continue;
            }
            if (!getInstanceListener().isPendingTestInstance(test, leadingTestConfig)) {
                // select only compatible
                continue;
            }
            if (getTestInstabilityRating(test) != leadingInstability) {
                // pack along only cases in the same stability category. Packing more dangerous
                // tests along jeopardizes the stability of this run. Packing more stable tests
                // along jeopardizes their stability rating.
                continue;
            }
            if (runBatchTests.size() >= getBatchSizeLimitForInstability(leadingInstability)) {
                // batch size is limited.
                break;
            }
            runBatchTests.add(test);
        }
        runBatch.setTestBatchTestDescriptionList(runBatchTests);
        return runBatch;
    }

    /**
     * Executes all tests on the device.
     */
    private void runTests()
        throws DeviceNotAvailableException {
        for (;;) {
            TestBatch batch = selectRunBatch(mTestInstances.keySet(), null);

            if (batch == null) {
                break;
            }

            runTestRunBatch(batch);
        }
    }

    /**
     * helper function to read the test names from testlist and add them to the instances map
     */
    private void addTestsToInstancesMap(File testlist, KhronosCTSBatchRunConfiguration runConfig, Map<TestDescription, Set<KhronosCTSBatchRunConfiguration>> instances)
    {
        try (final FileReader testlistInnerReader = new FileReader(testlist);
        final BufferedReader testlistReader = new BufferedReader(testlistInnerReader)) {
            String testName;
            while ((testName = testlistReader.readLine()) != null) {
                testName = testName.trim();
                // Skip empty lines.
                if (testName.isEmpty()) {
                    continue;
                }
                // Lines starting with "#" are comments.
                if (testName.startsWith("#")) {
                    continue;
                }
                TestDescription test = pathToIdentifier(testName);
                if (removeTestFromFilters(test, mIncludeFilters, mExcludeFilters))
                {
                    continue;
                }
                Set<KhronosCTSBatchRunConfiguration> testInstanceConfigSet = instances.get(test);
                if (testInstanceConfigSet!=null)
                {
                    testInstanceConfigSet.add(runConfig);
                }
                else
                {
                    testInstanceConfigSet = new LinkedHashSet<>();
                    testInstanceConfigSet.add(runConfig);
                }
                assert testInstanceConfigSet != null;
                instances.put(test, testInstanceConfigSet);
            }
        }
        catch (IOException e)
        {
            throw new RuntimeException("Failure while reading the test case list: " + e.getMessage());
        }
    }

    /**
     * Helper function to load test names from caseListResourceFileName
     */
    private void LoadTestsFromCaselistResource(String caseListResourceFileName, KhronosCTSBatchRunConfiguration runConfig, Map<TestDescription, Set<KhronosCTSBatchRunConfiguration>> instances)
    {
        try {
            String[] paths = caseListResourceFileName.split("/");
            File directoryToSearch = mBuildHelper.getTestsDir();
            for (int i = 0; i < paths.length - 1; ++i)
            {
                File dir = FileUtil.findDirectory(paths[i], directoryToSearch);
                if (dir==null)
                {
                    throw new FileNotFoundException("Cannot find deqp test list file: "
                        + caseListResourceFileName);
                }
                directoryToSearch = dir;
            }
            String fileName = paths[paths.length-1];
            File testlist = new File(directoryToSearch, fileName);
            if (testlist == null || !testlist.isFile()) {
                    throw new FileNotFoundException("Cannot find deqp test list file: "
                        + caseListResourceFileName);
            }
            addTestsToInstancesMap(testlist, runConfig, instances);
        }
        catch (FileNotFoundException e) {
            throw new RuntimeException("Cannot read deqp test list file: " + caseListResourceFileName);
        }
        catch (IOException e) {
            throw new RuntimeException("Cannot read deqp test list file: " + caseListResourceFileName);
        }
    }

    private KhronosCTSBatchRunConfiguration parseRunParam(String runParam)
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

    private void generateTestInstanceWithTestRunParameters(List<String> testRunParamLists, Map<TestDescription, Set<KhronosCTSBatchRunConfiguration>> testInstancesExperiment) {
        final String testRunParamArgCaseListResource = "--deqp-caselist-resource";
        for (String testRunParam : testRunParamLists){
            CLog.i("Debug testRunParam is %s", testRunParam);
            int indexOfCaseListFileBegin = testRunParam.indexOf(testRunParamArgCaseListResource);
            if (indexOfCaseListFileBegin == -1)
            {
                continue;
            }
            int indexOfCaseListFileEnd = testRunParam.substring(indexOfCaseListFileBegin).indexOf(',');
            String caseListFileArgValue = testRunParam.substring(indexOfCaseListFileBegin, indexOfCaseListFileEnd);
            int equalIndex = caseListFileArgValue.indexOf('=');
            if (equalIndex == -1) {
                throw new RuntimeException("Invalid caseListFileArgValue. Accepted format is --deqp-caselist-resource=filename");
            }
            String caseListFileName = caseListFileArgValue.substring(equalIndex+1);
            String runParam = testRunParam.substring(indexOfCaseListFileEnd+1);
            KhronosCTSBatchRunConfiguration runConfig = parseRunParam(runParam);
            CLog.i("Debug runConfig is %s", runConfig.getId());
            LoadTestsFromCaselistResource(caseListFileName, runConfig, testInstancesExperiment);
        }
    }

    /**
    * Run Android activity org.khronos.cts.ES32GetTestParamActivity through instrumentation process
    */
    private void runGetTestsParamsActivity() throws DeviceNotAvailableException {
        if (mTestRunParams != null) throw new AssertionError("Re-load of test run params not supported");
        mTestRunParams = new ArrayList();
        checkInterrupted(); // throws if interrupted
        final String instrumentationName =
            "org.khronos.gl_cts/org.khronos.cts.testercore.KhronosCTSInstrumentation";
        final String getTestsParamsActivity =
            "org.khronos.gl_cts/org.khronos.cts.ES32GetTestParamActivity";
        final String testsParamsFileName =
            "/sdcard/cts-test-params.xml";
        final String command = String.format(
            "am instrument %s -w -e khronosCTSTestName \"%s\" -e khronosCTSTestParamFileName \"%s\" %s",
            AbiUtils.createAbiFlag(mAbi.getName()),
            getTestsParamsActivity,
            testsParamsFileName,
            instrumentationName);
        final KhronosCTSInstrumentationParser parser = new KhronosCTSInstrumentationParser(getInstanceListener());
        Throwable interruptingError = null;
        try {
            executeShellCommandAndReadOutput(command, parser);
        } catch (Throwable ex) {
            CLog.e("runGetTestsParamsActivity() executeShellCommandAndReadOutput() throws exception");
            interruptingError = ex;
        } finally {
            parser.flush();
        }
        // Check interruption error, e.g. adb device lost
        if (interruptingError != null)
        {
            if (interruptingError instanceof AdbComLinkOpenError) {
                CLog.e("runGetTestsParamsActivity() interruptingError is AdbComLinkOpenError");
                mDeviceRecovery.recoverConnectionRefused();
            } else if (interruptingError instanceof AdbComLinkKilledError) {
                CLog.e("runGetTestsParamsActivity() interruptingError is AdbComLinkKilledError");
                mDeviceRecovery.recoverComLinkKilled();
            } else if (interruptingError instanceof RunInterruptedException) {
                CLog.e("runGetTestsParamsActivity() interruptingError is RunInterruptedException");
                throw (RunInterruptedException)interruptingError;
            } else {
                CLog.e("runGetTestsParamsActivity() interruptingError is other error");
                CLog.e(interruptingError);
                throw new RuntimeException(interruptingError);
            }
        } else if (!parser.wasSuccessful()) {
            CLog.e("runGetTestsParamsActivity() parser.was not successful");
            mDeviceRecovery.recoverComLinkKilled();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void run(ITestInvocationListener listener) throws DeviceNotAvailableException {
        final HashMap<String, Metric> emptyMap = new HashMap<>();
        long startTime = System.currentTimeMillis();
        setupTestEnvironment(KHRONOS_CTS_ONDEVICE_PKG);
        try {
            mDeviceRecovery.setDevice(mDevice);
            // run the activity to retrieve the test run params first
            runGetTestsParamsActivity();

            // sanity check runGetTestsParamsActivity completed successfully
            if (mTestRunParams == null || mTestRunParams.isEmpty())
            {
                CLog.e("Debug Failed to load test run parameters");
                throw new RuntimeException("Failed to load test run parameters, abort the rest of tests");
            }

            if (mTestInstances != null) {
                throw new AssertionError("Re-load of tests experiment not supported");
            }

            // generate test instances map with the mTestRunParams
            mTestInstances = new LinkedHashMap<>();
            generateTestInstanceWithTestRunParameters(mTestRunParams, mTestInstances);
            CLog.e("Debug total test to run: %d", mTestInstances.size());
            mRemainingTests = new HashSet<>(mTestInstances.keySet());

        } catch (Exception ex) {
            CLog.e("Exception while generating test run parameters: %s", ex.getMessage());
            teardownTestEnvironment();
            return;
        }

        listener.testRunStarted(getId(), mRemainingTests.size());

        try {
            getInstanceListener().setSink(listener);
            runTests();
        } catch (Exception ex) {
            // Platform is not behaving correctly, for example crashing when trying to create
            // a window. Instead of silently failing, signal failure by leaving the rest of the
            // test cases in "NotExecuted" state
            CLog.e("Exception while running tests: %s", ex.getMessage());
        } finally {
            listener.testRunEnded(System.currentTimeMillis() - startTime, emptyMap);
            teardownTestEnvironment();
            return;
        }
    }
}
