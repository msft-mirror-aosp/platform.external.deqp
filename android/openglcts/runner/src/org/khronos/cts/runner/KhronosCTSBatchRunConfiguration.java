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
package org.khronos.cts.runner;
import com.drawelements.deqp.runner.BatchRunConfiguration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
/**
 * Test configuration of dEPQ test instance execution.
 */
public class KhronosCTSBatchRunConfiguration extends BatchRunConfiguration{
    private HashMap<String, String> mRunConfigs;
    public KhronosCTSBatchRunConfiguration(HashMap<String, String> runConfigs) {
        mRunConfigs = runConfigs;
    }
    @Override
    public String getId() {
        List<String> runConfigKeys = new ArrayList<>(mRunConfigs.keySet());
        Collections.sort(runConfigKeys);
        StringBuilder runConfigString = new StringBuilder();
        for (String key : runConfigKeys) {
            if (runConfigString.length() != 0) {
                runConfigString.append(" ");
            }
            runConfigString.append(key);
            runConfigString.append("=");
            runConfigString.append(mRunConfigs.get(key));
        }
        return runConfigString.toString();
    }
    @Override
    public boolean equals(Object other) {
        if (other == null) {
            return false;
        } else if (!(other instanceof KhronosCTSBatchRunConfiguration)) {
            return false;
        } else {
            return getId().equals(((KhronosCTSBatchRunConfiguration)other).getId());
        }
    }
    @Override
    public int hashCode() {
        return getId().hashCode();
    }
}
