
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
package org.khronos.cts.testercore;
import android.app.Instrumentation;
import android.os.Bundle;
import java.lang.Boolean;
import java.lang.Thread;
import java.io.File;
public class KhronosCTSInstrumentation extends Instrumentation{
    private static final String LOG_TAG = "KhronosCTS";
    private static final long LAUNCH_TIMEOUT_MS = 10000;
    private static final long NO_DATA_TIMEOUT_MS = 5000;
    private static final long NO_ACTIVITY_SLEEP_MS = 100;
    private static final long REMOTE_DEAD_SLEEP_MS = 100;
    private String m_cmdLine;
    private String m_logFileName;
    private String m_testParamFileName;
    private String m_testName;
    private boolean m_logData;
    private void deleteDirectory(File directoryName)
    {
        // delete the files and sub-directories under the directoryName
        for (File file : directoryName.listFiles())
        {
            if (file.isDirectory())
            {
                deleteDirectory(file);
            }
            else
            {
                file.delete();
            }
        }
        // check the directoryName is an empty directory
        assert directoryName.listFiles().length == 0;
        // delete the directoryName
        directoryName.delete();
    }
    @Override
    public void onCreate (Bundle arguments) {
        super.onCreate(arguments);
        m_cmdLine = arguments.getString("khronosCTSCmdLine");
        m_logFileName = arguments.getString("khronosCTSLogFileName");
        m_testParamFileName = arguments.getString("khronosCTSTestParamFileName");
        m_testName = arguments.getString("khronosCTSTestName");
        if (m_cmdLine == null)
            m_cmdLine = "";
        if (m_logFileName == null)
            m_logFileName = "";
        if (m_testParamFileName == null)
            m_testParamFileName = "";
        if (m_testName == null)
            m_testName = "org.khronos.gl_cts/android.app.NativeActivity";
        if(arguments.getString("khronosCTSLogData") != null){
            if (arguments.getString("khronosCTSLogData").compareToIgnoreCase("true") == 0)
                m_logData = true;
            else
                m_logData = false;
        }
        else{
            m_logData = false;
        }
        start();
    }
    @Override
    public void onStart() {
        super.onStart();
        final KhronosCTSRemoteAPI khronosCTSRemoteApi = new KhronosCTSRemoteAPI(getTargetContext(), m_logFileName, m_testParamFileName);
        //final KhronosCTSTestLogFileManager testLogFileManager = new KhronosCTSTestLogFileManager();
        final KhronosCTSTestLogParser khronosCTSTestLogParser = new KhronosCTSTestLogParser();
        try
        {
            KhronosCTSLog.d(LOG_TAG, "onStart");
            String fileToParse = "";
            if (m_testName.equals("org.khronos.gl_cts/org.khronos.cts.ES32GetTestParamActivity"))
            {
                if (m_testParamFileName.isEmpty())
                {
                    throw new Exception ("activity org.khronos.cts.ES32GetTestParamActivity requires khronosCTSTestParamFileName arg");
                }
                fileToParse = m_testParamFileName;
            }
            else
            {
                if (m_logFileName.isEmpty())
                {
                    throw new Exception ("activity android.app.NativeActivity requires khronosCTSLogFileName arg");
                }
                fileToParse = m_logFileName;
            }
            final File logFile = new File(fileToParse);
            if (logFile.exists())
                logFile.delete();
            khronosCTSRemoteApi.start(m_testName, m_cmdLine);
            {
                final long startTimeMs = System.currentTimeMillis();
                while (true)
                {
                    final long timeSinceStartMs = System.currentTimeMillis() - startTimeMs;
                    if (logFile.exists())
                    {
                        break;
                    }
                    else if (timeSinceStartMs > LAUNCH_TIMEOUT_MS)
                    {
                        khronosCTSRemoteApi.kill();
                        throw new Exception ("Timeout while waiting for log file directory");
                    }
                    else
                    {
                        Thread.sleep(NO_ACTIVITY_SLEEP_MS);
                    }
                }
            }
            khronosCTSTestLogParser.init(this, fileToParse, m_logData);
            // parse until tester dies
            {
                while (true)
                {
                    if (!khronosCTSTestLogParser.parse())
                    {
                        Thread.sleep(NO_ACTIVITY_SLEEP_MS);
                        if(!khronosCTSRemoteApi.isRunning())
                            break;
                    }
                }
            }
            // parse remaining messages
            {
                long lastDataMs = System.currentTimeMillis();
                while (true)
                {
                    if (khronosCTSTestLogParser.parse())
                    {
                        lastDataMs = System.currentTimeMillis();
                    }
                    else
                    {
                        final long timeSinceLastDataMs = System.currentTimeMillis()-lastDataMs;
                        if (timeSinceLastDataMs > NO_DATA_TIMEOUT_MS)
                            break; // Assume no data is available for reading any more
                        // Remote is dead, wait a bit until trying to read again
                        Thread.sleep(REMOTE_DEAD_SLEEP_MS);
                    }
                }
            }
            finish(0, new Bundle());
        }
        catch (Exception e)
        {
            KhronosCTSLog.e(LOG_TAG, "Exception", e);
            Bundle info = new Bundle();
            info.putString("Exception", e.getMessage());
            finish(1, info);
        }
        finally{
            try {
                khronosCTSTestLogParser.deinit();
            } catch (Exception e) {
                KhronosCTSLog.w(LOG_TAG, "Got exception while closing log", e);
            }
            khronosCTSRemoteApi.kill();
        }
    }
    public void testCaseResult (String code, String details)
    {
        Bundle info = new Bundle();
        info.putString("dEQP-EventType", "TestCaseResult");
        info.putString("dEQP-TestCaseResult-Code", code);
        info.putString("dEQP-TestCaseResult-Details", details);
        sendStatus(0, info);
    }
    public void beginTestCase (String testCase)
    {
        Bundle info = new Bundle();
        info.putString("dEQP-EventType", "BeginTestCase");
        info.putString("dEQP-BeginTestCase-TestCasePath", testCase);
        sendStatus(0, info);
    }
    public void endTestCase ()
    {
        Bundle info = new Bundle();
        info.putString("dEQP-EventType", "EndTestCase");
        sendStatus(0, info);
    }
    public void testLogData (String log) throws InterruptedException
    {
        if (m_logData)
        {
            final int chunkSize = 4*1024;
            while (log != null)
            {
                String message;
                if (log.length() > chunkSize)
                {
                    message = log.substring(0, chunkSize);
                    log = log.substring(chunkSize);
                }
                else
                {
                    message = log;
                    log = null;
                }
                Bundle info = new Bundle();
                info.putString("dEQP-EventType", "TestLogData");
                info.putString("dEQP-TestLogData-Log", message);
                sendStatus(0, info);
                if (log != null)
                {
                    Thread.sleep(1); // 1ms
                }
            }
        }
    }
    public void beginTestRunParamsCollection()
    {
        Bundle info = new Bundle();
        info.putString("dEQP-EventType", "BeginTestRunParamsCollection");
        sendStatus(0, info);
    }
    public void endTestRunParamsCollection()
    {
        Bundle info = new Bundle();
        info.putString("dEQP-EventType", "EndTestRunParamsCollection");
        sendStatus(0, info);
    }
    public void beginTestRunParams(String testRunParams)
    {
        Bundle info = new Bundle();
        info.putString("dEQP-EventType", "BeginTestRunParams");
        info.putString("dEQP-TestRunParam", testRunParams);
        sendStatus(0, info);
    }
    public void endTestRunParams()
    {
        Bundle info = new Bundle();
        info.putString("dEQP-EventType", "EndTestRunParams");
        sendStatus(0, info);
    }
    public void beginSession ()
    {
        Bundle info = new Bundle();
        info.putString("dEQP-EventType", "BeginSession");
        sendStatus(0, info);
    }
    public void endSession ()
    {
        Bundle info = new Bundle();
        info.putString("dEQP-EventType", "EndSession");
        sendStatus(0, info);
    }
    public void sessionInfo (String name, String value)
    {
        Bundle info = new Bundle();
        info.putString("dEQP-EventType", "SessionInfo");
        info.putString("dEQP-SessionInfo-Name", name);
        info.putString("dEQP-SessionInfo-Value", value);
        sendStatus(0, info);
    }
    public void terminateTestCase (String reason)
    {
        Bundle info = new Bundle();
        info.putString("dEQP-EventType", "TerminateTestCase");
        info.putString("dEQP-TerminateTestCase-Reason", reason);
        sendStatus(0, info);
    }
    @Override
    public void onDestroy() {
        KhronosCTSLog.e(LOG_TAG, "onDestroy");
        super.onDestroy();
    }
}
