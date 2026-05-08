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

/**
 * SurfaceProviderActivity serves as the orchestrator and UI container for the parallel test runner.
 * It dynamically generates a grid layout containing multiple {@link SurfaceView}s,
 * each corresponding to an independent rendering worker.
 *
 * It implements {@link SurfaceLifecycleListener} to monitor when the raw surfaces
 * are created, changed, or destroyed, allowing it to dispatch these events
 * to background rendering threads.
 *
 * The number of parallel workers can be configured dynamically via Intent extras
 * using {@code "extra_max_workers"}.
 */
public class SurfaceProviderActivity extends Activity implements SurfaceLifecycleListener {
    private static final String TAG = "SurfaceProviderActivity";
    static final String EXTRA_MAX_WORKERS = "extra_max_workers";
    static final int DEFAULT_MAX_WORKERS = 4;
    static final int MAX_ALLOWED_WORKERS = 12;

    private GridLayout surfaceGrid;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        surfaceGrid = new GridLayout(this);
        setContentView(surfaceGrid);

        int maxWorkers = DEFAULT_MAX_WORKERS;
        if (getIntent() != null && getIntent().hasExtra(EXTRA_MAX_WORKERS)) {
            maxWorkers = getIntent().getIntExtra(EXTRA_MAX_WORKERS, DEFAULT_MAX_WORKERS);
        }

        if (maxWorkers <= 0) {
            Log.w(TAG, "Invalid maxWorkers count: " + maxWorkers + ". Defaulting to 1.");
            maxWorkers = 1;
        }

        if (maxWorkers > MAX_ALLOWED_WORKERS) {
            Log.w(TAG, "maxWorkers count " + maxWorkers + " exceeds maximum allowed. Clamping to " + MAX_ALLOWED_WORKERS + ".");
            maxWorkers = MAX_ALLOWED_WORKERS;
        }

        generateSurfaceViews(maxWorkers);
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
     * @param maxWorkers The targeted threshold of independent parallel surface cells to populate.
     */
    private void generateSurfaceViews(int maxWorkers) {
        Rect bounds = getWindowBounds();
        double windowAspectRatio = bounds.height() > 0 ? (double) bounds.width() / bounds.height()
            : 1.0;
        GridSize grid = calculateOptimalGrid(maxWorkers, windowAspectRatio);

        Log.i(TAG, String.format("Generated grid: %d columns, %d rows for %d workers. Window aspect: %.2f",
            grid.columns, grid.rows, maxWorkers, windowAspectRatio));

        surfaceGrid.setColumnCount(grid.columns);
        surfaceGrid.setRowCount(grid.rows);

        for (int i = 0; i < maxWorkers; i++) {
            SurfaceView surfaceView = new SurfaceView(this);

            // Note: Ensure SafeSurfaceCallback holds Context via WeakReference internally
            surfaceView.getHolder().addCallback(new SafeSurfaceCallback(i, this));

            // Relies on truncating integer division for deterministic row assignment
            GridLayout.Spec rowSpec = GridLayout.spec(i / grid.columns, GridLayout.FILL, 1.0f);
            GridLayout.Spec colSpec = GridLayout.spec(i % grid.columns, GridLayout.FILL, 1.0f);
            GridLayout.LayoutParams params = new GridLayout.LayoutParams(rowSpec, colSpec);
            params.width = 0;
            params.height = 0;

            surfaceView.setLayoutParams(params);
            surfaceGrid.addView(surfaceView);
        }
    }

    static GridSize calculateOptimalGrid(int maxWorkers, double containerAspectRatio) {
        int bestColumns = 1;
        int bestRows = maxWorkers;
        double minAspectDiff = Double.MAX_VALUE;

        for (int c = 1; c <= maxWorkers; c++) {
            int r = (int) Math.ceil((double) maxWorkers / c);

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
    public void onSurfaceCreated(int workerId, SurfaceHolder holder) {
        Log.i(TAG, "onSurfaceCreated: Raw Surface READY for worker " + workerId);
        // TODO: Dispatch surface to background rendering thread
    }

    @Override
    public void onSurfaceChanged(int workerId, SurfaceHolder holder, int format, int width, int height) {
        Log.i(TAG, String.format("onSurfaceChanged: Worker %d surface dimensions: [w: %d, h: %d]", workerId, width, height));
    }

    @Override
    public void onSurfaceDestroyed(int workerId) {
        Log.i(TAG, "onSurfaceDestroyed: Raw Surface DESTROYED for worker " + workerId);
        // TODO: Safely stop and join background worker thread before returning!
    }
}
