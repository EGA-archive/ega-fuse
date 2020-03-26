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
    public void noUsername() {
        String[] args = { "-u" };
        optionParser.parse(args);
    }

    @Test
    public void correctUsername() {
        String[] args = { "-u", "uu" };
        final OptionSet optionSet = optionParser.parse(args);
        assertEquals(args[1], optionSet.valueOf("u"));
    }
    
    @Test(expected = OptionException.class)
    public void noPassword() {
        String[] args = { "-p" };
        optionParser.parse(args);
    }
    
    @Test
    public void correctPassword() {
        String[] args = {"-p", "pp" };
        final OptionSet optionSet = optionParser.parse(args);
        assertEquals(args[1], optionSet.valueOf("p"));
    }
    
    @Test
    public void passAllArguments() {
        String[] args = { "-u", "uu", "-p", "pp", "-c", "10", "-m", "/tmp/mount", "-t", "btoken", "-rt", "rtoken", "-fu", "/home/user/credfile.txt", "-ft", "/home/user/token.txt" };
        final OptionSet optionSet = optionParser.parse(args);
        assertEquals(args[1], optionSet.valueOf("u"));
        assertEquals(args[3], optionSet.valueOf("p"));
        assertEquals(args[5], optionSet.valueOf("c").toString());
        assertEquals(args[7], optionSet.valueOf("m"));
        assertEquals(args[9], optionSet.valueOf("t"));
        assertEquals(args[11], optionSet.valueOf("rt"));
        assertEquals(args[13], optionSet.valueOf("fu"));
        assertEquals(args[15], optionSet.valueOf("ft"));
    }

}