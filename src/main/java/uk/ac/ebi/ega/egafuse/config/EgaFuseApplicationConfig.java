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

import java.util.Arrays;
import java.util.concurrent.TimeUnit;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.guava.GuavaCache;
import org.springframework.cache.support.SimpleCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.OkHttp3ClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.common.cache.CacheBuilder;

import okhttp3.ConnectionPool;
import okhttp3.OkHttpClient;
import uk.ac.ebi.ega.egafuse.runner.EgaFuseCommandLineRunner;
import uk.ac.ebi.ega.egafuse.service.EgaDatasetService;
import uk.ac.ebi.ega.egafuse.service.EgaDirectory;
import uk.ac.ebi.ega.egafuse.service.EgaFileService;
import uk.ac.ebi.ega.egafuse.service.EgaFuse;
import uk.ac.ebi.ega.egafuse.service.Token;

@Configuration
@EnableCaching
public class EgaFuseApplicationConfig {
    @Bean
    public CacheManager cacheManager(@Value("${maxCache}") int MAX_CACHE_SIZE) {
        SimpleCacheManager simpleCacheManager = new SimpleCacheManager();
        GuavaCache archive = new GuavaCache("archive",
                CacheBuilder.newBuilder().expireAfterAccess(5, TimeUnit.HOURS).maximumSize(MAX_CACHE_SIZE).build());
        simpleCacheManager.setCaches(Arrays.asList(archive));
        return simpleCacheManager;
    }

    @Bean
    public OkHttpClient OkHttpClientFactory(@Value("${connection.request.timeout}") int DEFAULT_REQUEST_TIMEOUT,
            @Value("${connection.timeout}") int DEFAULT_CONNECTION_TIMEOUT,
            @Value("${connection.alive.timeout}") int DEFAULT_KEEP_ALIVE_TIMEOUT,
            @Value("${connection}") int CONNECTION) {
        OkHttpClient.Builder builder = new OkHttpClient.Builder();
        ConnectionPool connectionPool = new ConnectionPool(CONNECTION, DEFAULT_KEEP_ALIVE_TIMEOUT, TimeUnit.MINUTES);
        OkHttpClient okHttpClient = builder.connectTimeout(DEFAULT_CONNECTION_TIMEOUT, TimeUnit.MINUTES)
                .readTimeout(DEFAULT_REQUEST_TIMEOUT, TimeUnit.MINUTES).connectionPool(connectionPool).build();
        return okHttpClient;
    }

    @Bean
    public RestTemplate restTemplate(OkHttpClient httpClient) {
        RestTemplate restTemplate = new RestTemplate();
        restTemplate.setRequestFactory(new OkHttp3ClientHttpRequestFactory(httpClient));
        return restTemplate;
    }

    @Bean
    public Token token(@Value("${cred.username}") String username, @Value("${cred.password}") String password,
            @Value("${ega.userId}") String egaUserId, @Value("${ega.userSecret}") String egaUserSecret,
            @Value("${ega.userGrant}") String egaUserGrant, @Value("${aai.server.url}") String aaiUrl) {
        return new Token(new NetHttpTransport(), new JacksonFactory(), username, password, egaUserId, egaUserSecret,
                egaUserGrant, aaiUrl);
    }

    @Bean
    public EgaFileService initEgaFileService(OkHttpClient okHttpClient, @Value("${app.server.url}") String apiURL,
            Token token) {
        return new EgaFileService(okHttpClient, apiURL, token);
    }

    @Bean
    public EgaDatasetService initEgaDatasetService(OkHttpClient okHttpClient, @Value("${app.server.url}") String apiURL,
            Token token, EgaFileService egaFileService) {
        return new EgaDatasetService(okHttpClient, apiURL, token, egaFileService);
    }

    @Bean
    public EgaFuse initEgaFuse(@Value("${mountPath}") String mountPath, EgaDatasetService egaDatasetService, EgaFileService egaFileService) {
        EgaDirectory egaDirectory = new EgaDirectory("");
        egaDirectory.add(new EgaDirectory("Datasets", egaDatasetService, egaFileService));
        return new EgaFuse(egaDirectory, mountPath);
    }

    @Bean
    public EgaFuseCommandLineRunner initEgaFuseCommandLineRunner(EgaFuse egafuse) {
        return new EgaFuseCommandLineRunner(egafuse);
    }
}
