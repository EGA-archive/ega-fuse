/*
 *
 * Copyright 2020 EMBL - European Bioinformatics Institute
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
package uk.ac.ebi.ega.egafuse.validator;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;
import java.util.StringTokenizer;

import jnr.posix.util.Platform;
import joptsimple.OptionSet;
import joptsimple.internal.Strings;

public class Validator {
    public static boolean isValid(OptionSet optionSet) throws IOException {
        if (!Platform.IS_WINDOWS) {
            File mntTest = new File(optionSet.valueOf("m").toString());
            if (!mntTest.exists() && !mntTest.isDirectory()) {
                throw new IllegalArgumentException(mntTest.getPath()
                        .concat(" can't be used as mount point. Ensure that the directory path exists and is empty"));
            }
        }

        if (optionSet.has("cf")) {
            String credFilePath = optionSet.valueOf("cf").toString();
            Map<String, String> credentials = readFileArguments(credFilePath);
            if (Strings.isNullOrEmpty(credentials.get("username"))
                    || Strings.isNullOrEmpty(credentials.get("password"))) {
                throw new IllegalArgumentException("Username or Password not Specified in File ".concat(credFilePath));
            }
        }

        return true;
    }

    private static Map<String, String> readFileArguments(String filepath) throws IOException {
        Map<String, String> filedata = new HashMap<>();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(filepath)))) {
            String line;
            while ((line = br.readLine()) != null) {
                StringTokenizer st = new StringTokenizer(line, ":");
                String key = st.nextToken(":");
                String value = st.nextToken(":");
                filedata.put(key, value);
            }
        } catch (IOException e) {
            throw new IOException(e.getMessage(), e);
        }
        return filedata;
    }
}