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
package uk.ac.ebi.ega.egafuse.runner;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.StringTokenizer;

import joptsimple.OptionParser;
import joptsimple.OptionSet;
import joptsimple.internal.Strings;
import joptsimple.util.PathConverter;
import uk.ac.ebi.ega.egafuse.model.Input;

public class CommandLineOptionParser {
    private static final String OPTIONS_HELP = "h";

    public static Input parser(String[] args) throws IOException {
        final OptionParser optionParser = buildParser();
        final OptionSet optionSet = optionParser.parse(args);
        if (optionSet.has(OPTIONS_HELP)) {
            optionParser.printHelpOn(System.out);
            System.exit(0);
        }

        final Input input = new Input();
        input.setConnection(Integer.valueOf(optionSet.valueOf("c").toString()));

        if (optionSet.has("cf")) {
            Path credFilePath = (Path) optionSet.valueOf("cf");
            Map<String, String> credentials = readAndValidateCredentialFile(credFilePath);
            input.setUsername(credentials.get("username"));
            input.setPassword(credentials.get("password"));
            input.setCredFile(credFilePath);
        } else {
            input.setUsername(optionSet.valueOf("username").toString());
            input.setPassword(optionSet.valueOf("password").toString());
        }

        Path mntPath = (Path) (optionSet.valueOf("m"));
        if (!Files.exists(mntPath)) {
            throw new IllegalArgumentException(mntPath.toString()
                    .concat(" can't be used as mount point. Ensure that the directory path exists and is empty"));
        }
        input.setMountPath(mntPath);
        return input;
    }

    private static OptionParser buildParser() {
        OptionParser parser = new OptionParser();
        parser.mutuallyExclusive(parser.accepts("u"), parser.accepts("cf"));
        parser.accepts("cf",
                "credential file path containing username & password, e.g. \n username:user1 \n  password:pass")
                .withRequiredArg().withValuesConvertedBy(new PathConverter());
        parser.accepts("u", "username").requiredUnless("cf").withRequiredArg();
        parser.accepts("p", "password").requiredIf("u").withRequiredArg();
        parser.accepts("c", "connections").withRequiredArg().ofType(Integer.class).defaultsTo(1);
        parser.accepts("m", "mount path").withRequiredArg().withValuesConvertedBy(new PathConverter())
                .defaultsTo(Paths.get("/tmp/mnt"));
        parser.accepts(OPTIONS_HELP, "Use this option to get help");
        parser.allowsUnrecognizedOptions();
        return parser;
    }

    private static Map<String, String> readAndValidateCredentialFile(Path filepath) throws IOException {
        Map<String, String> filedata = new HashMap<>();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(filepath.toFile())))) {
            String line;
            while ((line = br.readLine()) != null) {
                StringTokenizer st = new StringTokenizer(line, ":");
                String key = st.nextToken(":");
                String value = st.nextToken(":");
                filedata.put(key, value);
            }

            if (Strings.isNullOrEmpty(filedata.get("username")) || Strings.isNullOrEmpty(filedata.get("password"))) {
                throw new IllegalArgumentException(
                        "Username or Password not Specified in File ".concat(filepath.toString()));
            }
        } catch (IOException e) {
            throw new IOException(e.getMessage(), e);
        }
        return filedata;
    }
}
