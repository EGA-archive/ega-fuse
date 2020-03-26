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

import joptsimple.OptionParser;

public class CommandLineOptionParser {

    public static final String OPTIONS_HELP = "h";

    private static final OptionParser optionParser = buildParser();

    private static OptionParser buildParser() {
        OptionParser parser = new OptionParser();
        parser.accepts("u", "username").withRequiredArg();
        parser.accepts("p", "password").withRequiredArg();
        parser.accepts("c", "parallel connections").withRequiredArg().ofType( Integer.class );
        parser.accepts("m", "mount path").withRequiredArg().defaultsTo("/tmp/mnt");
        parser.accepts("t", "bearer token").withRequiredArg();
        parser.accepts("rt", "refresh token").withRequiredArg();
        parser.accepts("fu", "credential file path containing username & password, e.g. \n username:user1 \n  password:pass").withRequiredArg();
        parser.accepts("ft", "token file path containing bearer token & refresh token, e.g.\n accessToken:accToken \n refreshToken:refToken").withRequiredArg();
        parser.accepts(OPTIONS_HELP, "Use this option to get help");
        parser.allowsUnrecognizedOptions();
        return parser;
    }

    public static OptionParser getOptionParser() {
        return optionParser;
    }
}
