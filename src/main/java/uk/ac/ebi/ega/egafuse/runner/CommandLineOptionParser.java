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
import java.util.StringTokenizer;

import joptsimple.OptionParser;
import joptsimple.OptionSet;
import joptsimple.internal.Strings;
import joptsimple.util.PathConverter;
import uk.ac.ebi.ega.egafuse.model.CliConfigurationValues;
import uk.ac.ebi.ega.egafuse.model.Credential;

public class CommandLineOptionParser {
    private static final String OPTIONS_HELP = "h";

    public static CliConfigurationValues parser(OptionSet optionSet) throws IOException {
        final OptionParser optionParser = buildParser();
        if (optionSet.has(OPTIONS_HELP)) {
            optionParser.printHelpOn(System.out);
            System.exit(0);
        }

        final CliConfigurationValues cliConfigurationValues = new CliConfigurationValues();
        cliConfigurationValues.setConnection(Integer.valueOf(optionSet.valueOf("c").toString()));
        cliConfigurationValues.setMaxCache(Integer.valueOf(optionSet.valueOf("cache").toString()));

        if (optionSet.has("cf")) {
            cliConfigurationValues.setCredential(readCredentialFile((Path) optionSet.valueOf("cf")));
        } else {
            Credential credential = new Credential();
            credential.setUsername(optionSet.valueOf("u").toString());
            credential.setPassword(optionSet.valueOf("p").toString());
            cliConfigurationValues.setCredential(credential);
        }

        Path mntPath = (Path) (optionSet.valueOf("m"));
        if (Files.exists(mntPath)) {
            throw new IllegalArgumentException(mntPath.toString()
                    .concat(" can't be used as mount point. Ensure that the directory path should not exist and it will be created by fuse layer!"));
        }
        cliConfigurationValues.setMountPath(mntPath);
        return cliConfigurationValues;
    }

    public static OptionParser buildParser() {
        OptionParser parser = new OptionParser();
        parser.mutuallyExclusive(parser.accepts("u"), parser.accepts("cf"));
        parser.accepts("cf",
                "credential file path containing username & password, e.g. \n username:user1 \n  password:pass")
                .withRequiredArg().withValuesConvertedBy(new PathConverter());
        parser.accepts("u", "username").requiredUnless("cf").withRequiredArg();
        parser.accepts("p", "password").requiredIf("u").withRequiredArg();
        parser.accepts("c", "connections").withRequiredArg().ofType(Integer.class).defaultsTo(4);
        parser.accepts("cache", "max cache").withRequiredArg().ofType(Integer.class).defaultsTo(100);
        parser.accepts("m", "mount path").withRequiredArg().withValuesConvertedBy(new PathConverter())
                .defaultsTo(Paths.get("/tmp/mnt"));
        parser.accepts(OPTIONS_HELP, "Use this option to get help");
        parser.allowsUnrecognizedOptions();
        return parser;
    }

    private static Credential readCredentialFile(Path filepath) throws IOException {
        Credential credential = new Credential();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(filepath.toFile())))) {
            String line;
            while ((line = br.readLine()) != null) {
                StringTokenizer st = new StringTokenizer(line, ":");
                String key = st.nextToken(":");

                if (key.equalsIgnoreCase("username")) {
                    credential.setUsername(st.nextToken(":"));
                } else if (key.equalsIgnoreCase("password")) {
                    credential.setPassword(st.nextToken(":"));
                }
            }

            if (Strings.isNullOrEmpty(credential.getUsername()) || Strings.isNullOrEmpty(credential.getPassword())) {
                throw new IllegalArgumentException(
                        "Username or Password not Specified in File ".concat(filepath.toString()));
            }
        } catch (IOException e) {
            throw new IOException(e.getMessage(), e);
        }
        return credential;
    }
}
