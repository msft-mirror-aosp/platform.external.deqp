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

import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expectLastCall;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;
import static org.junit.Assert.assertThrows;
import android.os.DeadObjectException;
import android.os.RemoteException;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

@RunWith(RobolectricTestRunner.class)
public class NativeTestEventBridgeTest {

    private ITestEventCallback mockCallback;
    private NativeTestEventBridge bridge;

    @Before
    public void setUp() {
        mockCallback = createMock(ITestEventCallback.class);
        bridge = new NativeTestEventBridge(mockCallback);
    }

    @Test
    public void testConstructor_nullCallbackThrows() {
        assertThrows(IllegalArgumentException.class, () -> new NativeTestEventBridge(null));
    }

    @Test
    public void testBeginSession_delegatesToCallback() throws RemoteException {
        mockCallback.beginSession();
        replay(mockCallback);

        bridge.beginSession();

        verify(mockCallback);
    }

    @Test
    public void testEndSession_delegatesToCallback() throws RemoteException {
        mockCallback.endSession();
        replay(mockCallback);

        bridge.endSession();

        verify(mockCallback);
    }

    @Test
    public void testSessionInfo_delegatesToCallback() throws RemoteException {
        mockCallback.sessionInfo("releaseName", "1.0");
        replay(mockCallback);

        bridge.sessionInfo("releaseName", "1.0");

        verify(mockCallback);
    }

    @Test
    public void testBeginTestCase_delegatesToCallback() throws RemoteException {
        mockCallback.beginTestCase("dEQP-GLES3.info.version");
        replay(mockCallback);

        bridge.beginTestCase("dEQP-GLES3.info.version");

        verify(mockCallback);
    }

    @Test
    public void testEndTestCase_delegatesToCallback() throws RemoteException {
        mockCallback.endTestCase();
        replay(mockCallback);

        bridge.endTestCase();

        verify(mockCallback);
    }

    @Test
    public void testTestCaseResult_delegatesToCallback() throws RemoteException {
        mockCallback.testCaseResult("Pass", "Details passed");
        replay(mockCallback);

        bridge.testCaseResult("Pass", "Details passed");

        verify(mockCallback);
    }

    @Test
    public void testTerminateTestCase_delegatesToCallback() throws RemoteException {
        mockCallback.terminateTestCase("Watchdog timeout");
        replay(mockCallback);

        bridge.terminateTestCase("Watchdog timeout");

        verify(mockCallback);
    }

    @Test
    public void testTestLogData_isNoOp() {
        replay(mockCallback);

        bridge.testLogData("<TestCaseResult>Pass</TestCaseResult>");

        verify(mockCallback);
    }

    @Test
    public void testCallbackExceptions_areCaughtAndNotPropagated() throws RemoteException {
        mockCallback.beginSession();
        expectLastCall().andThrow(new RemoteException("beginSession failed"));
        mockCallback.sessionInfo("name", "value");
        expectLastCall().andThrow(new RemoteException("sessionInfo failed"));
        mockCallback.beginTestCase("dEQP-GLES3.info.version");
        expectLastCall().andThrow(new RemoteException("beginTestCase failed"));
        mockCallback.testCaseResult("Fail", "Error");
        expectLastCall().andThrow(new RemoteException("testCaseResult failed"));
        mockCallback.endTestCase();
        expectLastCall().andThrow(new RemoteException("endTestCase failed"));
        mockCallback.terminateTestCase("Crash");
        expectLastCall().andThrow(new RemoteException("terminateTestCase failed"));
        mockCallback.endSession();
        expectLastCall().andThrow(new RemoteException("endSession failed"));
        replay(mockCallback);

        bridge.beginSession();
        bridge.sessionInfo("name", "value");
        bridge.beginTestCase("dEQP-GLES3.info.version");
        bridge.testCaseResult("Fail", "Error");
        bridge.endTestCase();
        bridge.terminateTestCase("Crash");
        bridge.endSession();

        verify(mockCallback);
    }

    @Test
    public void testDeadObjectException_dropsSubsequentEvents() throws RemoteException {
        mockCallback.beginSession();
        expectLastCall().andThrow(new DeadObjectException());
        replay(mockCallback);

        bridge.beginSession();
        bridge.sessionInfo("name", "value");
        bridge.beginTestCase("dEQP-GLES3.info.version");
        bridge.testCaseResult("Fail", "Error");
        bridge.endTestCase();
        bridge.terminateTestCase("Crash");
        bridge.endSession();

        verify(mockCallback);
    }

    @Test
    public void testNonDeadRemoteException_keepsForwardingEvents() throws RemoteException {
        mockCallback.beginTestCase("dEQP-GLES3.info.version");
        expectLastCall().andThrow(new RemoteException("transient failure"));
        mockCallback.testCaseResult("Pass", "Details passed");
        replay(mockCallback);

        bridge.beginTestCase("dEQP-GLES3.info.version");
        bridge.testCaseResult("Pass", "Details passed");

        verify(mockCallback);
    }
}
