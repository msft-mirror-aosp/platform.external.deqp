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

import android.app.Activity;
import android.content.Intent;
import android.graphics.Rect;
import android.os.Build;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.widget.GridLayout;
import java.io.File;
import java.util.Queue;

/**
 * SurfaceProviderActivity serves as the orchestrator and UI container for the parallel test runner.
 * It dynamically generates a grid layout containing multiple {@link SurfaceView}s,
 * each corresponding to an independent rendering worker managed by {@link ParallelTestsScheduler}.
 */
public class SurfaceProviderActivity extends Activity {
    private static final String TAG = "SurfaceProviderActivity";
    static final String EXTRA_MAX_WORKERS = "extra_max_workers";
    static final String EXTRA_TEST_BATCHES_DIR = "extra_test_batches_dir";
    static final String DEFAULT_TEST_BATCHES_DIR;

    static {
        DEFAULT_TEST_BATCHES_DIR = new File(
            android.os.Environment.getExternalStorageDirectory(),
            "deqpparallel/caselists/"
        ).getAbsolutePath();
    }

    private GridLayout workerGridLayout;
    private final DeqpTestBatchLoader mTestBatchLoader = new DeqpTestBatchLoader();
    private ParallelTestsScheduler scheduler;
    private Thread loaderThread;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        workerGridLayout = new GridLayout(this);
        setContentView(workerGridLayout);

        int workerCount = ParallelRunnerConfig.DEFAULT_MAX_WORKERS;
        if (getIntent() != null && getIntent().hasExtra(EXTRA_MAX_WORKERS)) {
            workerCount = getIntent().getIntExtra(EXTRA_MAX_WORKERS, ParallelRunnerConfig.DEFAULT_MAX_WORKERS);
        }

        if (workerCount <= 0) {
            Log.w(TAG, "Invalid workerCount: " + workerCount + ". Defaulting to 1.");
            workerCount = 1;
        }

        if (workerCount > ParallelRunnerConfig.MAX_ALLOWED_WORKERS) {
            Log.w(TAG, "workerCount " + workerCount + " exceeds maximum allowed. Clamping to " + ParallelRunnerConfig.MAX_ALLOWED_WORKERS + ".");
            workerCount = ParallelRunnerConfig.MAX_ALLOWED_WORKERS;
        }

        String testBatchesDir = DEFAULT_TEST_BATCHES_DIR;
        if (getIntent() != null && getIntent().hasExtra(EXTRA_TEST_BATCHES_DIR)) {
            testBatchesDir = getIntent().getStringExtra(EXTRA_TEST_BATCHES_DIR);
        }

        final int finalWorkerCount = workerCount;
        final String finalTestBatchesDir = testBatchesDir;

        // Maintain reference to loader thread for proper cleanup in onDestroy()
        loaderThread = new Thread(() -> {
            mTestBatchLoader.loadFromDirectory(finalTestBatchesDir);

            runOnUiThread(() -> {
                if (isDestroyed() || isFinishing()) {
                    return;
                }

                // Initialize the scheduler safely AFTER batches are loaded
                scheduler = new ParallelTestsScheduler(this, finalWorkerCount, mTestBatchLoader, new ParallelTestsScheduler.Callback() {
                    @Override
                    public void onAllTestsCompleted() {
                        Log.i(TAG, "All rendering workers finished execution. Completing activity.");
                        runOnUiThread(() -> {
                            if (!isDestroyed() && !isFinishing()) {
                                SurfaceProviderActivity.this.finish();
                            }
                        });
                    }
                });

                // Generate views only when scheduler is ready to receive callbacks
                generateSurfaceViews(finalWorkerCount);
            });
        });

        loaderThread.start();
    }

    /**
     * Gets the in-memory queue of loaded pre-split batch file paths.
     */
    public Queue<String> getBatchQueue() {
        return mTestBatchLoader.getBatchQueue();
    }

    static class GridSize {
        final int columns;
        final int rows;

        GridSize(int columns, int rows) {
            this.columns = columns;
            this.rows = rows;
        }
    }

    /**
     * Dynamically populates the root grid container with native rendering viewports tailored to the active window bounds.
     *
     * To prevent structural frame scaling or clipping issues during headless parallel graphics execution, this method
     * evaluates the host window's physical aspect ratio to calculate a deterministic column and row partition. By
     * driving layout selection to minimize aspect deviation from an ideal 1:1 square context, every spawned
     * {@link SurfaceView} achieves maximum rendering precision across arbitrary device form factors.
     *
     * @param workerCount The targeted threshold of independent parallel surface cells to populate.
     */
    private void generateSurfaceViews(int workerCount) {
        Rect bounds = getWindowBounds();
        double windowAspectRatio = bounds.height() > 0 ? (double) bounds.width() / bounds.height() : 1.0;
        GridSize grid = calculateOptimalGrid(workerCount, windowAspectRatio);

        Log.i(TAG, String.format("Generated grid: %d columns, %d rows for %d workers. Window aspect: %.2f",
            grid.columns, grid.rows, workerCount, windowAspectRatio));

        workerGridLayout.setColumnCount(grid.columns);
        workerGridLayout.setRowCount(grid.rows);

        for (int i = 0; i < workerCount; i++) {
            SurfaceView surfaceView = new SurfaceView(this);

            // Let the scheduler register its callback to the surface view holder
            scheduler.registerSurface(i, surfaceView.getHolder());

            // Relies on truncating integer division for deterministic row assignment
            GridLayout.Spec rowSpec = GridLayout.spec(i / grid.columns, GridLayout.FILL, 1.0f);
            GridLayout.Spec colSpec = GridLayout.spec(i % grid.columns, GridLayout.FILL, 1.0f);
            GridLayout.LayoutParams params = new GridLayout.LayoutParams(rowSpec, colSpec);
            params.width = 0;
            params.height = 0;

            surfaceView.setLayoutParams(params);
            workerGridLayout.addView(surfaceView);
        }
    }

    static GridSize calculateOptimalGrid(int workerCount, double containerAspectRatio) {
        int bestColumns = 1;
        int bestRows = workerCount;
        double minAspectDiff = Double.MAX_VALUE;

        for (int c = 1; c <= workerCount; c++) {
            int r = (int) Math.ceil((double) workerCount / c);

            // Calculate the physical aspect ratio of an individual rendering buffer
            double cellAspectRatio = containerAspectRatio * ((double) r / c);

            // 1.0 represents a flawless 1:1 square rendering context
            double currentAspectDiff = Math.abs(cellAspectRatio - 1.0);

            if (currentAspectDiff < minAspectDiff) {
                minAspectDiff = currentAspectDiff;
                bestColumns = c;
                bestRows = r;
            }
        }
        return new GridSize(bestColumns, bestRows);
    }

    private Rect getWindowBounds() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            return getWindowManager().getCurrentWindowMetrics().getBounds();
        } else {
            DisplayMetrics metrics = new DisplayMetrics();
            getWindowManager().getDefaultDisplay().getMetrics(metrics);
            return new Rect(0, 0, metrics.widthPixels, metrics.heightPixels);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (loaderThread != null && loaderThread.isAlive()) {
            loaderThread.interrupt();
            loaderThread = null;
        }

        if (scheduler != null) {
            scheduler.shutdown();
            scheduler = null;
        }
    }
}
