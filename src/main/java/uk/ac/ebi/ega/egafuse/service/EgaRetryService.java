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
package uk.ac.ebi.ega.egafuse.service;

import static uk.ac.ebi.ega.egafuse.config.EgaFuseApplicationConfig.PAGE_SIZE;

import java.io.DataInputStream;
import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.CacheManager;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;

import com.google.common.io.CountingInputStream;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import uk.ac.ebi.ega.egafuse.exception.ClientProtocolException;

public class EgaRetryService {
    private static final Logger LOGGER = LoggerFactory.getLogger(EgaRetryService.class);
    private OkHttpClient okHttpClient;
    private String apiURL;
    private CacheManager cachemanager;
    private Token token;

    public EgaRetryService(OkHttpClient okHttpClient, String apiURL, Token token, CacheManager cachemanager) {
        this.okHttpClient = okHttpClient;
        this.apiURL = apiURL;
        this.token = token;
        this.cachemanager = cachemanager;
    }

    @Retryable(value = { IOException.class, ClientProtocolException.class }, maxAttempts = 6, backoff = @Backoff(10000))
    public String downloadPage(String fileId, int pageNumber, long fileSize)
            throws IOException, ClientProtocolException {
        String key = fileId + "_" + pageNumber;

        long startCoordinate = pageNumber * PAGE_SIZE;
        long bytesToRead = startCoordinate + PAGE_SIZE > fileSize ? (fileSize - startCoordinate) : PAGE_SIZE;
        String url = apiURL + "/files/" + fileId + "?destinationFormat=plain&startCoordinate=" + startCoordinate
                + "&endCoordinate=" + (startCoordinate + bytesToRead);
        LOGGER.info("page_number = " + pageNumber + ", url = " + url);

        Request fileRequest;
        try {
            fileRequest = new Request.Builder().url(url).addHeader("Authorization", "Bearer " + token.getBearerToken())
                    .build();

            try (Response response = okHttpClient.newCall(fileRequest).execute()) {
                byte[] buffer = buildResponseDownloadFiles(response, bytesToRead);
                cachemanager.getCache("archive").put(key, buffer);
                return "success";
            } catch (IOException e) {
                throw new IOException("Unable to execute request. Can be retried.", e);
            } catch (ClientProtocolException e) {
                throw new ClientProtocolException(e.toString());
            }
        } catch (IOException e) {
            LOGGER.error("Error in downloading file - {}", e.getMessage());
            throw new IOException("Unable to execute request. Can be retried.", e);
        }
    }

    private byte[] buildResponseDownloadFiles(final Response response, long bytesToRead)
            throws IOException, ClientProtocolException {
        final int status = response.code();
        switch (status) {
        case 200:
        case 206:
            byte[] buffer = new byte[(int) bytesToRead];
            try (CountingInputStream cIn = new CountingInputStream(response.body().byteStream());
                    DataInputStream dis = new DataInputStream(cIn);) {
                dis.readFully(buffer);
            }
            return buffer;
        default:
            LOGGER.error("status: {}", status);
            throw new ClientProtocolException(response.body().string());
        }
    }
}
