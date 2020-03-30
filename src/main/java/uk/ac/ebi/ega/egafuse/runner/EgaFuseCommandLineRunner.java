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

import static uk.ac.ebi.ega.egafuse.runner.CommandLineOptionParser.OPTIONS_HELP;

import java.io.IOException;

import org.springframework.boot.CommandLineRunner;

import joptsimple.OptionException;
import joptsimple.OptionParser;
import joptsimple.OptionSet;
import lombok.extern.slf4j.Slf4j;
import uk.ac.ebi.ega.egafuse.validator.Validator;

@Slf4j
public class EgaFuseCommandLineRunner implements CommandLineRunner {
    @Override
    public void run(String... args) throws IOException {
        final OptionSet optionSet = parseOptions(args);
        Validator.isValid(optionSet);
    }

    private OptionSet parseOptions(String... args) throws IOException {
        final OptionParser optionParser = CommandLineOptionParser.getOptionParser();
        try {
            final OptionSet optionSet = optionParser.parse(args);
            if (optionSet.has(OPTIONS_HELP)) {
                optionParser.printHelpOn(System.out);
                System.exit(0);
            }
            return optionSet;
        } catch (OptionException e) {
            log.error("Passed invalid command line arguments");
            optionParser.printHelpOn(System.out);
            System.exit(1);
        }
        return null;
    }
}
