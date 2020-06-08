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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutionException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.benmanes.caffeine.cache.AsyncLoadingCache;

import jnr.ffi.Pointer;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import uk.ac.ebi.ega.egafuse.exception.ClientProtocolException;
import uk.ac.ebi.ega.egafuse.model.CacheKey;
import uk.ac.ebi.ega.egafuse.model.File;

public class EgaFileService {
    private static final Logger LOGGER = LoggerFactory.getLogger(EgaFileService.class);
    private int cachePrefetch;
    private OkHttpClient okHttpClient;
    private String apiURL;
    private long chunkSize;
    private Token token;
    private ObjectMapper mapper;
    private AsyncLoadingCache<CacheKey, byte[]> cache;

    public EgaFileService(OkHttpClient okHttpClient, String apiURL, long chunkSize, int cachePrefetch, Token token,
            AsyncLoadingCache<CacheKey, byte[]> cache) {
        this.okHttpClient = okHttpClient;
        this.apiURL = apiURL;
        this.chunkSize = chunkSize;
        this.cachePrefetch = cachePrefetch;
        this.token = token;
        this.mapper = new ObjectMapper();
        this.cache = cache;
    }

    public List<EgaFile> getFiles(EgaDirectory egaDirectory) {
        String datasetId = egaDirectory.getName();
        if (datasetId.endsWith("/")) {
            datasetId = datasetId.substring(0, datasetId.length() - 1);
        }

        try {
            Request fileRequest = new Request.Builder()
                    .url(apiURL.concat("/metadata/datasets/").concat(datasetId).concat("/files"))
                    .addHeader("Authorization", "Bearer " + token.getBearerToken()).build();

            try (Response response = okHttpClient.newCall(fileRequest).execute()) {
                return buildResponseGetFiles(response);
            } catch (IOException e) {
                throw new IOException("Unable to execute request. Can be retried.", e);
            } catch (ClientProtocolException e) {
                throw e;
            }
        } catch (Exception e) {
            LOGGER.error("Error in get dataset - {}", e.getMessage(), e);
        }
        return Collections.emptyList();
    }

    private List<EgaFile> buildResponseGetFiles(final Response response) throws IOException, ClientProtocolException {
        final int status = response.code();
        switch (status) {
        case 200:
            List<File> files = mapper.readValue(response.body().string(), new TypeReference<List<File>>() {
            });
            List<EgaFile> egaFiles = new ArrayList<>();
            for (File file : files) {
                String filename = file.getFileName();
                String displayFilename = file.getDisplayFileName();

                if (filename.toLowerCase().endsWith(".cip")) {
                    if (displayFilename.contains("/")) {
                        displayFilename = displayFilename.substring(displayFilename.lastIndexOf("/") + 1);
                    }

                    file.setFileSize(file.getFileSize() - 16);
                    egaFiles.add(new EgaFile(displayFilename, file, this));
                }
            }
            return egaFiles;
        default:
            LOGGER.error("status: {}", status);
            throw new ClientProtocolException(response.body().string());
        }
    }

    public int fillBufferCurrentChunk(Pointer buffer, String fileId, long fileSize, long bytesToRead, long offset) {
        int requestedChunkSize = (int) Math.min(fileSize - offset, bytesToRead);
        int chunkIndex = (int) (offset / chunkSize);

        if (offset >= fileSize || requestedChunkSize <= 0)
            return -1;

        prefetchChunk(fileId, chunkIndex, fileSize);

        try {
            byte[] chunk = cache.get(getCacheKey(fileId, chunkIndex, fileSize)).get();
            if (chunk != null) {
                int chunkOffset = (int) (offset - chunkIndex * chunkSize);
                buffer.put(0L, chunk, chunkOffset, requestedChunkSize);
                return requestedChunkSize;
            }
        } catch (InterruptedException | ExecutionException e) {
            LOGGER.error("Chunk {} could not be retrieved for file {} bytesToRead {} offset {} ", chunkIndex, fileId,
                    bytesToRead, offset);
            LOGGER.error("Error in reading from cache - {} ", e.getMessage(), e);
        } 
        return -1;
    }

    private void prefetchChunk(String fileId, int chunkIndex, long fileSize) {
        int maxChunk = (int) (fileSize / chunkSize);
        int endChunk = Math.min(chunkIndex + cachePrefetch, maxChunk);

        while (chunkIndex <= endChunk) {
            cache.get(getCacheKey(fileId, chunkIndex++, fileSize));
        }
    }

    private CacheKey getCacheKey(String fileId, int chunkIndex, long fileSize) {
        long startCoordinate = chunkIndex * chunkSize;
        long chunkBytesToRead = startCoordinate + chunkSize > fileSize ? (fileSize - startCoordinate) : chunkSize;
        return new CacheKey(startCoordinate, chunkBytesToRead, fileId);
    }
}
