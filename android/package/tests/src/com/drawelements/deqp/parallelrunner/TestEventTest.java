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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import android.os.Bundle;
import java.util.List;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

@RunWith(RobolectricTestRunner.class)
public class TestEventTest {

    @Test
    public void testEmptyEvent() {
        TestEvent event = new TestEvent();
        List<Bundle> bundles = event.getBundles();
        assertNotNull(bundles);
        assertTrue(bundles.isEmpty());
    }

    @Test
    public void testAddBundle() {
        TestEvent event = new TestEvent();
        Bundle bundle1 = new Bundle();
        bundle1.putString("key1", "value1");

        event.addBundle(bundle1);

        List<Bundle> bundles = event.getBundles();
        assertEquals(1, bundles.size());
        assertEquals("value1", bundles.get(0).getString("key1"));
    }

    @Test
    public void testAddNullBundle_ignoresNull() {
        TestEvent event = new TestEvent();
        event.addBundle(null);

        List<Bundle> bundles = event.getBundles();
        assertTrue(bundles.isEmpty());
    }

    @Test
    public void testGetBundles_returnsCopy() {
        TestEvent event = new TestEvent();
        Bundle bundle1 = new Bundle();
        event.addBundle(bundle1);

        List<Bundle> bundles = event.getBundles();
        assertEquals(1, bundles.size());

        bundles.clear(); // Modifying the returned copy shouldn't modify the original

        assertEquals(1, event.getBundles().size());
    }
}
