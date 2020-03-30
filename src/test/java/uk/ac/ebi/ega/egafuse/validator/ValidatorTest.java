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

import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import org.junit.After;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.springframework.test.context.junit4.SpringRunner;

import joptsimple.OptionParser;
import uk.ac.ebi.ega.egafuse.runner.CommandLineOptionParser;

@RunWith(SpringRunner.class)
public class ValidatorTest {
    private final OptionParser optionParser = CommandLineOptionParser.getOptionParser();

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @After
    public void cleanTestEnvironment() {
        temporaryFolder.delete();
    }

    @Test(expected = IllegalArgumentException.class)
    public void passwordAbsentInCredFile() throws IOException {
        final File mountFolder = temporaryFolder.newFolder("tmp", "mount");
        final File credFolder = temporaryFolder.newFolder("home", "user");
        final File credFile = new File(credFolder, "credfile.txt");
        try (final FileOutputStream fileOutputStream = new FileOutputStream(credFile)) {
            fileOutputStream.write("username:amohan".getBytes());
            fileOutputStream.flush();
        }
        String[] args = { "-m", mountFolder.toPath().toAbsolutePath().toString(), "-cf",
                credFile.toPath().toAbsolutePath().toString() };
        Validator.isValid(optionParser.parse(args));
    }

    @Test(expected = IOException.class)
    public void credFileNotFound() throws IOException {
        final File mountFolder = temporaryFolder.newFolder("tmp", "mount");
        String[] args = { "-m", mountFolder.toPath().toAbsolutePath().toString(), "-cf", "/home/user/credfile.txt" };
        Validator.isValid(optionParser.parse(args));
    }

    @Test
    public void usernameAndPasswordBothPresent() throws IOException {
        final File mountFolder = temporaryFolder.newFolder("tmp", "mount");
        final File credFolder = temporaryFolder.newFolder("home", "user");
        final File credFile = new File(credFolder, "credfile.txt");
        try (final FileOutputStream fileOutputStream = new FileOutputStream(credFile)) {
            fileOutputStream.write("username:amohan".getBytes());
            fileOutputStream.write("\n".getBytes());
            fileOutputStream.write("password:testpass".getBytes());
            fileOutputStream.flush();
        }

        String[] args = { "-m", mountFolder.toPath().toAbsolutePath().toString(), "-cf",
                credFile.toPath().toAbsolutePath().toString() };
        assertTrue(Validator.isValid(optionParser.parse(args)));
    }
}
