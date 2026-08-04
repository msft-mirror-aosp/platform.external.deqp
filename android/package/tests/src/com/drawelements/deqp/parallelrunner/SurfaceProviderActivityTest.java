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
import android.content.Context;
import android.content.Intent;
import android.view.SurfaceView;
import android.view.View;
import android.widget.GridLayout;
import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import java.io.File;
import java.io.IOException;
import java.util.Queue;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;

/**
 * Unit tests for {@link SurfaceProviderActivity} using ActivityScenario.
 * These tests verify the dynamic UI generation logic of the SurfaceProviderActivity.
 */
@RunWith(AndroidJUnit4.class)
public class SurfaceProviderActivityTest {

    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();


    private static final String TEST_CASELIST_DIR = "/sdcard/deqpparallel/caselists/";
    private static final String TEST_LOG_DIR = "/sdcard/deqpparallel/logs/";
    private static final String TEST_CMD_LINE = "--deqp-gl-config-name=rgba8888d24s8 --deqp-watchdog=enable";

    private GridLayout getAndVerifySurfaceGrid(SurfaceProviderActivity activity, int expectedWorkers) {
        View contentView = activity.findViewById(android.R.id.content);
        assertNotNull(contentView);
        assertTrue(contentView instanceof android.view.ViewGroup);
        android.view.ViewGroup viewGroup = (android.view.ViewGroup) contentView;
        assertTrue(viewGroup.getChildCount() > 0);

        View firstChild = viewGroup.getChildAt(0);
        assertTrue(firstChild instanceof GridLayout);
        GridLayout gridLayout = (GridLayout) firstChild;

        assertEquals(expectedWorkers, gridLayout.getChildCount());
        for (int i = 0; i < expectedWorkers; i++) {
            assertTrue(gridLayout.getChildAt(i) instanceof SurfaceView);
        }
        return gridLayout;
    }

    private void assertGridGeometryTiling(GridLayout gridLayout, int expectedChildCount) {
        // Force measure and layout to populate absolute physical view boundary coordinates
        int screenWidth = 1080;
        int screenHeight = 1920;
        gridLayout.measure(
            View.MeasureSpec.makeMeasureSpec(screenWidth, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(screenHeight, View.MeasureSpec.EXACTLY)
        );
        gridLayout.layout(0, 0, screenWidth, screenHeight);

        assertEquals(expectedChildCount, gridLayout.getChildCount());
        if (expectedChildCount <= 0) return;

        View firstChild = gridLayout.getChildAt(0);
        int targetWidth = firstChild.getWidth();
        int targetHeight = firstChild.getHeight();
        assertTrue("Child window width must be greater than zero", targetWidth > 0);
        assertTrue("Child window height must be greater than zero", targetHeight > 0);

        int maxRight = firstChild.getRight();
        int maxBottom = firstChild.getBottom();

        for (int i = 1; i < expectedChildCount; i++) {
            View prev = gridLayout.getChildAt(i - 1);
            View curr = gridLayout.getChildAt(i);

            // 1. Validate uniform sizing across all child contexts (allowing minor integer rounding variances)
            assertTrue("Every child window should share uniform pixel width",
                Math.abs(curr.getWidth() - targetWidth) <= 2);
            assertTrue("Every child window should share uniform pixel height",
                Math.abs(curr.getHeight() - targetHeight) <= 2);

            // 2. Validate spatial adjacency/tiling constraints
            boolean isDirectlyRight = (curr.getLeft() == prev.getRight()) && (curr.getTop() == prev.getTop());
            boolean isNewRowBelow = (curr.getLeft() == 0) && (curr.getTop() == prev.getBottom());

            assertTrue("Child window " + i + " must geometrically tile directly relative to its preceding neighbor",
                isDirectlyRight || isNewRowBelow);

            maxRight = Math.max(maxRight, curr.getRight());
            maxBottom = Math.max(maxBottom, curr.getBottom());
        }

        // 3. Validate global container boundaries packing
        assertTrue("The populated child layout must tightly fill the screen width",
            Math.abs(screenWidth - maxRight) <= 2);
        assertTrue("The populated child layout must tightly fill the screen height",
            Math.abs(screenHeight - maxBottom) <= 2);
    }

    @Test
    public void testDefaultWorkersLaunch() {
        try (ActivityScenario<SurfaceProviderActivity> scenario = ActivityScenario.launch(SurfaceProviderActivity.class)) {
            scenario.onActivity(activity -> {
                GridLayout gridLayout = getAndVerifySurfaceGrid(activity, ParallelRunnerConfig.DEFAULT_MAX_WORKERS);
                assertGridGeometryTiling(gridLayout, ParallelRunnerConfig.DEFAULT_MAX_WORKERS);
            });
        }
    }

    @Test
    public void testVariousWorkerCountsGrid() {
        Context context = ApplicationProvider.getApplicationContext();
        int[] countsToTest = {4, 6, 7, 9};

        for (int workers : countsToTest) {
            Intent intent = SurfaceProviderActivity.createIntent(context, workers, TEST_CASELIST_DIR, TEST_LOG_DIR, TEST_CMD_LINE);

            try (ActivityScenario<SurfaceProviderActivity> scenario = ActivityScenario.launch(intent)) {
                scenario.onActivity(activity -> {
                    GridLayout gridLayout = getAndVerifySurfaceGrid(activity, workers);
                    assertGridGeometryTiling(gridLayout, workers);
                });
            }
        }
    }

    @Test
    public void testInvalidWorkerCountFallback() {
        Context context = ApplicationProvider.getApplicationContext();
        Intent intent = SurfaceProviderActivity.createIntent(context, -1, TEST_CASELIST_DIR, TEST_LOG_DIR, TEST_CMD_LINE);

        try (ActivityScenario<SurfaceProviderActivity> scenario = ActivityScenario.launch(intent)) {
            scenario.onActivity(activity -> {
                GridLayout gridLayout = getAndVerifySurfaceGrid(activity, ParallelRunnerConfig.DEFAULT_MAX_WORKERS);
                assertGridGeometryTiling(gridLayout, ParallelRunnerConfig.DEFAULT_MAX_WORKERS);
            });
        }
    }

    @Test
    public void testCalculateOptimalGrid() {
        // --- 6 Workers ---
        SurfaceProviderActivity.GridSize squareGrid6 = SurfaceProviderActivity.calculateOptimalGrid(6, 1.0);
        assertEquals(3, squareGrid6.columns);
        assertEquals(2, squareGrid6.rows);

        SurfaceProviderActivity.GridSize portraitGrid6 = SurfaceProviderActivity.calculateOptimalGrid(6, 0.5625);
        assertEquals(2, portraitGrid6.columns);
        assertEquals(3, portraitGrid6.rows);

        SurfaceProviderActivity.GridSize landscapeGrid6 = SurfaceProviderActivity.calculateOptimalGrid(6, 1.777);
        assertEquals(4, landscapeGrid6.columns);
        assertEquals(2, landscapeGrid6.rows);

        // --- 7 Workers ---
        SurfaceProviderActivity.GridSize squareGrid7 = SurfaceProviderActivity.calculateOptimalGrid(7, 1.0);
        assertEquals(3, squareGrid7.columns);
        assertEquals(3, squareGrid7.rows);

        SurfaceProviderActivity.GridSize portraitGrid7 = SurfaceProviderActivity.calculateOptimalGrid(7, 0.5625);
        assertEquals(2, portraitGrid7.columns);
        assertEquals(4, portraitGrid7.rows);

        SurfaceProviderActivity.GridSize landscapeGrid7 = SurfaceProviderActivity.calculateOptimalGrid(7, 1.777);
        assertEquals(4, landscapeGrid7.columns);
        assertEquals(2, landscapeGrid7.rows);

        // --- 9 Workers ---
        SurfaceProviderActivity.GridSize squareGrid9 = SurfaceProviderActivity.calculateOptimalGrid(9, 1.0);
        assertEquals(3, squareGrid9.columns);
        assertEquals(3, squareGrid9.rows);

        SurfaceProviderActivity.GridSize portraitGrid9 = SurfaceProviderActivity.calculateOptimalGrid(9, 0.5625);
        assertEquals(2, portraitGrid9.columns);
        assertEquals(5, portraitGrid9.rows);

        SurfaceProviderActivity.GridSize landscapeGrid9 = SurfaceProviderActivity.calculateOptimalGrid(9, 1.777);
        assertEquals(5, landscapeGrid9.columns);
        assertEquals(2, landscapeGrid9.rows);
    }

    @Test
    public void testCaselistLoadingFromDirectory() throws IOException {
        Context context = ApplicationProvider.getApplicationContext();

        File tempDir = tempFolder.newFolder("deqp_temp_caselists");

        File file10 = new File(tempDir, "caselist_10.txt");
        File file2 = new File(tempDir, "caselist_2.txt");
        File file1 = new File(tempDir, "caselist_1.txt");

        file10.createNewFile();
        file2.createNewFile();
        file1.createNewFile();

        Intent intent = SurfaceProviderActivity.createIntent(context, ParallelRunnerConfig.DEFAULT_MAX_WORKERS,
            tempDir.getAbsolutePath(), TEST_LOG_DIR, TEST_CMD_LINE);

        try (ActivityScenario<SurfaceProviderActivity> scenario = ActivityScenario.launch(intent)) {
            // Wait briefly to allow async background thread loading to execute
            int maxRetries = 40; 
            while (maxRetries > 0) {
                final int[] currentSize = new int[1];
                scenario.onActivity(activity -> {
                    if (activity.getBatchQueue() != null) {
                        currentSize[0] = activity.getBatchQueue().size();
                    }
                });
                
                if (currentSize[0] == 3) {
                    break;
                }
                Thread.sleep(50);
                maxRetries--;
            }
            scenario.onActivity(activity -> {
                Queue<String> batchQueue = activity.getBatchQueue();
                assertNotNull(batchQueue);
                assertEquals(3, batchQueue.size());

                // Verify files are sorted numerically
                assertEquals(file1.getAbsolutePath(), batchQueue.poll());
                assertEquals(file2.getAbsolutePath(), batchQueue.poll());
                assertEquals(file10.getAbsolutePath(), batchQueue.poll());
            });
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}
