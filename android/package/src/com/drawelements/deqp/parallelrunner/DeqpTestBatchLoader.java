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

import java.util.Locale;
import android.util.Log;
import android.icu.text.Collator;
import android.icu.text.RuleBasedCollator;
import java.io.File;
import java.util.Arrays;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * DeqpTestBatchLoader is responsible for discovering and queuing pre-split dEQP test batch
 * files pushed to the device storage by the host runner.
 */
public class DeqpTestBatchLoader {
    private static final String TAG = "DeqpTestBatchLoader";

    private volatile Queue<String> mBatchQueue = new ConcurrentLinkedQueue<>();

    /**
     * Scans the specified directory and loads all pre-split .txt test batch files in natural numeric order.
     * This method performs file loading atomically using a volatile reference swap.
     *
     * @param directoryPath Path to the directory containing the test batch files.
     */
    public void loadFromDirectory(String directoryPath) {
        if (directoryPath == null) {
            Log.w(TAG, "Test batches directory path is null.");
            return;
        }

        File dir = new File(directoryPath);
        if (!dir.exists()) {
            Log.e(TAG, "Test batches directory does not exist: " + directoryPath);
            return;
        }
        if (!dir.isDirectory()) {
            Log.e(TAG, "Specified path is not a directory: " + directoryPath);
            return;
        }

        File[] files = dir.listFiles();
        if (files == null) {
            Log.e(TAG, "Failed to list files in directory (possible permission issue): " + directoryPath);
            return;
        }

        // Maintain numerical/natural execution sequence
        RuleBasedCollator collator = (RuleBasedCollator) Collator.getInstance(Locale.US);
        collator.setNumericCollation(true);
        Arrays.sort(files, (f1, f2) -> collator.compare(f1.getName(), f2.getName()));

        Queue<String> tempQueue = new ConcurrentLinkedQueue<>();
        for (File file : files) {
            if (file.isFile() && file.getName().endsWith(".txt")) {
                tempQueue.add(file.getAbsolutePath());
            }
        }

        // Atomic volatile swap ensures concurrent threads see a complete and fully loaded queue
        mBatchQueue = tempQueue;
        Log.i(TAG, "Loaded " + mBatchQueue.size() + " test batch files from: " + directoryPath);
    }

    /**
     * Returns the FIFO queue containing loaded batch file absolute paths.
     */
    public Queue<String> getBatchQueue() {
        return mBatchQueue;
    }
}
