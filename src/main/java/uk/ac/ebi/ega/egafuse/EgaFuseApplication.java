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
package uk.ac.ebi.ega.egafuse;

import java.io.IOException;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.PropertySource;

import uk.ac.ebi.ega.egafuse.runner.CommandLineOptionParser;
import uk.ac.ebi.ega.egafuse.runner.CommandLineOptionPropertySource;
import uk.ac.ebi.ega.egafuse.runner.EgaFuseCommandLineRunner;

@SpringBootApplication
public class EgaFuseApplication {
    public static void main(String[] args) throws IOException {
        PropertySource<?> ps = new CommandLineOptionPropertySource("CommandLineOptionPropertySource",
                CommandLineOptionParser.buildParser().parse(args));

        new SpringApplicationBuilder(EgaFuseApplication.class).initializers((applicationContext) -> {
            applicationContext.getEnvironment().getPropertySources().addLast(ps);
        }).run(args); 
    }

    @Bean
    public EgaFuseCommandLineRunner initEgaFuseCommandLineRunner() {
        return new EgaFuseCommandLineRunner();
    }
}
