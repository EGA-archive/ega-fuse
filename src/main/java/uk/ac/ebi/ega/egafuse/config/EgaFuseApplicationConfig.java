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

import com.google.common.cache.CacheBuilder;

import okhttp3.ConnectionPool;
import okhttp3.OkHttpClient;

@Configuration
@EnableCaching
public class EgaFuseApplicationConfig {
    @Value("${connection.max}")
    private int DEFAULT_MAX_TOTAL_CONNECTIONS;

    @Value("${connection.request.timeout}")
    private int DEFAULT_REQUEST_TIMEOUT;

    @Value("${connection.timeout}")
    private int DEFAULT_CONNECTION_TIMEOUT;

    @Value("${connection.alive.timeout}")
    private int DEFAULT_KEEP_ALIVE_TIMEOUT;

    @Value("${cache.max.size}")
    private int CACHE_MAX_SIZE;

    @Bean
    public CacheManager cacheManager() {
        SimpleCacheManager simpleCacheManager = new SimpleCacheManager();
        GuavaCache archive = new GuavaCache("archive",
                CacheBuilder.newBuilder().expireAfterAccess(5, TimeUnit.HOURS).maximumSize(CACHE_MAX_SIZE).build());
        simpleCacheManager.setCaches(Arrays.asList(archive));
        return simpleCacheManager;
    }

    @Bean
    public OkHttpClient OkHttpClientFactory() {
        OkHttpClient.Builder builder = new OkHttpClient.Builder();
        ConnectionPool connectionPool = new ConnectionPool(DEFAULT_MAX_TOTAL_CONNECTIONS, DEFAULT_KEEP_ALIVE_TIMEOUT,
                TimeUnit.MINUTES);
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
}
