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

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import org.junit.After;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import joptsimple.OptionException;
import joptsimple.OptionSet;
import uk.ac.ebi.ega.egafuse.model.CliConfigurationValues;

public class CommandLineOptionParserTest {
    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @After
    public void cleanTestEnvironment() {
        temporaryFolder.delete();
    }
    
    @Test
    public void usernameAndPasswordInCf() throws IOException{        
        final File mountFolder = new File("tmp/mount");
        final File credFolder = temporaryFolder.newFolder("home", "user");
        final File credFile = new File(credFolder, "credfile.txt");
        try (final FileOutputStream fileOutputStream = new FileOutputStream(credFile)) {
            fileOutputStream.write("username:amohan".getBytes());
            fileOutputStream.write("\n".getBytes());
            fileOutputStream.write("password:testpass".getBytes());
            fileOutputStream.flush();
        }
        String[] args = { "-m", mountFolder.toPath().toAbsolutePath().toString(), "-cf", credFile.toPath().toAbsolutePath().toString(), "-c", "2"};
        OptionSet set = CommandLineOptionParser.buildParser().parse(args) ;        
        CliConfigurationValues cliConfigurationValues = CommandLineOptionParser.parser(set);
        assertEquals(cliConfigurationValues.getCredential().getUsername(), "amohan");
        assertEquals(cliConfigurationValues.getCredential().getPassword(), "testpass");
        assertEquals(cliConfigurationValues.getConnection(), 2);
        assertEquals(cliConfigurationValues.getMountPath().toString(), mountFolder.getAbsolutePath().toString());
    }
    
    @Test(expected = IllegalArgumentException.class)
    public void passwordMissingInCf() throws IOException{        
        final File mountFolder = new File("tmp/mount");
        final File credFolder = temporaryFolder.newFolder("home", "user");
        final File credFile = new File(credFolder, "credfile.txt");
        try (final FileOutputStream fileOutputStream = new FileOutputStream(credFile)) {
            fileOutputStream.write("username:amohan".getBytes());
            fileOutputStream.flush();
        }
        String[] args = { "-m", mountFolder.toPath().toAbsolutePath().toString(), "-cf", credFile.toPath().toAbsolutePath().toString()};
        OptionSet set = CommandLineOptionParser.buildParser().parse(args) ;        
        CommandLineOptionParser.parser(set);
    }
    
    @Test(expected = IOException.class)
    public void CfNotPresent() throws IOException{        
        final File mountFolder = temporaryFolder.newFolder("tmp", "mount");
        String[] args = { "-m", mountFolder.toPath().toAbsolutePath().toString(), "-cf", "/randomdir/random.txt"};
        OptionSet set = CommandLineOptionParser.buildParser().parse(args) ;        
        CommandLineOptionParser.parser(set);
    }
    
    @Test
    public void usernameAndPasswordPresent() throws IOException{        
        final File mountFolder = new File("tmp/mount");
        String[] args = { "-m", mountFolder.toPath().toAbsolutePath().toString(), "-u", "amohan", "-p", "testpass"};
        OptionSet set = CommandLineOptionParser.buildParser().parse(args) ;        
        CliConfigurationValues cliConfigurationValues = CommandLineOptionParser.parser(set);
        assertEquals(cliConfigurationValues.getCredential().getUsername(), "amohan");
        assertEquals(cliConfigurationValues.getCredential().getPassword(), "testpass");
    }

    @Test(expected = OptionException.class)
    public void passwordMissing() throws IOException{        
        final File mountFolder = temporaryFolder.newFolder("tmp", "mount");       
        String[] args = { "-m", mountFolder.toPath().toAbsolutePath().toString(), "-u", "amohan"};
        OptionSet set = CommandLineOptionParser.buildParser().parse(args) ;        
        CommandLineOptionParser.parser(set);
    }
    
    @Test(expected = IllegalArgumentException.class)
    public void mountPathPresent() throws IOException{
        final File mountFolder = temporaryFolder.newFolder("tmp", "mount");
        String[] args = { "-m", mountFolder.getAbsolutePath(), "-u", "amohan", "-p", "testpass"};
        OptionSet set = CommandLineOptionParser.buildParser().parse(args) ;        
        CommandLineOptionParser.parser(set);
    }
}
