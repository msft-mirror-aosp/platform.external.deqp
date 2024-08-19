
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
import android.app.ActivityManager;
import android.content.Context;
import android.content.ComponentName;
import android.content.Intent;
import android.os.Process;
import java.util.List;
public class KhronosCTSRemoteAPI{
    private static final String LOG_TAG = "KhronosCTS";
    private Context    m_context;
    private String     m_processName;
    private String     m_logFileName;
    private String     m_testParamFileName;
    private boolean    m_isTestProcessStarted;
    public KhronosCTSRemoteAPI (Context context, String logFileName, String testParamFileName) {
        m_context = context;
        m_processName = m_context.getPackageName() + ":testercore";
        m_logFileName = logFileName;
        m_testParamFileName = testParamFileName;
        m_isTestProcessStarted = false;
    }
    private ComponentName getDefaultTestComponent () {
        return new ComponentName(m_context.getPackageName(), "android.app.NativeActivity");
    }
    private ComponentName getTestComponent(String testName) {
        if (testName != null && !testName.equals("")) {
            ComponentName component = ComponentName.unflattenFromString(testName);
            if (component == null) {
                KhronosCTSLog.e(LOG_TAG, "Invalid component name supplied (" + testName + "), using default");
                component = getDefaultTestComponent();
            }
            return component;
        }
        else {
            return getDefaultTestComponent();
        }
    }
    public boolean start (String testerName, String cmdLine) {
        KhronosCTSLog.w(LOG_TAG, "KhronosCTSRemoteAPI start: " + testerName);
        // Choose component
        ComponentName component = getTestComponent(testerName);
        Intent testIntent = new Intent();
        testIntent.setComponent(component);
        testIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        // Add all data to cmdLine
        cmdLine = testerName + " " + cmdLine;
        if (!m_logFileName.isEmpty())
        {
            cmdLine += " --deqp-log-filename=" + m_logFileName;
        }
        cmdLine = cmdLine.replaceAll("  ", " ");
        testIntent.putExtra("cmdLine", cmdLine);
        if (!m_testParamFileName.isEmpty())
        {
            testIntent.putExtra("khronosCTSTestParamFileName", m_testParamFileName);
        }
        // Try to resolve intent.
        boolean isActivity = m_context.getPackageManager().resolveActivity(testIntent, 0) != null;
        if (!isActivity) {
            KhronosCTSLog.e(LOG_TAG, "Can't resolve component as activity (" + component.flattenToString() + "), using default");
            component = getDefaultTestComponent();
        }
        try {
            m_context.startActivity(testIntent);
        } catch (Exception e) {
            KhronosCTSLog.e(LOG_TAG, "Failed to start test", e);
            return false;
        }
        m_isTestProcessStarted = true;
        return true;
    }
    public boolean kill() {
        ActivityManager.RunningAppProcessInfo processInfo = findProcess(m_processName);
        // \note not mutating m_isTestProcessStarted yet since process does not die immediately
        if (processInfo != null) {
            KhronosCTSLog.d(LOG_TAG, "Killing " + m_processName);
            Process.killProcess(processInfo.pid);
            return true;
        } else {
            return false;
        }
    }
    public boolean isRunning() {
        if (!m_isTestProcessStarted) {
            return false;
        } else if (isProcessRunning(m_processName)) {
            return true;
        } else {
            // Cache result. Safe, because only start() can spawn the process
            m_isTestProcessStarted = false;
            return false;
        }
    }
    public String getLogFileName() {
        return m_logFileName;
    }
    private ActivityManager.RunningAppProcessInfo findProcess (String name) {
        ActivityManager activityMgr = (ActivityManager)m_context.getSystemService(Context.ACTIVITY_SERVICE);
        List<ActivityManager.RunningAppProcessInfo> processes = activityMgr.getRunningAppProcesses();
        for (ActivityManager.RunningAppProcessInfo info : processes) {
            KhronosCTSLog.d(LOG_TAG, "Found proc : " + info.processName + " " + info.pid
                                        + " name of the target process: " + name );
            if (info.processName.equals(name))
                return info;
        }
        return null;
    }
    private boolean isProcessRunning (String processName) {
        return (findProcess(processName) != null);
    }
}
