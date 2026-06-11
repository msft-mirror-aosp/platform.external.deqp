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

import androidx.test.ext.junit.runners.AndroidJUnit4;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.HashSet;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Unit tests for {@link DeqpTestBatchLoader}.
 */
@RunWith(AndroidJUnit4.class)
public class DeqpTestBatchLoaderTest {

    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();

    @Test
    public void testLoadFromDirectoryNumericalSorting() throws IOException {
        File tempDir = tempFolder.newFolder("test_batches_numerical");

        File file10 = new File(tempDir, "batch_10.txt");
        File file2 = new File(tempDir, "batch_2.txt");
        File file1 = new File(tempDir, "batch_1.txt");
        File otherFile = new File(tempDir, "not_a_batch.md");

        // Complex filenames and edge cases (large values, nested digit chunks)
        File fileLarge = new File(tempDir, "batch_2147483648.txt"); // 32-bit overflow
        File fileNested10 = new File(tempDir, "batch_v2_10.txt");
        File fileNested2 = new File(tempDir, "batch_v2_2.txt");

        assertTrue(file10.createNewFile());
        assertTrue(file2.createNewFile());
        assertTrue(file1.createNewFile());
        assertTrue(otherFile.createNewFile());
        assertTrue(fileLarge.createNewFile());
        assertTrue(fileNested10.createNewFile());
        assertTrue(fileNested2.createNewFile());

        DeqpTestBatchLoader loader = new DeqpTestBatchLoader();
        loader.loadFromDirectory(tempDir.getAbsolutePath());

        Queue<String> queue = loader.getBatchQueue();
        assertNotNull(queue);
        assertEquals(6, queue.size());

        // Verify natural numeric sequence
        assertEquals(file1.getAbsolutePath(), queue.poll());
        assertEquals(file2.getAbsolutePath(), queue.poll());
        assertEquals(file10.getAbsolutePath(), queue.poll());
        assertEquals(fileLarge.getAbsolutePath(), queue.poll());
        assertEquals(fileNested2.getAbsolutePath(), queue.poll());
        assertEquals(fileNested10.getAbsolutePath(), queue.poll());
    }

    @Test
    public void testConcurrentAccess() throws IOException, InterruptedException {
        File tempDir = tempFolder.newFolder("test_concurrent");

        int count = 100;
        File[] files = new File[count];
        for (int i = 0; i < count; i++) {
            files[i] = new File(tempDir, "batch_" + i + ".txt");
            assertTrue(files[i].createNewFile());
        }

        DeqpTestBatchLoader loader = new DeqpTestBatchLoader();
        loader.loadFromDirectory(tempDir.getAbsolutePath());

        Queue<String> queue = loader.getBatchQueue();
        assertNotNull(queue);
        assertEquals(count, queue.size());

        int threadCount = 10;
        Thread[] threads = new Thread[threadCount];
        Set<String> polledFiles = Collections.synchronizedSet(new HashSet<>());
        AtomicBoolean failed = new AtomicBoolean(false);

        for (int i = 0; i < threadCount; i++) {
            threads[i] = new Thread(() -> {
                while (true) {
                    String val = queue.poll();
                    if (val == null) {
                        break;
                    }
                    if (!polledFiles.add(val)) {
                        failed.set(true); // Duplicate detected!
                    }
                }
            });
            threads[i].start();
        }

        for (int i = 0; i < threadCount; i++) {
            threads[i].join();
        }

        assertTrue("Race condition detected: duplicate items polled!", !failed.get());
        assertEquals(count, polledFiles.size());
    }

    @Test
    public void testLoadFromInvalidDirectory() {
        DeqpTestBatchLoader loader = new DeqpTestBatchLoader();
        // Non-existent directory path
        loader.loadFromDirectory("/invalid/path/to/directory/that/does/not/exist");
        Queue<String> queue = loader.getBatchQueue();
        assertNotNull(queue);
        assertTrue(queue.isEmpty());
    }
}
