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
package uk.ac.ebi.ega.egafuse.config;

import static org.junit.Assert.assertEquals;

import java.io.IOException;

import org.junit.ClassRule;
import org.junit.Test;
import org.junit.contrib.java.lang.system.EnvironmentVariables;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.CacheManager;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;

import okhttp3.OkHttpClient;

@TestPropertySource("classpath:application-test.properties")
@ContextConfiguration(classes = EgaFuseApplicationConfig.class)
@RunWith(SpringRunner.class)
public class EgaFuseApplicationConfigTest {
    @Value("${connection.request.timeout}")
    private int DEFAULT_REQUEST_TIMEOUT;

    @Value("${connection.timeout}")
    private int DEFAULT_CONNECTION_TIMEOUT;

    @Autowired
    private CacheManager cacheManager;

    @Autowired
    private OkHttpClient okhttp;

    @ClassRule
    public static final EnvironmentVariables environmentVariables = new EnvironmentVariables()
        .set("maxCache", "4")
        .set("connection", "5");

    @Test
    public void testResourceParameters() throws IOException {
        assertEquals(DEFAULT_REQUEST_TIMEOUT * 60000, okhttp.readTimeoutMillis());
        assertEquals(DEFAULT_CONNECTION_TIMEOUT * 60000, okhttp.connectTimeoutMillis());
        assertEquals("archive", cacheManager.getCache("archive").getName());
    }
}