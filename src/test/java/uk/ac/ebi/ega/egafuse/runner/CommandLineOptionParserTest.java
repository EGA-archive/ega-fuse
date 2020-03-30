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

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.junit4.SpringRunner;

import joptsimple.OptionException;
import joptsimple.OptionParser;
import joptsimple.OptionSet;
import uk.ac.ebi.ega.egafuse.runner.CommandLineOptionParser;

@RunWith(SpringRunner.class)
public class CommandLineOptionParserTest {
    private final OptionParser optionParser = CommandLineOptionParser.getOptionParser();

    @Test(expected = OptionException.class)
    public void mutuallyExclusiveNoUsernameOrCf() {
        String[] args = {};
        optionParser.parse(args);
    }

    @Test(expected = OptionException.class)
    public void mutuallyExclusiveUsernameNoPassword() {
        String[] args = { "-u", "uu" };
        optionParser.parse(args);
    }

    @Test
    public void mutuallyExclusiveUsernameWithPassword() {
        String[] args = { "-u", "uu", "-p", "pp" };
        optionParser.parse(args);
    }

    @Test
    public void mutuallyExclusiveCf() {
        String[] args = { "-cf", "/home/user/credfile.txt" };
        optionParser.parse(args);
    }

    @Test
    public void passAllArguments() {
        String[] args = { "-u", "uu", "-p", "pp", "-m", "/tmp/mount", "-cf", "/home/user/credfile.txt" };
        final OptionSet optionSet = optionParser.parse(args);
        assertEquals(args[1], optionSet.valueOf("u"));
        assertEquals(args[3], optionSet.valueOf("p"));
        assertEquals(args[5], optionSet.valueOf("m"));
        assertEquals(args[7], optionSet.valueOf("cf"));
    }
}