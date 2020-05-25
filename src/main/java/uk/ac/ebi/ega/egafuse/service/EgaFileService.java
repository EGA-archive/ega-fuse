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

import static uk.ac.ebi.ega.egafuse.config.EgaFuseApplicationConfig.NUM_PAGES;
import static uk.ac.ebi.ega.egafuse.config.EgaFuseApplicationConfig.PAGE_SIZE;
import static uk.ac.ebi.ega.egafuse.config.EgaFuseApplicationConfig.ARCHIVE;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.CacheManager;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import jnr.ffi.Pointer;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import uk.ac.ebi.ega.egafuse.exception.ClientProtocolException;
import uk.ac.ebi.ega.egafuse.model.File;

public class EgaFileService {
    private static final Logger LOGGER = LoggerFactory.getLogger(EgaFileService.class);
    private OkHttpClient okHttpClient;
    private String apiURL;
    private Token token;
    private ObjectMapper mapper;
    private CacheManager cachemanager;
    private Map<String, Future<Boolean>> keys = new HashMap<>();
    private ExecutorService executor;
    private EgaRetryService egaRetryService;

    public EgaFileService(OkHttpClient okHttpClient, String apiURL, Token token, CacheManager cachemanager,
            ExecutorService executor, EgaRetryService egaRetryService) {
        this.okHttpClient = okHttpClient;
        this.apiURL = apiURL;
        this.token = token;
        this.mapper = new ObjectMapper();
        this.cachemanager = cachemanager;
        this.executor = executor;
        this.egaRetryService = egaRetryService;
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
                String filename = file.getDisplayFileName();
                if (filename.contains("/")) {
                    filename = filename.substring(filename.lastIndexOf("/") + 1);
                }

                if (filename.toLowerCase().endsWith(".gpg")) {
                } else if (filename.toLowerCase().endsWith(".cip")) {
                    file.setFileSize(file.getFileSize() - 16);
                }

                egaFiles.add(new EgaFile(filename.substring(0, filename.length() - 4), file, this));
            }
            return egaFiles;
        default:
            LOGGER.error("status: {}", status);
            throw new ClientProtocolException(response.body().string());
        }
    }

    public int fillBufferCurrentPage(Pointer buffer, String fileId, long totalFileSize, long currentPageSize,
            long offset) {
        int bytesToRead = (int) Math.min(totalFileSize - offset, currentPageSize);
        int currentPage = (int) (offset / PAGE_SIZE);

        if (offset >= totalFileSize || bytesToRead <= 0)
            return -1;

        loadPage(totalFileSize, fileId, currentPage, totalFileSize);

        byte[] page = null;
        final String key = fileId + "_" + currentPage;
        try {
            if (keys.get(key) != null && keys.get(key).get() && cachemanager.getCache(ARCHIVE).get(key) != null) {
                page = (byte[]) cachemanager.getCache(ARCHIVE).get(key).get();
            }
        } catch (InterruptedException | ExecutionException e) {
            LOGGER.error("Error in reading from cachemanager - {} ", e.getMessage(), e);
        } 

        if (page == null) {
            LOGGER.error("Service seems to be down");
            return -1;
        } else {
            int pageOffset = (int) (offset - currentPage * PAGE_SIZE);
            buffer.put(0L, page, pageOffset, bytesToRead);
            return bytesToRead;
        }
    }

    private void loadPage(long fsize, String fileId, int currentPage, long fileSize) {
        int maxPage = (int) (fsize / PAGE_SIZE);
        int nextEndPage = currentPage + NUM_PAGES;
        nextEndPage = nextEndPage > maxPage ? maxPage : nextEndPage;

        for (; currentPage <= nextEndPage; currentPage++) {
            final int nextPageNumber = currentPage;
            final String key = fileId + "_" + nextPageNumber;
            keys.computeIfAbsent(key, k -> executor.submit(() -> {
                return egaRetryService.downloadPage(fileId, nextPageNumber, fileSize);
            }));
        }
    }

}
