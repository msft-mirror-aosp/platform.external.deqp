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

import com.drawelements.deqp.testercore.DeqpInstrumentation;
import com.drawelements.deqp.testercore.LogParser;
import com.drawelements.deqp.testercore.QpaParser;
import com.drawelements.deqp.testercore.TestLogParser;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import static org.junit.Assert.assertTrue;

@RunWith(RobolectricTestRunner.class)
public class LogParserFactoryTest {

    @Test
    public void testParserSelection_JavaParser() throws Exception {
        assertTrue(new LogParserFactoryImpl().create(DeqpInstrumentation.REPORTING_MODE_JAVA_LOG_PARSER)
                instanceof QpaParser);
    }

    @Test
    public void testParserSelection_NativeParser() throws Exception {
        LogParser parser = null;
        try {
            parser = new LogParserFactoryImpl().create(DeqpInstrumentation.REPORTING_MODE_NATIVE_LOG_PARSER);
        } catch (Error e) {
            // Expected in Robolectric due to missing native library in host environment.
            // It will throw either UnsatisfiedLinkError (first time) or NoClassDefFoundError (subsequent).
            assertTrue(e instanceof UnsatisfiedLinkError || e instanceof NoClassDefFoundError);
            return;
        }
        assertTrue(parser instanceof TestLogParser);
    }
}
